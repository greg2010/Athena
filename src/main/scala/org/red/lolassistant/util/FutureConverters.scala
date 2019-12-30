package org.red.lolassistant.util

import java.lang.reflect.Type

import net.rithms.riot.api.RiotApiException
import net.rithms.riot.api.request.{AsyncRequest, RequestAdapter}
import cats.effect.{ContextShift, IO}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}
import scala.language.implicitConversions

object FutureConverters extends LazyLogging {
  private class FutureRequestListener[T](p: Promise[T]) extends RequestAdapter {
    override def onRequestSucceeded(request: AsyncRequest): Unit = p.success(request.getDto[T])
    override def onRequestFailed(e: RiotApiException): Unit = p.failure(e)
    override def onRequestTimeout(request: AsyncRequest): Unit = p.failure(new TimeoutException())
  }

  private class IORequestListener[T](cb: Either[Throwable, T] => Unit) extends RequestAdapter {
    override def onRequestSucceeded(request: AsyncRequest): Unit = cb(Right(request.getDto[T]))
    override def onRequestFailed(e: RiotApiException): Unit = cb(Left(e))
    override def onRequestTimeout(request: AsyncRequest): Unit = cb(Left(new TimeoutException()))
  }

  implicit def requestToScalaFuture[T](r: AsyncRequest)(implicit ec: ExecutionContext): Future[T] = {
  val p = Promise[T]
    r.addListeners(new FutureRequestListener[T](p))
    p.future
  }

  implicit def requestToIOTask[T](r: AsyncRequest)(implicit cs: ContextShift[IO]): IO[T] = {
    IO.suspend {
      IO.async[T] { cb =>
        r.addListeners(new IORequestListener[T](cb))
      }
    }
  }
}
