package org.kys.lolassistant

import java.util.logging.Level

import io.circe._
import io.circe.parser.parse
import cats.effect.IO
import cats.implicits._
import com.softwaremill.sttp.{DeserializationError, Response, SttpBackend}
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.typesafe.scalalogging.LazyLogging
import org.kys.lolassistant.api.dto.`match`.Match
import org.kys.lolassistant.api.dto.currentgameinfo.CurrentGameInfo
import org.kys.lolassistant.api.dto.summoner.Summoner
import org.kys.lolassistant.api.{Platform, RiotApi}
import org.kys.lolassistant.data.SummonerMatchHistory
import org.kys.lolassistant.api.dto.`match`.Match
import org.kys.lolassistant.api.dto.currentgameinfo.CurrentGameInfo
import org.kys.lolassistant.api.dto.summoner.Summoner
import org.kys.lolassistant.api.{Platform, RiotApi}
import org.kys.lolassistant.data.SummonerMatchHistory
import org.kys.lolassistant.util.AtomicRateLimitHandler
import org.kys.lolassistant.util.FutureConverters.requestToScalaFuture
import org.kys.lolassistant.util.exceptions.{NotFoundException, RiotException}

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

class RiotApiClient(riotApi: RiotApi)(implicit sttpBackend: SttpBackend[IO, Nothing]) extends LazyLogging {

  private def liftDoubleEither[T](r: Response[Either[DeserializationError[io.circe.Error], T]]): IO[T] = {
    r.body match {
      case Left(str) =>
        r.code match {
          case 404 => IO.raiseError(NotFoundException("Riot API responded: Not Found"))
          case code => IO.raiseError(RiotException(code, parse(str).flatMap(_.hcursor.downField("status").get[String]("message")).toOption))
        }
      case Right(Left(parseError)) => IO.raiseError(parseError.error)
      case Right(Right(resp)) => IO.pure(resp)
    }
  }

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
