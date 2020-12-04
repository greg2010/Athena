package org.kys.athena.riot.api

import cats.effect.{ContextShift, IO}
import cats.implicits._
import io.circe
import io.circe.parser.parse
import org.kys.athena.data.SummonerMatchHistory
import org.kys.athena.http.models.current.InGameSummoner
import org.kys.athena.riot.api.backends.CombinedSttpBackend
import org.kys.athena.riot.api.dto.`match`.Match
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.riot.api.dto.currentgameinfo.{CurrentGameInfo, CurrentGameParticipant}
import org.kys.athena.riot.api.dto.league.League
import org.kys.athena.riot.api.dto.summoner.Summoner
import org.kys.athena.util.exceptions.{NotFoundException, RiotException}
import sttp.client3.{DeserializationException, HttpError, Response, ResponseException}
import sttp.model.StatusCode


class RiotApiClient(riotApi: RiotApi)
                   (combinedSttpBackend: CombinedSttpBackend[IO, Any]) {

  private def liftErrors[T](r: Response[Either[ResponseException[String, circe.Error], T]]): IO[T] = {
    r.body match {
      case Left(HttpError(body, statusCode)) => {
        statusCode match {
          case StatusCode.NotFound => {
            scribe.debug(s"Riot api responded with 404")
            IO.raiseError(NotFoundException("Riot API responded: Not Found"))
          }
          case code => {
            val maybeReason: Option[String] = parse(body)
              .flatMap(_.hcursor.downField("status").get[String]("message"))
              .toOption
            scribe.warn(s"Got non-200/404 from Riot API: code=$code maybeReason=$maybeReason")
            IO.raiseError(RiotException(code.code, maybeReason))
          }
        }
      }
      case Left(DeserializationException(errDesc, ex)) => {
        scribe.error(s"Got parse error while parsing Riot API response. error=${errDesc}", ex)
        IO.raiseError(ex)
      }
      case Right(resp) => IO.pure(resp)
    }
  }

  def summonerByName(name: String)(implicit platform: Platform): IO[Summoner] = {
    scribe.debug(s"Querying summoner by name=$name platform=$platform")
    combinedSttpBackend.sendCachedRateLimited(riotApi.summoner.byName(platform, name)).flatMap(liftErrors)
  }

  def summonerBySummonerId(id: String)(implicit platform: Platform): IO[Summoner] = {
    scribe.debug(s"Querying summoner by id=$id platform=$platform")
    combinedSttpBackend.sendCachedRateLimited(riotApi.summoner.bySummonerId(platform, id)).flatMap(liftErrors)
  }

  def leaguesBySummonerId(id: String)(implicit platform: Platform): IO[List[League]] = {
    scribe.debug(s"Querying leagues by id=$id platform=$platform")
    combinedSttpBackend.sendCachedRateLimited(riotApi.league.bySummonerId(platform, id)).flatMap(liftErrors)
  }

  def currentGameBySummonerId(summonerId: String)(implicit platform: Platform): IO[CurrentGameInfo] = {
    scribe.debug(s"Querying current game by summonerId=$summonerId platform=$platform")
    combinedSttpBackend.sendRatelimited(riotApi.spectator.activeGameBySummoner(platform, summonerId))
      .flatMap(liftErrors)
  }

  def matchByMatchId(matchId: Long)(implicit platform: Platform): IO[Match] = {
    scribe.debug(s"Querying match by matchId=$matchId platform=$platform")
    combinedSttpBackend.sendCachedRateLimited(riotApi.`match`.matchByMatchId(platform, matchId))
      .flatMap(liftErrors)
  }

  def matchHistoryBySummonerId(summonerId: String, gamesQueryCount: Int, queues: Set[GameQueueTypeEnum] = Set())
                              (implicit platform: Platform, cs: ContextShift[IO]): IO[List[Match]] = {
    scribe.debug(s"Querying match history by " + s"summonerId=$summonerId " + s"gamesQueryCount=$gamesQueryCount " +
                 s"queues=${queues.mkString(",")} " + s"platform=$platform")
    summonerBySummonerId(summonerId).flatMap { summoner =>
      combinedSttpBackend.sendCachedRateLimited(
        riotApi.`match`.matchlistByAccountId(platform, summoner.accountId, queues))
        .flatMap(liftErrors)
        .flatMap { ml => {
          ml.matches.take(gamesQueryCount).map { reference =>
            matchByMatchId(reference.gameId)
          }
        }.parSequence
        }
    }
  }

  // Returns hydrated match history for each summoner (last `gamesQueryCount` games)
  def matchHistoryByInGameSummonerSet(inGameSummonerSet: Set[InGameSummoner],
                                      gamesQueryCount: Int, queues: Set[GameQueueTypeEnum] = Set())
                                     (implicit platform: Platform,
                                      cs: ContextShift[IO]): IO[Set[SummonerMatchHistory]] = {
    inGameSummonerSet.toList.map { inGameSummoner =>
      matchHistoryBySummonerId(inGameSummoner.summonerId, gamesQueryCount, queues).map { history =>
        SummonerMatchHistory(inGameSummoner, history)
      }
    }.parSequence.map(_.toSet)
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
