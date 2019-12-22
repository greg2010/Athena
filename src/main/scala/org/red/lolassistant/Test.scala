package org.red.lolassistant

import java.util.logging.Level

import net.rithms.riot.api.ApiConfig
import net.rithms.riot.api.RiotApi
import net.rithms.riot.api.endpoints.`match`.dto.{Match, MatchList, Participant}
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo
import net.rithms.riot.api.endpoints.summoner.dto.Summoner
import net.rithms.riot.api.request.ratelimit.BufferedRateLimitHandler
import net.rithms.riot.constant.Platform
import org.red.lolassistant.util.AtomicRateLimitHandler
import scala.concurrent.duration._

import scala.jdk.CollectionConverters._
import scala.concurrent.{Await, Future, Promise, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global
import org.red.lolassistant.util.FutureConverters._

import scala.concurrent.duration.Duration

object Test extends App {

  import net.rithms.riot.api.ApiConfig

  val config = new ApiConfig().setKey("RGAPI-e0d1c7ea-0d30-4cf5-854c-31a2fe69425f")
    .setDebugLevel(Level.ALL)
    .setMaxAsyncThreads(10)
    .setRateLimitHandler(new AtomicRateLimitHandler(200.millis))

  val apiAsync = new RiotApi(config).getAsyncApi

  def summonerByName(name: String): Future[Summoner] = requestToScalaFuture[Summoner](apiAsync.getSummonerByName(Platform.NA, name))

  def summonerBySummonerId(id: String): Future[Summoner] = requestToScalaFuture[Summoner](apiAsync.getSummoner(Platform.NA, id))

  def currentGameBySummonerId(summonerId: String): Future[CurrentGameInfo] = requestToScalaFuture[CurrentGameInfo](apiAsync.getActiveGameBySummoner(Platform.NA, summonerId))

  def matchIdsBySummonerIds(summonerIds: List[String]): Future[List[(Summoner, MatchList)]] = {
    Future.sequence {
      summonerIds.map(summonerBySummonerId).map { summoners =>
        summoners.flatMap { summoner =>
          requestToScalaFuture[MatchList](apiAsync.getMatchListByAccountId(Platform.NA, summoner.getAccountId))
            .map (matchHistory => (summoner, matchHistory))
        }
      }
    }
  }

  def getMatchesByMatchList(matchList: MatchList): Future[List[Match]] = {
    Future.sequence {
      matchList.getMatches.asScala.toList.map(_.getGameId).map { id =>
        requestToScalaFuture[Match](apiAsync.getMatch(Platform.NA, id))
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
  val playersFromGames2 =
    Future.sequence {
      Range(0, 100).map { i =>
        Future().flatMap { _ =>
          println(s"Executing request $i")
          this.summonerByName("C9 Sneaky")
        }
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



  Await.ready(playersFromGames, Duration.Inf)
  Await.ready(playersFromGames2, Duration.Inf)
  /*Await.ready(playersFromGames3, Duration.Inf)
  Await.ready(playersFromGames4, Duration.Inf)
  Await.ready(playersFromGames5, Duration.Inf)*/

  println(playersFromGames)

}
