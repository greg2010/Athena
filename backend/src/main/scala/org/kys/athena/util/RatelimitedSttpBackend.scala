package org.kys.athena.util

import java.nio.charset.StandardCharsets

import cats.effect.IO
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import io.github.resilience4j.ratelimiter.RateLimiter
import sttp.client.{Request, Response, SttpBackend}
import com.typesafe.scalalogging.LazyLogging
import org.kys.athena.api.Platform
import sttp.capabilities.Effect
import sttp.client.impl.cats.implicits._
import sttp.monad.MonadError

import scala.concurrent.duration._


class RatelimitedSttpBackend[P](rateLimiterList: List[RateLimiter],
                                delegate: IO[SttpBackend[IO, P]],
                                cacheFor: FiniteDuration,
                                cacheMaxCount: Long)
                               (implicit monadError: MonadError[IO])
  extends SttpBackend[IO, P] with LazyLogging {

  private def generateUrlId[T, R >: P with Effect[IO]](r: Request[T, R]): String = {
    val text  = r.uri.toString() + r.method.method
    val bytes = java.util.Base64.getEncoder.encode(text.getBytes())
    new String(bytes, StandardCharsets.US_ASCII)
  }

  private val cache: Cache[String, Response[_]] = Scaffeine().recordStats()
    .expireAfterWrite(cacheFor)
    .maximumSize(cacheMaxCount)
    .build[String, Response[_]]()

  private val rateLimitersByRegion: Map[Platform, List[RateLimiter]] = Platform
    .values
    .map(p => (p, RiotRateLimiters.rateLimiters))
    .toMap

  override def send[T, R >: P with Effect[IO]](request: Request[T, R]): IO[Response[T]] = {
    delegate.flatMap { d =>
      d.send(request)
    }
  }

  def sendRatelimited[T, R >: P with Effect[IO]](request: Request[T, R])
                                                (implicit platform: Platform): IO[Response[T]] = {
    RatelimitedSttpBackend.decorateF(rateLimitersByRegion(platform), this.send(request))
  }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def sendCachedRateLimited[T, R >: P with Effect[IO]](request: Request[T, R])(implicit platform: Platform)
  : IO[Response[T]] = {
    cache.getIfPresent(generateUrlId(request)) match {
      case Some(resp) => {
        logger.debug(s"Hit cache for request '${request.method.method} ${request.uri.toString()}'")

        IO.pure(resp.asInstanceOf[Response[T]])
      }
      case None => {
        this.sendRatelimited(request).map {
          case r if r.is200 => cache.put(generateUrlId(request), r)
            r
          case r => r
        }
      }
    }
  }

  override def close(): IO[Unit] = delegate.map(_.close())

  override def responseMonad: MonadError[IO] = monadError
}

object RatelimitedSttpBackend extends LazyLogging {

  def decorateF[T](rateLimiterList: List[RateLimiter], service: => IO[T])
                  (implicit monadError: MonadError[IO]): IO[T] = {
    monadError.flatMap(monadError.unit(())) { _ =>
      try {
        rateLimiterList.foreach { rateLimiter =>
          rateLimiter.getEventPublisher.onSuccess { event =>
            logger.trace("Got permit at " + s"eventCreationTime=${event.getCreationTime} " +
                         s"rateLimiterName=${event.getRateLimiterName} " + s"permitCount=${event.getNumberOfPermits}")
          }
          rateLimiter.getEventPublisher.onFailure { event =>
            logger.warn(s"Failed to obtain rate limit permit at " + s"eventCreationTime=${event.getCreationTime} " +
                        s"rateLimiterName=${event.getRateLimiterName} " + s"permitCount=${event.getNumberOfPermits}")
          }
          RateLimiter.waitForPermission(rateLimiter)
        }
        service
      } catch {
        case t: Throwable => monadError.error(t)
      }
    }
  }
}
