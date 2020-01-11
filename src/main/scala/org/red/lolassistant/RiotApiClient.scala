package org.red.lolassistant

import java.util.logging.Level

import cats.effect.IO
import cats.implicits._
import com.softwaremill.sttp.{DeserializationError, Response, SttpBackend}
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.typesafe.scalalogging.LazyLogging
import org.red.lolassistant.api.dto.`match`.Match
import org.red.lolassistant.api.dto.currentgameinfo.CurrentGameInfo
import org.red.lolassistant.api.dto.summoner.Summoner
import org.red.lolassistant.api.{Platform, RiotApi}
import org.red.lolassistant.data.SummonerMatchHistory
import org.red.lolassistant.util.AtomicRateLimitHandler
import org.red.lolassistant.util.FutureConverters.requestToScalaFuture

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

class RiotApiClient(implicit ec: ExecutionContext, sttpBackend: SttpBackend[IO, Nothing]) extends LazyLogging {

  /*
  import net.rithms.riot.api.ApiConfig

  val config = new ApiConfig().setKey("RGAPI-f360e02d-916e-472b-a9a9-0279b7488b36")
    .setDebugLevel(Level.ALL)
    .setMaxAsyncThreads(5)
    .setRateLimitHandler(new AtomicRateLimitHandler(5))

  val apiAsync: RiotApiAsync = new RiotApi(config).getAsyncApi
*/

  private def liftDoubleEither[T](r: Response[Either[DeserializationError[io.circe.Error], T]]): IO[T] = {
    r.body match {
      case Left(str) => IO.raiseError(new RuntimeException(str))
      case Right(Left(parseError)) => IO.raiseError(parseError.error)
      case Right(Right(resp)) => IO.pure(resp)
    }
  }

  val riotApi = new RiotApi("RGAPI-2d1746f0-4c91-4658-838b-b7c2bba857b0")

  def summonerByName(name: String)(implicit platform: Platform): IO[Summoner] = {
    logger.debug(s"Querying summoner by name=$name platform=$platform")
    riotApi.summoner.byName(platform, name).send().flatMap(liftDoubleEither)
  }

  def summonerBySummonerId(id: String)(implicit platform: Platform): IO[Summoner] = {
    logger.debug(s"Querying summoner by id=$id platform=$platform")
    riotApi.summoner.bySummonerId(platform, id).send().flatMap(liftDoubleEither)
  }

  def currentGameBySummonerId(summonerId: String)(implicit platform: Platform): IO[CurrentGameInfo] = {
    logger.debug(s"Querying current game by summonerId=$summonerId platform=$platform")
    riotApi.spectator.activeGameBySummoner(platform, summonerId).send().flatMap(liftDoubleEither)
  }

  def matchByMatchId(matchId: Long)(implicit platform: Platform): IO[Match] = {
    logger.debug(s"Querying match by matchId=$matchId platform=$platform")
    riotApi.`match`.matchByMatchId(platform,matchId).send().flatMap(liftDoubleEither)
  }

  def matchHistoryBySummonerId(summonerId: String, gamesQueryCount: Int)(implicit platform: Platform): IO[List[Match]] = {
    logger.debug(s"Querying match history by summonerId=$summonerId gamesQueryCount=$gamesQueryCount platform=$platform")
    summonerBySummonerId(summonerId).flatMap { summoner =>
      riotApi.`match`.matchlistByAccountId(platform, summoner.accountId).send().flatMap(liftDoubleEither)
        .flatMap { ml =>
          {
            ml.matches.take(gamesQueryCount).map { reference =>
              matchByMatchId(reference.gameId)
            }
          }.sequence
        }
    }
  }

  // Returns hydrated match history for each summoner (last `gamesQueryCount` games)
  def matchHistoryByCurrentGameInfo(game: CurrentGameInfo, gamesQueryCount: Int)
                                   (implicit platform: Platform): IO[Map[Long, List[SummonerMatchHistory]]] = {
    val gameParticipants = game.participants
    gameParticipants.map(_.teamId).distinct.map { teamId =>
      gameParticipants.filter(_.teamId == teamId).map { participant =>
        matchHistoryBySummonerId(participant.summonerId, gamesQueryCount).map { history =>
          SummonerMatchHistory(participant.summonerId, history)
        }
      }.sequence.map(hist => (teamId, hist))
    }.sequence.map(_.toMap)
  }
}
