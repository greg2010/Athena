package org.kys.athena.util

import zio._
import zio.clock.Clock
import cats.effect.concurrent.Semaphore
import zio.interop.catz._

object ZCatsSemaphore {

  def make(permits: Int): ZManaged[Clock, Nothing, Semaphore[Task]] = {
    ZIO.runtime[Clock].flatMap { implicit runtime =>
      Semaphore[Task](permits)
    }.toManaged_.orDie
  }
}
