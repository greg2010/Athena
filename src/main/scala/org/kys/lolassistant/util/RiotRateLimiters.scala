package org.kys.lolassistant.util

import java.time.Duration

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiter

object RiotRateLimiters {
  private val confOneSec = RateLimiterConfig.custom()
    .timeoutDuration(Duration.ofMillis(100))
    .limitRefreshPeriod(Duration.ofSeconds(1))
    .limitForPeriod(20)
    .build();

  private val confTwoMin = RateLimiterConfig.custom()
    .timeoutDuration(Duration.ofMillis(100))
    .limitRefreshPeriod(Duration.ofSeconds(120))
    .limitForPeriod(100)
    .build();


  val rateLimiters = List(RateLimiter.of("riotAPIOneSec", confOneSec), RateLimiter.of("RiotAPITwoMin", confTwoMin))
}
