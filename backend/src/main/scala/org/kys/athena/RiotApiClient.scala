package org.kys.athena

import io.circe.parser.parse
import cats.effect.IO
import cats.implicits._
import com.softwaremill.sttp.{DeserializationError, Response}
import com.typesafe.scalalogging.LazyLogging
import org.kys.athena.api.dto.currentgameinfo.CurrentGameParticipant
import org.kys.athena.api.dto.`match`.Match
import org.kys.athena.api.dto.currentgameinfo.CurrentGameInfo
import org.kys.athena.api.dto.summoner.Summoner
import org.kys.athena.api.{Platform, RiotApi}
import org.kys.athena.data.SummonerMatchHistory
import org.kys.athena.http.models.InGameSummoner
import org.kys.athena.util.RatelimitedSttpBackend
import org.kys.athena.http.models.extensions._
import org.kys.athena.util.exceptions.{NotFoundException, RiotException}

class RiotApiClient(riotApi: RiotApi)(implicit ratelimitedSttpBackend: RatelimitedSttpBackend[Nothing])
    extends LazyLogging {

  private def liftDoubleEither[T](r: Response[Either[DeserializationError[io.circe.Error], T]]): IO[T] = {
    r.body match {
      case Left(str) =>
        r.code match {
          case 404 =>
            logger.debug(s"Riot api responded with 404")
            IO.raiseError(NotFoundException("Riot API responded: Not Found"))
          case code =>
            val maybeReason: Option[String] =
              parse(str).flatMap(_.hcursor.downField("status").get[String]("message")).toOption
            logger.warn(s"Got non-200/404 from Riot API: code=$code maybeReason=$maybeReason")
            IO.raiseError(
              RiotException(code, parse(str).flatMap(_.hcursor.downField("status").get[String]("message")).toOption))
        }
      case Right(Left(parseError)) =>
        logger.error(s"Got parse error while parsing Riot API response. error=${parseError.message}", parseError.error)
        IO.raiseError(parseError.error)
      case Right(Right(resp)) =>
        logger.debug(s"Got a successful response from Riot API, bodyType=${resp.getClass.toString}")
        IO.pure(resp)
    }
  }

  def summonerByName(name: String)(implicit platform: Platform): IO[Summoner] = {
    logger.debug(s"Querying summoner by name=$name platform=$platform")
    ratelimitedSttpBackend.sendCachedRateLimited(riotApi.summoner.byName(platform, name)).flatMap(liftDoubleEither)
  }

  def summonerBySummonerId(id: String)(implicit platform: Platform): IO[Summoner] = {
    logger.debug(s"Querying summoner by id=$id platform=$platform")
    ratelimitedSttpBackend.sendCachedRateLimited(riotApi.summoner.bySummonerId(platform, id)).flatMap(liftDoubleEither)
  }

  def currentGameBySummonerId(summonerId: String)(implicit platform: Platform): IO[CurrentGameInfo] = {
    logger.debug(s"Querying current game by summonerId=$summonerId platform=$platform")
    ratelimitedSttpBackend
      .sendRatelimited(riotApi.spectator.activeGameBySummoner(platform, summonerId))
      .flatMap(liftDoubleEither)
  }

  def matchByMatchId(matchId: Long)(implicit platform: Platform): IO[Match] = {
    logger.debug(s"Querying match by matchId=$matchId platform=$platform")
    ratelimitedSttpBackend
      .sendCachedRateLimited(riotApi.`match`.matchByMatchId(platform, matchId))
      .flatMap(liftDoubleEither)
  }

  def matchHistoryBySummonerId(summonerId: String, gamesQueryCount: Int)(
      implicit platform: Platform): IO[List[Match]] = {
    logger.debug(
      s"Querying match history by summonerId=$summonerId gamesQueryCount=$gamesQueryCount platform=$platform")
    summonerBySummonerId(summonerId).flatMap { summoner =>
      ratelimitedSttpBackend
        .sendCachedRateLimited(riotApi.`match`.matchlistByAccountId(platform, summoner.accountId))
        .flatMap(liftDoubleEither)
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
  def matchHistoryByInGameSummonerSet(inGameSummonerSet: Set[InGameSummoner], gamesQueryCount: Int)(
      implicit platform: Platform
  ): IO[Set[SummonerMatchHistory]] = {
    inGameSummonerSet.toList
      .map { inGameSummoner =>
        matchHistoryBySummonerId(inGameSummoner.summonerId, gamesQueryCount).map { history =>
          data.SummonerMatchHistory(inGameSummoner, history)
        }
      }
      .sequence
      .map(_.toSet)
  }

  // Groups `Summoner` and `CurrentGameParticipant`
  def inGameSummonerByParticipant(participant: CurrentGameParticipant)(
      implicit platform: Platform): IO[InGameSummoner] = {
    this.summonerBySummonerId(participant.summonerId).map { summoner =>
      InGameSummoner(summoner, participant)
    }
  }
}
