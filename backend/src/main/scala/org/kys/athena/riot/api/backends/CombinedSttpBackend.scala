package org.kys.athena.riot.api.backends


import cats.effect.{ConcurrentEffect, ContextShift}
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.riot.api.ratelimit.RegionalRateLimiter
import sttp.capabilities.Effect
import sttp.client.{Request, Response, SttpBackend}
import sttp.monad.MonadError

import scala.concurrent.duration._


class CombinedSttpBackend[F[_], P](override val rateLimiter: RegionalRateLimiter[F],
                                   delegate: SttpBackend[F, P],
                                   override val cacheFor: FiniteDuration,
                                   override val cacheMaxCount: Long)
                                  (implicit cs: ContextShift[F])
  extends SttpBackend[F, P] with CachedBackend[F, P] with RateLimitedBackend[F, P] {
  override def responseMonad: MonadError[F] = delegate.responseMonad

  override def send[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] = {
    responseMonad.flatMap(cs.shift)(_ => delegate.send(request))
  }

  def sendRatelimited[T, R >: P with Effect[F]](request: Request[T, R])
                                               (implicit platform: Platform): F[Response[T]] = {
    rateLimitRequest(request, this.send)
  }

  def sendCached[T, R >: P with Effect[F]](request: Request[T, R]): F[Response[T]] = {
    cacheRequest(request, this.send)
  }

  def sendCachedRateLimited[T, R >: P with Effect[F]](request: Request[T, R])(implicit platform: Platform)
  : F[Response[T]] = {
    cacheRequest(request, this.sendRatelimited)
  }

  override def close(): F[Unit] = delegate.close()

}
