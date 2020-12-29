package org.kys.athena.modules.ratelimiter

import zio._
import zio.clock.Clock
import zio.stream.ZStream
import zio.duration._
import cats.effect.concurrent.Semaphore
import org.kys.athena.riot.api.RateLimit
import org.kys.athena.util.ZCatsSemaphore
import zio.console.Console
import zio.interop.catz._

import scala.language.reflectiveCalls


trait TokenBucket {
  def acquire: UIO[Unit]

  def release: UIO[Unit]
}

object TokenBucket {

  def make(rl: RateLimit, initial: Int = 0): ZManaged[Clock with Console, Nothing, TokenBucket] = {
    // Allocate cats semaphore (explicit .acquire and .release are required), and an unbounded queue
    for {
      s <- ZCatsSemaphore.make(rl.n)
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
          _ <- UIO.effectTotal(
            scribe.trace(s"Released permit after ${toSleep.toMillis}ms $pm/${rl.n} permits remaining"))
        } yield ()).exitCode
      }.forkManaged
      tb <- ZManaged.effectTotal {
        new TokenBucket {
          override def acquire: UIO[Unit] = {
            for {
              _ <- s.acquire.orDie
              pm <- s.available.orDie
              _ <- UIO.effectTotal(scribe.trace(s"Acquired permit for req $pm/${rl.n} permits remaining"))
            } yield ()
          }

          override def release: UIO[Unit] = {
            for {
              t <- clock.nanoTime.provide(c).map(tt => tt + rl.t.toNanos)
              _ <- q.offer(t)
              _ <- UIO.effectTotal(scribe.trace(s"Pushed t=$t to release queue"))
            } yield ()
          }

          def acquireReleaseN(n: Int): UIO[Unit] = {
            for {
              _ <- UIO.collectAllPar(UIO.replicate(n)(acquire))
              _ <- UIO.collectAllPar(UIO.replicate(n)(release))
            } yield ()
          }
        }
      }
      _ <- tb.acquireReleaseN(initial).toManaged_
    } yield tb
  }
}
