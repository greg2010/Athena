package org.kys.athena

import org.kys.athena.modules.{CacheModule, ConfigModule, CurrentGameModule, GroupModule, LoggerModule, RiotApiModule}
import org.kys.athena.http.routes.LogicEndpoints
import org.kys.athena.meraki.api.MerakiApiClient
import sttp.client3.httpclient.zio.HttpClientZioBackend
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.kys.athena.modules.ConfigModule.ConfigModule
import org.kys.athena.http.middleware.ApacheLogging
import org.kys.athena.modules.ratelimiter.RateLimiter
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


    val zioClient = HttpClientZioBackend.layer()

    val config = ConfigModule.live
    val logger = config >>> LoggerModule.live // TODO: figure out a better way to configure logger

    val cache        = config >>> CacheModule.live
    val rrl          = (Console.live ++ Clock.live ++ config ++ logger) >>> RateLimiter.live
    val riotClient   = (config ++ zioClient ++ cache ++ rrl ++ Clock.live) >>> RiotApiModule.live
    val merakiClient = zioClient >>> MerakiApiClient.live
    val gc           = riotClient >>> GroupModule.live
    val cgc          = (riotClient ++ merakiClient) >>> CurrentGameModule.live

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
