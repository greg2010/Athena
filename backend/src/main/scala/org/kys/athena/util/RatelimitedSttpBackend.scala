package org.kys.athena.util

import java.nio.charset.StandardCharsets

import cats.effect.IO
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import io.github.resilience4j.ratelimiter.RateLimiter
import com.softwaremill.sttp.MonadError
import com.softwaremill.sttp.{Request, Response, SttpBackend}
import com.typesafe.scalalogging.LazyLogging
import org.kys.athena.api.Platform

import scala.concurrent.duration._

class RatelimitedSttpBackend[S](
    rateLimiterList: List[RateLimiter],
    delegate: SttpBackend[IO, S],
    cacheFor: Duration,
    cacheMaxCount: Long
)(implicit monadError: MonadError[IO])
    extends SttpBackend[IO, S]
    with LazyLogging {

  private def generateUrlId[T](r: Request[T, S]): String = {
    val text = r.uri.toString() + r.method.m
    val bytes = java.util.Base64.getEncoder.encode(text.getBytes())
    new String(bytes, StandardCharsets.US_ASCII)
  }

  private val cache: Cache[String, Response[_]] =
    Scaffeine()
      .recordStats()
      .expireAfterWrite(cacheFor)
      .maximumSize(cacheMaxCount)
      .build[String, Response[_]]()

  private val rateLimitersByRegion: Map[Platform, List[RateLimiter]] =
    Platform.values.map(p => (p, RiotRateLimiters.rateLimiters)).toMap

  override def send[T](request: Request[T, S]): IO[Response[T]] = delegate.send(request)

  def sendRatelimited[T](request: Request[T, S])(implicit platform: Platform): IO[Response[T]] = {
    RatelimitedSttpBackend.decorateF(rateLimitersByRegion(platform), this.send(request))
  }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def sendCachedRateLimited[T](request: Request[T, S])(implicit platform: Platform): IO[Response[T]] = {
    cache.getIfPresent(generateUrlId(request)) match {
      case Some(resp) =>
        logger.debug(s"Hit cache for request '${request.method.m} ${request.uri.toString()}'")

        IO.pure(resp.asInstanceOf[Response[T]])
      case None =>
        this.sendRatelimited(request).map {

          case r if r.is200 =>
            cache.put(generateUrlId(request), r)
            r
          case r => r
        }
    }
  }

  override def close(): Unit = delegate.close()

  override def responseMonad: MonadError[IO] = delegate.responseMonad
}

object RatelimitedSttpBackend extends LazyLogging {

  def decorateF[T](rateLimiterList: List[RateLimiter], service: => IO[T])(
      implicit monadError: MonadError[IO]): IO[T] = {
    monadError.flatMap(monadError.unit(())) { _ =>
      try {
        rateLimiterList.foreach { rateLimiter =>
          rateLimiter.getEventPublisher.onSuccess { event =>
            logger.trace(
              "Got permit at " +
                s"eventCreationTime=${event.getCreationTime} " +
                s"rateLimiterName=${event.getRateLimiterName} " +
                s"permitCount=${event.getNumberOfPermits}")
          }
          rateLimiter.getEventPublisher.onFailure { event =>
            logger.warn(
              s"Failed to obtain rate limit permit at " +
                s"eventCreationTime=${event.getCreationTime} " +
                s"rateLimiterName=${event.getRateLimiterName} " +
                s"permitCount=${event.getNumberOfPermits}")
          }
          RateLimiter.waitForPermission(rateLimiter)
        }
        service
      } catch {
        case t: Throwable =>
          monadError.error(t)
      }
    }
  }
}
