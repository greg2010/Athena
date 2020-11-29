package org.kys.athena


import cats.data.Kleisli
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpApp, HttpRoutes, Request, Response}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
import org.kys.athena.riot.api.backends.CombinedSttpBackend
import org.kys.athena.riot.api.ratelimit.{RegionalRateLimiter, RiotRateLimits}
import org.kys.athena.riot.api.{RiotApi, RiotApiClient}
import org.kys.athena.controllers.{CurrentGameController, GroupController}
import org.kys.athena.http.middleware.{ApacheLogging, ErrorHandler}
import org.kys.athena.http.routes.Base
import org.kys.athena.meraki.api.{MerakiApi, MerakiApiClient}
import org.kys.athena.util.ThreadPools
import sttp.client.http4s.Http4sBackend

import scala.concurrent.duration._


/** This is the main entrypoint to the application.
  * Extending object StreamApp makes it runnable,
  * and .serve blocks the main thread until the JVM is shutdown.
  * Example is taken from [[https://http4s.org/v0.20/service/ HTTP4s documentation]] */
object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val resources = for {
      http4sClient <- prepareClient
      apiClients <- prepareApiClients(http4sClient)
      controllers <- prepareControllers(apiClients._1, apiClients._2)
      svc <- prepareSvc(controllers._1, controllers._2)
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

  // Stage 3 - allocate http clients
  def prepareApiClients(combinedSttpBackend: CombinedSttpBackend[IO, Any])
  : Resource[IO, (RiotApiClient, MerakiApiClient)] = {
    Resource.pure[IO, (RiotApiClient, MerakiApiClient)] {
      val riotApi   = new RiotApi(LAConfig.riotApiKey)
      val rac       = new RiotApiClient(riotApi)(combinedSttpBackend)
      val merakiApi = new MerakiApi
      val mac       = new MerakiApiClient(merakiApi)(combinedSttpBackend)
      (rac, mac)
    }
  }

  // Stage 4 - allocate logic controllers
  def prepareControllers(riotApiClient: RiotApiClient,
                         merakiApiClient: MerakiApiClient)
  : Resource[IO, (CurrentGameController, GroupController)] = {
    ThreadPools.allocateCached(Some("Controller"))(implicitly[ConcurrentEffect[IO]]).map { tp =>
      val cgc = new CurrentGameController(riotApiClient, merakiApiClient)(implicitly[ContextShift[IO]])
      val gc  = new GroupController(riotApiClient)(implicitly[ContextShift[IO]])
      (cgc, gc)
    }
  }

  // Stage 5 - allocate http routes (server)
  def prepareSvc(cgc: CurrentGameController,
                 gc: GroupController): Resource[IO, Kleisli[IO, Request[IO], Response[IO]]] = {
    Resource.pure[IO, Kleisli[IO, Request[IO], Response[IO]]] {
      val baseRoutes: HttpRoutes[IO] = Base(cgc, gc)
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
