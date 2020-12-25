package org.kys.athena.riot.api.ratelimit

import zio._
import zio.clock.Clock
import zio.stream.ZStream
import zio.duration._
import cats.effect.concurrent.Semaphore
import zio.console.Console
import zio.interop.catz._


trait TokenBucket {
  def withPermit[E <: Throwable, A](task: IO[E, A]): IO[E, A]

  def acquire: UIO[Unit]

  def release: UIO[Unit]
}

object TokenBucket {

  def make(rl: RateLimit): ZManaged[Clock with Console, Throwable, TokenBucket] = {
    // Allocate cats semaphore (explicit .acquire and .release are required), and an unbounded queue
    for {
      s <- ZIO.runtime[Clock].flatMap { implicit runtime =>
        Semaphore[Task](rl.n)
      }.toManaged_
      c <- ZIO.runtime[Clock].map(_.environment).toManaged_
      q <- Queue.unbounded[Long].toManaged_
      // Consume the queue by pulling entries, sleeping until ts, and calling .release on semaphore
      _ <- ZStream
        .fromQueue(q).foreach { ts =>
        (for {
          toSleep <- clock.nanoTime.map(ns => (ts - ns).nanoseconds)
          _ <- UIO.effectTotal(scribe.trace(s"Got $ts sleeping for ${toSleep.toMillis}ms"))
          _ <- clock.sleep(toSleep)
          _ <- s.release
          pm <- s.available.orDie
          _ <- UIO.effectTotal(scribe.trace(s"Released permit after ${toSleep.toMillis}ms $pm permits remaining"))
        } yield ()).exitCode
      }.forkManaged
    } yield new TokenBucket {
      override def withPermit[E <: Throwable, A](task: IO[E, A]): IO[E, A] = {
        // Try acquire a permit, execute task, push release timestamp onto the queue
        for {
          _ <- s.acquire.orDie
          pm <- s.available.orDie
          _ <- UIO.effectTotal(scribe.trace(s"Acquired permit for req $pm permits remaining"))
          res <- task
          t <- clock.nanoTime.provide(c).map(tt => tt + rl.t.toNanos + (rl.t.toNanos * rl.fuzzyDelayPercent).toLong)
          _ <- q.offer(t)
          _ <- UIO.effectTotal(scribe.trace(s"Pushed t=$t to release queue"))
        } yield res
      }

      override def acquire: UIO[Unit] = {
        for {
          _ <- s.acquire.orDie
          pm <- s.available.orDie
          _ <- UIO.effectTotal(scribe.trace(s"Acquired permit for req $pm permits remaining"))
        } yield ()
      }

      override def release: UIO[Unit] = {
        for {
          t <- clock.nanoTime.provide(c).map(tt => tt + rl.t.toNanos)
          _ <- q.offer(t)
          _ <- UIO.effectTotal(scribe.trace(s"Pushed t=$t to release queue"))
        } yield ()
      }
    }
  }
}
