package org.red.lolassistant

import java.time.Duration
import java.util.logging.Level

import cats.effect.{ContextShift, IO}
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import net.rithms.riot.api.ApiConfig
import net.rithms.riot.api.endpoints.`match`.dto.{Match, MatchList, Participant}
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo
import net.rithms.riot.api.endpoints.summoner.dto.Summoner
import net.rithms.riot.api.request.ratelimit.BufferedRateLimitHandler
import org.red.lolassistant.api.{Platform, RiotApi}
import org.red.lolassistant.util.{AtomicRateLimitHandler, RatelimitedSttpBackend}

import scala.jdk.CollectionConverters._
import scala.concurrent.{Await, Future, Promise, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global
import org.red.lolassistant.util.FutureConverters._



object Test extends App {

  val config = RateLimiterConfig.custom()
    .timeoutDuration(Duration.ofMillis(100))
    .limitRefreshPeriod(Duration.ofSeconds(1))
    .limitForPeriod(20)
    .build();

  import io.github.resilience4j.ratelimiter.RateLimiter

  val rateLimiter = RateLimiter.of("sttpBackend", config)

  implicit val cs = IO.contextShift(global)
  import com.softwaremill.sttp.impl.cats.AsyncMonadAsyncError
  implicit val ratelimitedSttpBackend: RatelimitedSttpBackend[IO, Nothing] = new RatelimitedSttpBackend[IO, Nothing](rateLimiter, AsyncHttpClientCatsBackend[cats.effect.IO]())(new AsyncMonadAsyncError[IO]())

  val riotApi = new RiotApi("RGAPI-2d1746f0-4c91-4658-838b-b7c2bba857b0")
  //implicit val sttpBackend: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend[cats.effect.IO]()

  val riotApiClient = new RiotApiClient
  val pc = new PremadeController(riotApiClient)
  val premadeTask = pc.getPremades(Platform.NA, "dd god", 5)
  val resp = premadeTask.unsafeRunSync()

  println(resp)

  /*
  implicit val cs = IO.contextShift(global)
  val riotApiClient = new RiotApiClient
  val pc = new PremadeController(riotApiClient)
  val premadeTask = pc.getPremades(Platform.NA, "RushFrog", 5)
  val resp = premadeTask

  Await.ready(resp, Duration.Inf)

  println(resp)

  val resp = for {
    fiber <- premadeTask.fork
    resp <- fiber.await
  } yield {
    resp.toEither match {
      case Right(r) => println(r)
      case Left(ex) => println("Failed!: " + ex)
    }
  }
  Thread.sleep(100000)
*/
  /*
  import net.rithms.riot.api.ApiConfig

  val config = new ApiConfig().setKey("RGAPI-ff66bd34-5511-4aa9-bf5f-65fef095230d")
    .setDebugLevel(Level.ALL)
    .setMaxAsyncThreads(10)
    .setRateLimitHandler(new AtomicRateLimitHandler(200.millis))

  val apiAsync = new RiotApi(config).getAsyncApi

  def summonerByName(name: String): Task[Summoner] =
    requestToZIOTask[Summoner](apiAsync.getSummonerByName(Platform.NA, name))

  def summonerBySummonerId(id: String): Task[Summoner] =
    requestToZIOTask[Summoner](apiAsync.getSummoner(Platform.NA, id))

  def currentGameBySummonerId(summonerId: String): Task[CurrentGameInfo] =
    requestToZIOTask[CurrentGameInfo](apiAsync.getActiveGameBySummoner(Platform.NA, summonerId))

  def matchIdsBySummonerIds(summonerIds: List[String]): Task[List[(Summoner, MatchList)]] = {
    Task.sequence {
      summonerIds.map(summonerBySummonerId).map { summoners =>
        summoners.flatMap { summoner =>
          requestToZIOTask[MatchList](apiAsync.getMatchListByAccountId(Platform.NA, summoner.getAccountId))
            .map (matchHistory => (summoner, matchHistory))
        }
      }
    }
  }

  def getMatchesByMatchList(matchList: MatchList): Task[List[Match]] = {
    Task.sequence {
      matchList.getMatches.asScala.toList.map(_.getGameId).map { id =>
        requestToZIOTask[Match](apiAsync.getMatch(Platform.NA, id))
      }
    }
  }

  def getPlayersByMatchList(matchList: List[Match]): List[(Long, List[Participant])] = {
    matchList.map { m =>
      (m.getGameId, m.getParticipants.asScala.toList)
    }
  }

  val playersFromGames = this.summonerByName("C9 Sneaky")
  Thread.sleep(1000)

  val playersFromGames2 = Task.sequence {
    Range(0, 100).map { i =>
      this.summonerByName("C9 Sneaky")
    }
  }

  /*
  val playersFromGames = this.summonerByName("C9 Sneaky")
      .flatMap(summoner => currentGameBySummonerId(summoner.getId))
      .flatMap(game => matchIdsBySummonerIds(game.getParticipants.asScala.toList.map(_.getSummonerId)))
  val playersFromGames2 = this.summonerByName("C9 Sneaky")
    .flatMap(summoner => currentGameBySummonerId(summoner.getId))
    .flatMap(game => matchIdsBySummonerIds(game.getParticipants.asScala.toList.map(_.getSummonerId)))
  val playersFromGames3 = this.summonerByName("C9 Sneaky")
    .flatMap(summoner => currentGameBySummonerId(summoner.getId))
    .flatMap(game => matchIdsBySummonerIds(game.getParticipants.asScala.toList.map(_.getSummonerId)))
  val playersFromGames4 = this.summonerByName("C9 Sneaky")
    .flatMap(summoner => currentGameBySummonerId(summoner.getId))
    .flatMap(game => matchIdsBySummonerIds(game.getParticipants.asScala.toList.map(_.getSummonerId)))
  val playersFromGames5 = this.summonerByName("C9 Sneaky")
    .flatMap(summoner => currentGameBySummonerId(summoner.getId))
    .flatMap(game => matchIdsBySummonerIds(game.getParticipants.asScala.toList.map(_.getSummonerId)))*/



  for {
    fiber <- playersFromGames.flatMap(_ => playersFromGames2).fork
    exit <- fiber.await
  } yield exit

  /*Await.ready(playersFromGames.fork, Duration.Inf)
  Await.ready(playersFromGames2, Duration.Inf)
  Await.ready(playersFromGames3, Duration.Inf)
  Await.ready(playersFromGames4, Duration.Inf)
  Await.ready(playersFromGames5, Duration.Inf)*/

  println(playersFromGames)
  */
}
