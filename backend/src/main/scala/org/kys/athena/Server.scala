package org.kys.athena

import java.util.concurrent.{Executors, ThreadFactory}

import cats.data.Kleisli
import cats.effect.{Blocker, ConcurrentEffect, ExitCode, IO, IOApp, Resource, Timer}
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpApp, HttpRoutes, Request, Response}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
import org.kys.athena.api.backends.CombinedSttpBackend
import org.kys.athena.api.ratelimit.{RegionalRateLimiter, RiotRateLimits}
import org.kys.athena.api.{RiotApi, RiotApiClient}
import org.kys.athena.controllers.{CurrentGameController, GroupController, PositionHeuristicsController}
import org.kys.athena.http.middleware.{ApacheLogging, ErrorHandler}
import org.kys.athena.http.routes.Base
import org.kys.athena.util.ThreadPools
import sttp.client.http4s.Http4sBackend

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}


/** This is the main entrypoint to the application.
  * Extending object StreamApp makes it runnable,
  * and .serve blocks the main thread until the JVM is shutdown.
  * Example is taken from [[https://http4s.org/v0.20/service/ HTTP4s documentation]] */
object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val resources = for {
      http4sClient <- prepareClient
      rateLimiter <- allocateRateLimiters
      riotApiClient <- prepareRiotApiClient(http4sClient)
      controllers <- prepareControllers(riotApiClient, http4sClient)
      svc <- prepareSvc(controllers._1, controllers._2, controllers._3)
      server <- prepareServer(svc)

    } yield (server)


    resources.use { server =>
      server
        .compile
        .drain
        .as(ExitCode.Success)
        .map { c =>
          System.exit(c.code)
          c
        }
    }
  }

  // Stage 0 - allocate thread pool(s)

  // Stage 1 - allocate client rate limiters
  def allocateRateLimiters: Resource[IO, RegionalRateLimiter[IO]] = {
    val rateLimits = LAConfig.riotApiIsProd match {
      case true => RiotRateLimits.prodRateLimit
      case false => RiotRateLimits.devRateLimit
    }

    RegionalRateLimiter.start(rateLimits)(ConcurrentEffect[IO], Timer[IO])
  }

  // Stage 2 - allocate http client
  def prepareClient: Resource[IO, CombinedSttpBackend[IO, Any]] = {
    val res = for {
      ec <- ThreadPools.allocateCached[IO](Some("client"))
      rl <- allocateRateLimiters
      bb <- Http4sBackend.usingDefaultClientBuilder[IO](Blocker.liftExecutionContext(ec._1), ec._1)(
        ConcurrentEffect[IO],
        IO.contextShift(ec._1))
    } yield (ec._1, rl, bb)

    res.evalMap { case (ec, rl, bb) =>
      IO.pure {
        new CombinedSttpBackend[IO, Any](rl,
                                         bb,
                                         LAConfig.cacheRiotRequestsFor.seconds,
                                         LAConfig.cacheRiotRequestsMaxCount)(IO.contextShift(ec))
      }
    }
  }

  // Stage 3 - allocate riot http client
  def prepareRiotApiClient(combinedSttpBackend: CombinedSttpBackend[IO, Any]): Resource[IO, RiotApiClient] = {
    Resource.pure[IO, RiotApiClient] {
      val riotApi = new RiotApi(LAConfig.riotApiKey)
      new RiotApiClient(riotApi)(combinedSttpBackend)
    }
  }

  // Stage 4 - allocate logic controllers
  def prepareControllers(riotApiClient: RiotApiClient,
                         combinedSttpBackend:
                         CombinedSttpBackend[IO, Any]): Resource[IO, (CurrentGameController, GroupController,
    PositionHeuristicsController)] = {
    Resource.pure[IO, (CurrentGameController, GroupController, PositionHeuristicsController)] {
      val cgc = new CurrentGameController(riotApiClient)
      val gc  = new GroupController(riotApiClient)
      val hc  = new PositionHeuristicsController(combinedSttpBackend)
      (cgc, gc, hc)
    }
  }

  // Stage 5 - allocate http routes (server)
  def prepareSvc(cgc: CurrentGameController,
                 gc: GroupController,
                 hc: PositionHeuristicsController): Resource[IO, Kleisli[IO, Request[IO], Response[IO]]] = {
    Resource.pure[IO, Kleisli[IO, Request[IO], Response[IO]]] {
      val baseRoutes: HttpRoutes[IO] = Base(cgc, gc, hc)
      val httpApp   : HttpRoutes[IO] = Router(LAConfig.http.prefix -> baseRoutes)
      ApacheLogging(CORS(ErrorHandler(httpApp))).orNotFound
    }
  }

  // Stage 6 - allocate http server
  def prepareServer(svc: HttpApp[IO]): Resource[IO, fs2.Stream[IO, ExitCode]] = {
    ThreadPools.allocateCached[IO](Some("server")).map {
      case (ec, _) =>
        BlazeServerBuilder[IO](executionContext = ec)(
          implicitly[ConcurrentEffect[IO]], IO.timer(ec))
          .bindHttp(port = LAConfig.http.port, host = LAConfig.http.host)
          .withIdleTimeout(6.minutes)
          .withResponseHeaderTimeout(5.minutes)
          .withHttpApp(svc)
          .serve
    }
  }
}
