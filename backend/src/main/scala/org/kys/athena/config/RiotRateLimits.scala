package org.kys.athena.config

import org.kys.athena.riot.api.ratelimit.RateLimit

import scala.concurrent.duration.DurationInt


object RiotRateLimits {

  // Taken from https://developer.riotgames.com/docs/portal#web-apis_rate-limiting
  val devRateLimit = List(
    RateLimit(20, 1.second),
    RateLimit(100, 2.minutes)
    )

  val prodRateLimit = List(
    RateLimit(500, 10.second),
    RateLimit(30000, 10.minutes)
    )
}
