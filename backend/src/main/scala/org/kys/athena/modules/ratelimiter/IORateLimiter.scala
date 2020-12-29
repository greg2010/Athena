package org.kys.athena.modules.ratelimiter

import org.kys.athena.riot.api.RateLimitInitial
import zio._
import zio.clock.Clock
import zio.console.Console


trait IORateLimiter {
  def execute[E, A](job: IO[E, A]): IO[E, A]
}

object IORateLimiter {

  def make(ll: List[RateLimitInitial]): ZManaged[Clock with Console, Nothing, IORateLimiter] = {
    ZManaged.foreach(ll)(e => TokenBucket.make(e.rl, e.initial)).map { rll =>
      new IORateLimiter {
        override def execute[E, A](job: IO[E, A]): IO[E, A] = {
          val acquireAll = IO.foreachPar_(rll)(rl => rl.acquire)
          val releaseAll = IO.foreachPar_(rll)(rl => rl.release)

          IO.bracket(acquireAll)(_ => releaseAll)(_ => job)
        }
      }
    }
  }
}
