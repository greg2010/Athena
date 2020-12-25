package org.kys.athena.riot.api.ratelimit

import zio._
import zio.clock.Clock
import zio.console.Console


trait RateLimiter {
  def execute[E <: Throwable, A](job: IO[E, A]): IO[E, A]
}

object RateLimiter {
  def make(ll: List[RateLimit]): ZManaged[Clock with Console, Throwable, RateLimiter] = {
    ZManaged.foreach(ll)(TokenBucket.make).map { rll =>
      new RateLimiter {
        override def execute[E <: Throwable, A](job: IO[E, A]): IO[E, A] = {
          IO.bracket(IO.foreachPar_(rll)(rl => rl.acquire))(_ => IO.foreachPar_(rll)(rl => rl.release))(_ => job)
        }
      }
    }
  }
}
