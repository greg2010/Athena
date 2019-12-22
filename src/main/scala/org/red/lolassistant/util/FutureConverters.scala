package org.red.lolassistant.util

import net.rithms.riot.api.RiotApiException
import net.rithms.riot.api.request.{AsyncRequest, RequestAdapter}

import scala.concurrent.{Future, Promise, TimeoutException}
import scala.language.implicitConversions

object FutureConverters {
  private class RequestListener[T](p: Promise[T]) extends RequestAdapter {
    override def onRequestSucceeded(request: AsyncRequest): Unit = p.success(request.getDto[T])
    override def onRequestFailed(e: RiotApiException): Unit = p.failure(e)
    override def onRequestTimeout(request: AsyncRequest): Unit = p.failure(new TimeoutException())
  }

  implicit def requestToScalaFuture[T](r: AsyncRequest): Future[T] = {
  val p = Promise[T]
    r.addListeners(new RequestListener[T](p))
    p.future
  }
}
