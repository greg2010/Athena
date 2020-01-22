package org.kys.lolassistant.util

import io.github.resilience4j.ratelimiter.RateLimiter
import com.softwaremill.sttp.MonadError
import com.softwaremill.sttp.{Request, Response, SttpBackend}
import com.typesafe.scalalogging.LazyLogging

class RatelimitedSttpBackend[F[_], S](rateLimiterList: List[RateLimiter],
                                      delegate: SttpBackend[F, S]
                                     )(implicit monadError: MonadError[F]) extends SttpBackend[F, S] {

  override def send[T](request: Request[T, S]): F[Response[T]] = {
    RatelimitedSttpBackend.decorateF(rateLimiterList, delegate.send(request))
  }

  override def close(): Unit = delegate.close()

  override def responseMonad: MonadError[F] = delegate.responseMonad
}

object RatelimitedSttpBackend extends LazyLogging {
  def decorateF[F[_], T](rateLimiterList: List[RateLimiter],
                         service: => F[T])(implicit monadError: MonadError[F]): F[T] = {
    monadError.flatMap(monadError.unit(())){ _=>
      try {
        rateLimiterList.foreach { rateLimiter =>
          RateLimiter.waitForPermission(rateLimiter)
          rateLimiter.getEventPublisher.onFailure { event =>
            logger.warn(s"Failed to obtain rate limit permit at ${event.getCreationTime} rateLimiterName=${event.getRateLimiterName}")
          }
        }
        service
      } catch {
        case t: Throwable =>
          monadError.error(t)
      }
    }
  }
}