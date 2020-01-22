package org.kys.lolassistant

import java.time.Duration
import java.util.concurrent.Executors

import org.http4s.dsl.io._
import cats.data.Kleisli
import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import org.http4s.dsl.io.{->, /, GET, Ok, Root}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpRoutes, Request, Response}
import cats.implicits._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import org.http4s.implicits._
import org.http4s.server.Router
import org.kys.lolassistant.api.RiotApi
import org.kys.lolassistant.http.middleware.{ApacheLogging, ErrorHandler}
import org.kys.lolassistant.http.routes.Base
import org.kys.lolassistant.util.RatelimitedSttpBackend
import org.kys.lolassistant.api.RiotApi
import org.kys.lolassistant.http.middleware.{ApacheLogging, ErrorHandler}
import org.kys.lolassistant.http.routes
import org.kys.lolassistant.http.routes.Base
import org.kys.lolassistant.util.RatelimitedSttpBackend
import org.kys.lolassistant.util.RiotRateLimiters

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/** This is the main entrypoint to the application.
 * Extending object StreamApp makes it runnable,
 * and .serve blocks the main thread until the JVM is shutdown.
 * Example is taken from [[https://http4s.org/v0.20/service/ HTTP4s documentation]]
 */
object Server extends IOApp {


  // Setup HTTP client
  val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(blockingEC)
  import com.softwaremill.sttp.impl.cats.AsyncMonadAsyncError
  implicit val ratelimitedSttpBackend: RatelimitedSttpBackend[Nothing] =
    new RatelimitedSttpBackend[Nothing](
      RiotRateLimiters.rateLimiters, AsyncHttpClientCatsBackend[cats.effect.IO](),
      LAConfig.cacheRiotRequestsFor.seconds, LAConfig.cacheRiotRequestsMaxCount)(new AsyncMonadAsyncError[IO]())

  // Setup Riot API
  val riotApi = new RiotApi(LAConfig.riotApiKey)
  val riotApiClient = new RiotApiClient(riotApi)

  // Setup business logic
  val pc = new PremadeController(riotApiClient)

  // Setup HTTP server
  val baseRoutes: HttpRoutes[IO] = Base(pc)
  val httpApp: HttpRoutes[IO] = Router(LAConfig.http.prefix -> baseRoutes)
  val svc: Kleisli[IO, Request[IO], Response[IO]] = ApacheLogging(ErrorHandler(httpApp)).orNotFound;
  def run(args: List[String]): IO[ExitCode] = {
    BlazeServerBuilder[IO]
      .bindHttp(port = LAConfig.http.port, host = LAConfig.http.host)
      .withIdleTimeout(5.minutes)
      .withResponseHeaderTimeout(5.minutes)
      .withHttpApp(svc)
      .serve
      .compile
      .drain
      .as(ExitCode.Success).map { c =>
      System.exit(c.code)
      c
    }
  }
}
