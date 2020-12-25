package org.kys.athena.riot.api.ratelimit

import cats.effect.Concurrent
import org.kys.athena.config.ConfigModule
import org.kys.athena.config.ConfigModule.ConfigModule
import zio._
import org.kys.athena.riot.api.dto.common.Platform
import zio.clock.Clock
import zio.console.Console


trait RegionalRateLimiter {
  def executePlatform[E <: Throwable, A](platform: Platform, job: IO[E, A]): IO[E, A]
}


object RegionalRateLimiter {
  def make(ll: List[RateLimit]): ZManaged[Clock with Console, Throwable, RegionalRateLimiter] = {
    ZManaged.foreach(Platform.values)(p => RateLimiter.make(ll).map(rl => (p, rl))).map(_.toMap).map { rlm =>
      new RegionalRateLimiter {
        override def executePlatform[E <: Throwable, A](platform: Platform, job: IO[E, A]): IO[E, A] = {
          rlm(platform).execute(job)
        }
      }
    }
  }

  def layer: ZLayer[Clock with Console with ConfigModule, Throwable, Has[RegionalRateLimiter]] = {
    ZLayer.fromServiceManyManaged[ConfigModule.Service, Clock with Console, Throwable, Has[RegionalRateLimiter]] { c =>
      RegionalRateLimiter.make(c.loaded.rateLimitList).map(Has(_))
    }
  }
}