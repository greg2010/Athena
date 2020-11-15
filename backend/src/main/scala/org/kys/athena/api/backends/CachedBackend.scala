package org.kys.athena.api.backends

import java.nio.charset.StandardCharsets

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import org.kys.athena.api.Platform
import sttp.capabilities.Effect
import sttp.client.{Request, Response}
import sttp.monad.MonadError

import scala.concurrent.duration.FiniteDuration


trait CachedBackend[F[_], P] {
  val cacheFor: FiniteDuration
  val cacheMaxCount: Long

  def responseMonad: MonadError[F]

  private def generateUrlId[T, R >: P with Effect[F]](r: Request[T, R]): String = {
    val text  = r.uri.toString() + r.method.method
    val bytes = java.util.Base64.getEncoder.encode(text.getBytes())
    new String(bytes, StandardCharsets.US_ASCII)
  }

  private val cache: Cache[String, Response[_]] = Scaffeine().recordStats()
    .expireAfterWrite(cacheFor)
    .maximumSize(cacheMaxCount)
    .build[String, Response[_]]()

  protected def cacheRequest[T, R >: P with Effect[F]](request: Request[T, R],
                                                     sendFunc: Request[T, R] => F[Response[T]]): F[Response[T]] = {
    cache.getIfPresent(generateUrlId(request)) match {
      case Some(resp) => {
        scribe.debug(s"Hit cache for request '${request.method.method} ${request.uri.toString()}'")
        responseMonad.unit(resp.asInstanceOf[Response[T]])
      }
      case None => {
        responseMonad.map(sendFunc(request)) {
          case r if r.is200 => cache.put(generateUrlId(request), r)
            r
          case r => r
        }
      }
    }
  }
}
