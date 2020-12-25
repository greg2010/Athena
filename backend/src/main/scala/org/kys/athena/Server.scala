package org.kys.athena


import org.kys.athena.config.{ConfigModule, LoggerConfig, RiotRateLimits}
import org.kys.athena.controllers.{CurrentGameController, GroupController}
import org.kys.athena.http.routes.LogicEndpoints
import org.kys.athena.meraki.api.MerakiApiClient
import org.kys.athena.riot.api.backends.CombinedSttpBackend
import org.kys.athena.riot.api.RiotApiClient
import sttp.capabilities
import sttp.capabilities.zio.ZioStreams
import sttp.client3.SttpBackend
import sttp.client3.httpclient.zio.HttpClientZioBackend
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.kys.athena.config.ConfigModule.ConfigModule
import org.kys.athena.http.middleware.ApacheLogging
import org.kys.athena.riot.api.ratelimit.{RateLimit, RegionalRateLimiter}
import scribe.filter.{level => flevel, _}
import scribe.{Level, Logger}
import scribe.format._
import zio.clock.Clock
import zio.interop.catz._
import zio._
import zio.console.Console

import scala.concurrent.duration._


/** This is the main entrypoint to the application.
  * Extending object StreamApp makes it runnable,
  * and .serve blocks the main thread until the JVM is shutdown.
  * Example is taken from [[https://http4s.org/v0.20/service/ HTTP4s documentation]] */
object Server extends App {
  type AppRuntime = LogicEndpoints.Env with Clock
  type AppTask[A] = RIO[AppRuntime, A]

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {


    val defaultZioBackend = ZLayer.fromManaged(HttpClientZioBackend.managed())

    val config = ConfigModule.live
    val logger = config >>> LoggerConfig.layer // TODO: figure out a better way to configure logger

    val rrl          = (Console.live ++ Clock.live ++ config ++ logger) >>> RegionalRateLimiter.layer
    val httpClient   = (config ++ defaultZioBackend ++ rrl) >>> CombinedSttpBackend.layer
    val riotClient   = (config ++ httpClient) >>> RiotApiClient.live
    val merakiClient = httpClient >>> MerakiApiClient.live
    val gc           = riotClient >>> GroupController.live
    val cgc          = (riotClient ++ merakiClient) >>> CurrentGameController.live

    allocateHttpServer.provideCustomLayer(cgc ++ gc ++ config).exitCode
  }

  def allocateHttpServer: ZIO[AppRuntime with ConfigModule, Throwable, Unit] = {
    ZIO.runtime[AppRuntime with ConfigModule]
      .flatMap { implicit runtime =>

        val config = runtime.environment.get[ConfigModule.Service].loaded

        val routes: HttpRoutes[AppTask] = Router(config.http.prefix -> LogicEndpoints.routes)
        val svc                         = ApacheLogging(CORS(routes)).orNotFound

        BlazeServerBuilder[AppTask](runtime.platform.executor.asEC)
          .bindHttp(port = config.http.port, host = config.http.host)
          .withIdleTimeout(6.minutes)
          .withResponseHeaderTimeout(5.minutes)
          .withHttpApp(svc)
          .serve.compile.drain
      }
  }
}
