package org.red.lolassistant

import java.util.logging.Level

import com.typesafe.scalalogging.LazyLogging
import net.rithms.riot.api.endpoints.`match`.dto.{Match, MatchList}
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo
import net.rithms.riot.api.endpoints.summoner.dto.Summoner
import net.rithms.riot.api.{RiotApi, RiotApiAsync}
import net.rithms.riot.constant.Platform
import org.red.lolassistant.data.SummonerMatchHistory
import org.red.lolassistant.util.AtomicRateLimitHandler
import org.red.lolassistant.util.FutureConverters.requestToScalaFuture

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

class RiotApiClient(implicit ec: ExecutionContext) extends LazyLogging {

  import net.rithms.riot.api.ApiConfig

  val config = new ApiConfig().setKey("RGAPI-f360e02d-916e-472b-a9a9-0279b7488b36")
    .setDebugLevel(Level.ALL)
    .setMaxAsyncThreads(5)
    .setRateLimitHandler(new AtomicRateLimitHandler(5))

  val apiAsync: RiotApiAsync = new RiotApi(config).getAsyncApi


  def summonerByName(name: String)(implicit platform: Platform): Future[Summoner] = {
    logger.debug(s"Querying summoner by name=$name platform=$platform")
    requestToScalaFuture[Summoner](apiAsync.getSummonerByName(platform, name))
  }

  def summonerBySummonerId(id: String)(implicit platform: Platform): Future[Summoner] = {
    logger.debug(s"Querying summoner by id=$id platform=$platform")
    requestToScalaFuture[Summoner](apiAsync.getSummoner(platform, id))
  }

  def currentGameBySummonerId(summonerId: String)(implicit platform: Platform): Future[CurrentGameInfo] = {
    logger.debug(s"Querying current game by summonerId=$summonerId platform=$platform")
    requestToScalaFuture[CurrentGameInfo](apiAsync.getActiveGameBySummoner(platform, summonerId))
  }

  def matchByMatchId(matchId: Long)(implicit platform: Platform): Future[Match] = {
    logger.debug(s"Querying match by matchId=$matchId platform=$platform")
    requestToScalaFuture[Match](apiAsync.getMatch(platform, matchId))
  }

  def matchHistoryBySummonerId(summonerId: String, gamesQueryCount: Int)(implicit platform: Platform): Future[List[Match]] = {
    logger.debug(s"Querying match history by summonerId=$summonerId gamesQueryCount=$gamesQueryCount platform=$platform")
    summonerBySummonerId(summonerId).flatMap { summoner =>
      requestToScalaFuture[MatchList](apiAsync.getMatchListByAccountId(Platform.NA, summoner.getAccountId))
        .flatMap { ml =>
          Future.sequence {
            ml.getMatches.asScala.toList.take(gamesQueryCount).map { reference =>
              matchByMatchId(reference.getGameId)
            }
          }
        }
    }
  }

  // Returns hydrated match history for each summoner (last `gamesQueryCount` games)
  def matchHistoryByCurrentGameInfo(game: CurrentGameInfo, gamesQueryCount: Int)
                                   (implicit platform: Platform): Future[Map[Int, List[SummonerMatchHistory]]] = {
    val gameParticipants = game.getParticipants.asScala.toList
    Future.sequence {
      gameParticipants.map(_.getTeamId).distinct.map { teamId =>
        val summonerHistory = gameParticipants.filter(_.getTeamId == teamId).map { participant =>
          matchHistoryBySummonerId(participant.getSummonerId, gamesQueryCount).map { history =>
            SummonerMatchHistory(participant.getSummonerId, history)
          }
        }
        Future.sequence(summonerHistory).map(hist => (teamId, hist))
      }
    }.map(_.toMap)
  }
}
