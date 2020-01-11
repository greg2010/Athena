package org.red.lolassistant.util


import io.github.resilience4j.ratelimiter.RateLimiter
import com.softwaremill.sttp.MonadError
import com.softwaremill.sttp.{Request, Response, SttpBackend}

class RatelimitedSttpBackend[F[_], S](rateLimiter: RateLimiter,
                                      delegate: SttpBackend[F, S]
                                     )(implicit monadError: MonadError[F]) extends SttpBackend[F, S] {

  override def send[T](request: Request[T, S]): F[Response[T]] = {
    RateLimitingSttpBackend.decorateF(rateLimiter, delegate.send(request))
  }

  override def close(): Unit = delegate.close()

  override def responseMonad: MonadError[F] = delegate.responseMonad
}

object RateLimitingSttpBackend {
  def decorateF[F[_], T](rateLimiter: RateLimiter,
                         service: => F[T])(implicit monadError: MonadError[F]): F[T] = {
    monadError.flatMap(monadError.unit(())){ _=>
      try {
        RateLimiter.waitForPermission(rateLimiter)
        service
      } catch {
        case t: Throwable =>
          monadError.error(t)
      }
    }
  }
}