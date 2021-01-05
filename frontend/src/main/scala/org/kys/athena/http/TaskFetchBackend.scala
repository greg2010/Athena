package org.kys.athena.http

import sttp.client3.{AbstractFetchBackend, FetchOptions}
import org.scalajs.dom.experimental.{BodyInit, Response, Request => FetchRequest}
import sttp.client3.internal.NoStreams
import zio._

import scala.scalajs.js
import scala.scalajs.js.{Promise, UndefOr}


class TaskFetchBackend(fetchOptions: FetchOptions, customizeRequest: FetchRequest => FetchRequest)
  extends AbstractFetchBackend[Task, Nothing, Any](fetchOptions, customizeRequest)(new TaskMonad) {
  override val streams: NoStreams = NoStreams

  override protected def addCancelTimeoutHook[T](result: Task[T], cancel: () => Unit): Task[T] = {
    result.tapBoth(_ => UIO.effectTotal(cancel()), _ => UIO.effectTotal(cancel()))
  }

  override protected def handleStreamBody(s: Nothing): Task[UndefOr[BodyInit]] = {
    ZIO.succeed(js.undefined)
  }

  override protected def handleResponseAsStream(response: Response): Task[(Nothing, () => Task[Unit])] = {
    Task.fail(new IllegalStateException("ZIO FetchBackend does not support streaming responses"))
  }

  override protected def transformPromise[T](promise: => Promise[T]): Task[T] = {
    ZIO.fromPromiseJS(promise)
  }
}

object TaskFetchBackend {
  def apply(fetchOptions: FetchOptions = FetchOptions.Default,
            customizeRequest: FetchRequest => FetchRequest = identity): TaskFetchBackend = {
    new TaskFetchBackend(fetchOptions, customizeRequest)
  }
}