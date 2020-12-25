package org.kys.athena.riot.api.backends

import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.riot.api.ratelimit.RegionalRateLimiter
import sttp.capabilities.Effect
import sttp.client3.{Request, Response}
import zio.Task


trait RateLimitedBackend[P] {
  protected val rateLimiter: RegionalRateLimiter

  protected def rateLimitRequest[T, R >: P with Effect[Task]](request: Request[T, R],
                                                              sendFunc: Request[T, R] => Task[Response[T]])
                                                             (implicit platform: Platform): Task[Response[T]] = {
    rateLimiter.executePlatform(platform, sendFunc(request))
  }
}
