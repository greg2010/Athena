package org.kys.athena

import io.circe.parser.parse
import cats.effect.IO
import cats.implicits._
import sttp.client.{DeserializationException, HttpError, Identity, RequestT, Response, ResponseException}
import com.typesafe.scalalogging.LazyLogging
import io.circe
import org.kys.athena.api.dto.currentgameinfo.CurrentGameParticipant
import org.kys.athena.api.dto.`match`.Match
import org.kys.athena.api.dto.currentgameinfo.CurrentGameInfo
import org.kys.athena.api.dto.league.League
import org.kys.athena.api.dto.summoner.Summoner
import org.kys.athena.api.{Platform, RiotApi}
import org.kys.athena.data.SummonerMatchHistory
import org.kys.athena.http.models.InGameSummoner
import org.kys.athena.util.RatelimitedSttpBackend
import org.kys.athena.http.models.extensions._
import org.kys.athena.util.exceptions.{NotFoundException, RiotException}
import sttp.model.StatusCode


class RiotApiClient(riotApi: RiotApi)(implicit ratelimitedSttpBackend: RatelimitedSttpBackend[Any])
  extends LazyLogging {

  private def liftErrors[T](r: Response[Either[ResponseException[String, circe.Error], T]]): IO[T] = {
    r.body match {
      case Left(HttpError(body, statusCode)) => {
        statusCode match {
          case StatusCode.NotFound => {
            logger.debug(s"Riot api responded with 404")
            IO.raiseError(NotFoundException("Riot API responded: Not Found"))
          }
          case code => {
            val maybeReason: Option[String] = parse(body)
              .flatMap(_.hcursor.downField("status").get[String]("message"))
              .toOption
            logger.warn(s"Got non-200/404 from Riot API: code=$code maybeReason=$maybeReason")
            IO.raiseError(RiotException(code.code, maybeReason))
          }
        }
      }
      case Left(DeserializationException(errDesc, ex)) => {
        logger.error(s"Got parse error while parsing Riot API response. error=${errDesc}", ex)
        IO.raiseError(ex)
      }
      case Right(resp) => IO.pure(resp)
    }
  }

  def summonerByName(name: String)(implicit platform: Platform): IO[Summoner] = {
    logger.debug(s"Querying summoner by name=$name platform=$platform")
    ratelimitedSttpBackend.sendCachedRateLimited(riotApi.summoner.byName(platform, name)).flatMap(liftErrors)
  }

  def summonerBySummonerId(id: String)(implicit platform: Platform): IO[Summoner] = {
    logger.debug(s"Querying summoner by id=$id platform=$platform")
    ratelimitedSttpBackend.sendCachedRateLimited(riotApi.summoner.bySummonerId(platform, id)).flatMap(liftErrors)
  }

  def leaguesBySummonerId(id: String)(implicit platform: Platform): IO[List[League]] = {
    logger.debug(s"Querying leagues by id=$id platform=$platform")
    ratelimitedSttpBackend.sendCachedRateLimited(riotApi.league.bySummonerId(platform, id)).flatMap(liftErrors)
  }

  def currentGameBySummonerId(summonerId: String)(implicit platform: Platform): IO[CurrentGameInfo] = {
    logger.debug(s"Querying current game by summonerId=$summonerId platform=$platform")
    ratelimitedSttpBackend.sendRatelimited(riotApi.spectator.activeGameBySummoner(platform, summonerId))
      .flatMap(liftErrors)
  }

  def matchByMatchId(matchId: Long)(implicit platform: Platform): IO[Match] = {
    logger.debug(s"Querying match by matchId=$matchId platform=$platform")
    ratelimitedSttpBackend.sendCachedRateLimited(riotApi.`match`.matchByMatchId(platform, matchId))
      .flatMap(liftErrors)
  }

  def matchHistoryBySummonerId(summonerId: String, gamesQueryCount: Int, queues: Set[Int] = Set())
                              (implicit platform: Platform): IO[List[Match]] = {
    logger.debug(s"Querying match history by " + s"summonerId=$summonerId " + s"gamesQueryCount=$gamesQueryCount " +
                 s"queues=${queues.mkString(",")}" + s"platform=$platform")
    summonerBySummonerId(summonerId).flatMap { summoner =>
      ratelimitedSttpBackend.sendCachedRateLimited(
        riotApi.`match`.matchlistByAccountId(platform, summoner.accountId, queues))
        .flatMap(liftErrors)
        .flatMap { ml => {
          ml.matches.take(gamesQueryCount).map { reference =>
            matchByMatchId(reference.gameId)
          }
        }.sequence
        }
    }
  }

  // Returns hydrated match history for each summoner (last `gamesQueryCount` games)
  def matchHistoryByInGameSummonerSet(inGameSummonerSet: Set[InGameSummoner],
                                      gamesQueryCount: Int, queues: Set[Int] = Set())
                                     (implicit platform: Platform): IO[Set[SummonerMatchHistory]] = {
    inGameSummonerSet.toList.map { inGameSummoner =>
      matchHistoryBySummonerId(inGameSummoner.summonerId, gamesQueryCount, queues).map { history =>
        data.SummonerMatchHistory(inGameSummoner, history)
      }
    }.sequence.map(_.toSet)
  }

  // Groups `Summoner`, `League`, and `CurrentGameParticipant`
  def inGameSummonerByParticipant(participant: CurrentGameParticipant)
                                 (implicit platform: Platform): IO[InGameSummoner] = {
    for {
      summoner <- this.summonerBySummonerId(participant.summonerId)
      leagues <- this.leaguesBySummonerId(participant.summonerId)
    } yield InGameSummoner(summoner, participant, leagues)
  }
}
