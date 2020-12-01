package org.kys.athena.util

import cats.effect.IO
import com.raquo.airstream.signal.Var


object IOToAS {
  def writeIOToVar[T](io: IO[T],
                      v: Var[Option[T]]): IO[Unit] = {
    io.redeemWith (ex => IO.delay {
      scribe.error("Exception caught while running IO", ex)
      v.setError(ex)
    }, res => IO.delay {
      scribe.debug(s"Writing IO to var res=${res}")
      v.set(Some(res))
    })
  }
}
