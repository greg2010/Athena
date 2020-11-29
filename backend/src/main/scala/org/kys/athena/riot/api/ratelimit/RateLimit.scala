package org.kys.athena.riot.api.ratelimit

import scala.concurrent.duration.FiniteDuration


case class RateLimit(n: Int, t: FiniteDuration)
