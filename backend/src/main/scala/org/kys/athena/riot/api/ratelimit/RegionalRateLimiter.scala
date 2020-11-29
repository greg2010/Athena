package org.kys.athena.riot.api.ratelimit

import cats.data.NonEmptyList
import cats.effect.{Concurrent, ConcurrentEffect, Resource, Timer}
import cats.implicits._
import org.kys.athena.riot.api.Platform


class RegionalRateLimiter[F[_]](platformList: Map[Platform, RateLimiter[F]])(implicit F: Concurrent[F]) {

  def executePlatform[A](platform: Platform, job: F[A]): F[A] = {
    platformList(platform).execute(job)
  }
}


object RegionalRateLimiter {
  // Just a nice constructor of RegionalRateLimiter
  def start[F[_]](limitList: NonEmptyList[RateLimit])
                 (implicit F: ConcurrentEffect[F], timer: Timer[F]): Resource[F, RegionalRateLimiter[F]] = {
    val platforms = Platform.values
      .map(p => RateLimiter.start(limitList).map(re => (p, re)))
      .toList
      .sequence
      .map(_.toMap)

    platforms.map(new RegionalRateLimiter(_))
  }
}