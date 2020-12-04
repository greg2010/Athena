package org.kys.athena.util

import cats.effect.IO
import com.raquo.airstream.core.Observer


object IOToAS {
  type DataState[T] = Either[Throwable, Option[T]]

  def writeIOToObserver[T](io: IO[T], v: Observer[DataState[T]]): IO[Unit] = {
    io.attempt.flatMap {
      case Right(r) => IO.delay {
        v.onNext(Right(Some(r)))
      }
      case Left(ex) => IO.delay {
        scribe.error("Exception caught while running IO", ex)
        v.onNext(Left(ex))
      }
    }
  }
}
