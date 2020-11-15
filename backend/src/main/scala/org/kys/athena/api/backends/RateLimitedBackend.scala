package org.kys.athena.api.backends

import org.kys.athena.api.Platform
import org.kys.athena.api.ratelimit.RegionalRateLimiter
import sttp.capabilities.Effect
import sttp.client.{Request, Response}


trait RateLimitedBackend[F[_], P] {
  protected val rateLimiter: RegionalRateLimiter[F]

  protected def rateLimitRequest[T, R >: P with Effect[F]](request: Request[T, R],
                                                         sendFunc: Request[T, R] => F[Response[T]])
                                                        (implicit platform: Platform): F[Response[T]] = {
    rateLimiter.executePlatform(platform, sendFunc(request))
  }

}
