package org.kys.athena.riot.api.ratelimit


import cats.effect.concurrent.Semaphore
import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, IO, Resource, Timer}
import cats.implicits._
import org.kys.athena.util.ThreadPools

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration


trait TokenBucket[F[_]] {
  def execute[A](job: F[A]): F[A]
}


object TokenBucket {
  def start[F[_]](rateLimit: RateLimit)(implicit F: ConcurrentEffect[F]): Resource[F, TokenBucket[F]] = {
    allocateTimer.evalMap {
      case (t, ec) =>
        Semaphore[F](rateLimit.n).map { bucket =>
          new TokenBucket[F] {
            override def execute[A](job: F[A]): F[A] = {
              for {
                c <- bucket.count
                _ <- F.delay {
                  if (c == 0) scribe.warn(s"Hit ratelimit for limiterCount=${rateLimit.n} delay=${rateLimit.t}")
                  else ()
                }
                _ <- F.liftIO(IO.shift(ec) *> F.toIO(bucket.acquire))
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
  : Resource[F, (Timer[IO], ExecutionContext)] = {
    ThreadPools.allocateScheduled(Some("rate")).map { case (ec, sc) => (IO.timer(ec, sc), ec) }
  }
}
