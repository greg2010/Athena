package org.kys.athena.riot.api.ratelimit

import cats.data.NonEmptyList

import scala.concurrent.duration.DurationInt


object RiotRateLimits {

  // Taken from https://developer.riotgames.com/docs/portal#web-apis_rate-limiting
  val devRateLimit = NonEmptyList.of(
    RateLimit(20, 1.second),
    RateLimit(100, 2.minutes)
    )

  val prodRateLimit = NonEmptyList.of(
    RateLimit(500, 10.second),
    RateLimit(30000, 10.minutes)
    )
}
