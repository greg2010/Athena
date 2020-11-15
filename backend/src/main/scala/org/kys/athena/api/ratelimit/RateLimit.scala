package org.kys.athena.api.ratelimit

import scala.concurrent.duration.FiniteDuration


case class RateLimit(n: Int, t: FiniteDuration)
