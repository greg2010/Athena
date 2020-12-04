package org.kys.athena.util

import cats.effect.{Concurrent, Resource}
import cats.implicits._
import com.google.common.util.concurrent.ThreadFactoryBuilder

import java.util.concurrent.{ExecutorService, Executors, ScheduledExecutorService}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}


object ThreadPools {
  def allocateCached[F[_]](threadPoolName: Option[String] = None)
                          (implicit F: Concurrent[F]): Resource[F, (ExecutionContextExecutorService, ExecutorService)
  ] = {
    Resource.make {
      F.delay {
        val tfb = threadPoolName match {
          case Some(name) => new ThreadFactoryBuilder().setNameFormat(s"$name-pool-%d").build()
          case None => new ThreadFactoryBuilder().build()
        }
        val sc = Executors.newCachedThreadPool(tfb)
        val ec = ExecutionContext.fromExecutorService(sc)
        (ec, sc)
      }
    } {
      case (ec, sc) => F.delay(ec.shutdownNow()).map(_ => ())
    }
  }


  def allocateScheduled[F[_]](threadPoolName: Option[String] = None)
                                     (implicit F: Concurrent[F]): Resource[F, (ExecutionContextExecutorService, ScheduledExecutorService)] = {
    Resource.make {
      F.delay {
        val tfb = threadPoolName match {
          case Some(name) => new ThreadFactoryBuilder().setNameFormat(s"$name-pool-%d").build()
          case None => new ThreadFactoryBuilder().build()
        }
        val sc = Executors.newScheduledThreadPool(4, tfb)
        val ec = ExecutionContext.fromExecutorService(sc)
        (ec, sc)
      }
    } {
      case (ec, sc) => F.delay(ec.shutdownNow()).map(_ => ())
    }
  }
}
