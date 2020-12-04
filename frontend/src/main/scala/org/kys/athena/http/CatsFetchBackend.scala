package org.kys.athena.http

import cats.effect.{Concurrent, ContextShift, IO}
import org.scalajs.dom.experimental
import org.scalajs.dom.experimental.{BodyInit, Request => FetchRequest}
import sttp.client3.internal.NoStreams
import sttp.client3.{AbstractFetchBackend, FetchOptions}

import scala.scalajs.js
import scala.scalajs.js.{Promise, UndefOr}


class CatsFetchBackend private(fetchOptions: FetchOptions, customizeRequest: FetchRequest => FetchRequest)
                              (implicit F: Concurrent[IO], contextShift: ContextShift[IO])
  extends AbstractFetchBackend[IO, Nothing, Any](options = fetchOptions, customizeRequest = customizeRequest)(
    new EffectMonad[IO]) {
  override val streams: NoStreams = NoStreams

  override protected def addCancelTimeoutHook[T](result: IO[T],
                                                 cancel: () => Unit): IO[T] = result

  override protected def handleStreamBody(s: streams.BinaryStream): IO[UndefOr[BodyInit]] = {
    IO.pure(js.undefined)
  }

  override protected def handleResponseAsStream(response: experimental.Response): IO[(streams.BinaryStream, () =>
    IO[Unit])] = throw new IllegalStateException("IO FetchBackend does not support streaming responses")

  override protected def transformPromise[T](promise: => Promise[T]): IO[T] = {
    IO.fromFuture(IO.delay(promise.toFuture))
  }
}

object CatsFetchBackend {
  def apply(fetchOptions: FetchOptions = FetchOptions.Default,
            customizeRequest: FetchRequest => FetchRequest = identity)
           (implicit F: Concurrent[IO], contextShift: ContextShift[IO]): CatsFetchBackend = {
    new CatsFetchBackend(fetchOptions, customizeRequest)(F, contextShift)
  }
}