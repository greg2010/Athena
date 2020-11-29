package org.kys.athena.riot.api.ratelimit


import cats.effect.concurrent.Semaphore
import cats.effect.{Concurrent, ConcurrentEffect, IO, Resource, Timer}
import cats.implicits._
import org.kys.athena.util.ThreadPools

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS, NANOSECONDS}


trait TokenBucket[F[_]] {
  def execute[A](job: F[A]): F[A]
}


object TokenBucket {
  def start[F[_]](rateLimit: RateLimit)(implicit F: ConcurrentEffect[F]): Resource[F, TokenBucket[F]] = {
    allocateTimer.evalMap { t =>
        Semaphore[F](rateLimit.n).map { bucket =>
          new TokenBucket[F] {
            override def execute[A](job: F[A]): F[A] = {
              for {
                _ <- bucket.acquire
                j <- job
                _ <- F.pure(releaseAfter(rateLimit.t))
              } yield j
            }

            private def releaseAfter(d: FiniteDuration): Unit = {
              t.sleep(d).flatMap(_ => F.toIO(bucket.release)).unsafeRunAsyncAndForget()
            }
          }
        }
    }
  }


  private def allocateTimer[F[_]](implicit F: Concurrent[F])
  : Resource[F, Timer[IO]] = {
    ThreadPools.allocateScheduled(Some("rate")).map { case (ec, sc) => IO.timer(ec, sc) }
  }
}
