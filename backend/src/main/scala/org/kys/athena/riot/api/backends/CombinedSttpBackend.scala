package org.kys.athena.riot.api.backends


import org.kys.athena.config.ConfigModule
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.riot.api.ratelimit.RegionalRateLimiter
import sttp.capabilities
import sttp.capabilities.Effect
import sttp.capabilities.zio.ZioStreams
import sttp.client3.{Request, Response, SttpBackend}
import sttp.monad.MonadError
import zio.{Has, Task, ZLayer}

import scala.concurrent.duration._


class CombinedSttpBackend[P](override val rateLimiter: RegionalRateLimiter,
                             delegate: SttpBackend[Task, P],
                             override val cacheFor: FiniteDuration,
                             override val cacheMaxCount: Long)
  extends SttpBackend[Task, P] with CachedBackend[Task, P] with RateLimitedBackend[P] {
  override def responseMonad: MonadError[Task] = delegate.responseMonad

  override def send[T, R >: P with Effect[Task]](request: Request[T, R]): Task[Response[T]] = {
    delegate.send(request)
  }

  def sendRatelimited[T, R >: P with Effect[Task]](request: Request[T, R])
                                                  (implicit platform: Platform): Task[Response[T]] = {
    rateLimitRequest(request, this.send)
  }

  def sendCached[T, R >: P with Effect[Task]](request: Request[T, R]): Task[Response[T]] = {
    cacheRequest(request, this.send)
  }

  def sendCachedRateLimited[T, R >: P with Effect[Task]](request: Request[T, R])(implicit platform: Platform)
  : Task[Response[T]] = {
    cacheRequest(request, this.sendRatelimited)
  }

  override def close(): Task[Unit] = delegate.close()
}

object CombinedSttpBackend {
  def layer: ZLayer[Has[SttpBackend[Task, ZioStreams with capabilities.WebSockets]] with
    Has[ConfigModule.Service] with Has[RegionalRateLimiter], Nothing, Has[CombinedSttpBackend[Any]]] = {
    ZLayer.fromFunction[
      Has[SttpBackend[Task, ZioStreams with capabilities.WebSockets]] with
        Has[ConfigModule.Service] with
        Has[RegionalRateLimiter], CombinedSttpBackend[Any]] { env =>
      val backend = env.get[SttpBackend[Task, ZioStreams with capabilities.WebSockets]]
      val config  = env.get[ConfigModule.Service]
      val rrl     = env.get[RegionalRateLimiter]
      new CombinedSttpBackend[Any](rrl,
                                   backend,
                                   config.loaded.cacheRiotRequestsFor.seconds,
                                   config.loaded.cacheRiotRequestsMaxCount)
    }
  }
}