package org.kys.athena.api.ratelimit


import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import cats.effect.concurrent.{Deferred, Semaphore}
import cats.effect.{Clock, Concurrent, ConcurrentEffect, ContextShift, Fiber, IO, Resource, SyncIO, Timer}
import cats.implicits._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
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
                t.sleep(d).flatMap (_ => F.toIO(bucket.release)).unsafeRunAsyncAndForget()
            }
          }
        }
    }
  }


  private def allocateTimer[F[_]](implicit F: Concurrent[F])
  : Resource[F, Timer[IO]] =
    allocateThreadPool.map { case (ec, sc) => IO.timer(ec, sc) }


  private def allocateThreadPool[F[_]](implicit F: Concurrent[F]): Resource[F, (ExecutionContextExecutorService, ScheduledExecutorService)] = {
    Resource.make {
      F.delay {
        val sc = Executors.newScheduledThreadPool(4)
        val ec = ExecutionContext.fromExecutorService(sc)
        (ec, sc)
      }
    } {
      case (sc, ec) => F.delay(ec.shutdownNow()).map(_ => ())
    }
  }
}
