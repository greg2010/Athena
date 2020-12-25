package org.kys.athena.riot.api


import io.circe
import io.circe.parser.parse
import org.kys.athena.config.ConfigModule
import org.kys.athena.config.ConfigModule.ConfigModule
import org.kys.athena.data.SummonerMatchHistory
import org.kys.athena.http.models.current.InGameSummoner
import org.kys.athena.riot.api.backends.CombinedSttpBackend
import org.kys.athena.riot.api.dto.`match`.Match
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.riot.api.dto.currentgameinfo.{CurrentGameInfo, CurrentGameParticipant}
import org.kys.athena.riot.api.dto.league.League
import org.kys.athena.riot.api.dto.summoner.Summoner
import org.kys.athena.riot.api.errors._
import org.kys.athena.config.Config
import org.kys.athena.util.exceptions.{NotFoundException, RiotException}
import sttp.client3.{DeserializationException, HttpError, Response, ResponseException}
import sttp.model.StatusCode
import zio._
import zio.macros.accessible


@accessible
object RiotApiClient {
  type RiotApiClient = Has[RiotApiClient.Service]

  // All of these are exposed via ZIO's ZLayer
  trait Service {
    def summonerByName(name: String)(implicit platform: Platform): IO[RiotApiError, Summoner]

    def summonerBySummonerId(id: String)(implicit platform: Platform): IO[RiotApiError, Summoner]

    def leaguesBySummonerId(id: String)(implicit platform: Platform): IO[RiotApiError, List[League]]

    def currentGameBySummonerId(summonerId: String)(implicit platform: Platform): IO[RiotApiError, CurrentGameInfo]

    def matchByMatchId(matchId: Long)(implicit platform: Platform): IO[RiotApiError, Match]

    def matchHistoryBySummonerId(summonerId: String, gamesQueryCount: Int, queues: Set[GameQueueTypeEnum] = Set())
                                (implicit platform: Platform): IO[RiotApiError, List[Match]]

    def matchHistoryByInGameSummonerSet(inGameSummonerSet: Set[InGameSummoner],
                                        gamesQueryCount: Int, queues: Set[GameQueueTypeEnum] = Set())
                                       (implicit platform: Platform): IO[RiotApiError, Set[SummonerMatchHistory]]

    def inGameSummonerByParticipant(participant: CurrentGameParticipant)
                                   (implicit platform: Platform): IO[RiotApiError, InGameSummoner]
  }

  val live: ZLayer[Has[ConfigModule.Service] with Has[CombinedSttpBackend[Any]], Nothing, Has[Service]] = {
    ZLayer.fromServices[ConfigModule.Service, CombinedSttpBackend[Any], Service] { (config, combinedSttpBackend) =>
      new Service {
        val riotApi = new RiotApi(config.loaded.riotApiKey)

        private def liftErrors[T](r: Response[Either[ResponseException[String, circe.Error], T]]): IO[RiotApiError, T]
        = {
          r.body match {
            case Left(HttpError(body, statusCode)) => {
              statusCode match {
                case StatusCode.NotFound => {
                  scribe.debug(s"Riot API responded with 404")
                  IO.fail(NotFoundError)
                }
                case StatusCode.TooManyRequests => {
                  scribe.warn("Riot API responded with 429")
                  IO.fail(RateLimitError)
                }
                case StatusCode.BadRequest => {
                  scribe.error(s"Riot API responded with 400. Request was to ${r.request.toString()}")
                  IO.fail(BadRequestError)
                }
                case StatusCode.Forbidden => {
                  scribe.error("Riot API responded with 403: check your API key")
                  IO.fail(ForbiddenError)
                }
                case code if code.isServerError => {
                  scribe.error(s"Riot API responded with 5xx: ${code.code}")
                  IO.fail(ServerError)
                }
                case code => {
                  val maybeReason: Option[String] = parse(body)
                    .flatMap(_.hcursor.downField("status").get[String]("message"))
                    .toOption
                  scribe.warn(s"Got unknown error from Riot API: code=$code maybeReason=$maybeReason")
                  IO.fail(OtherError(code.code, maybeReason))
                }
              }
            }
            case Left(DeserializationException(errDesc, ex)) => {
              scribe.error(s"Got parse error while parsing Riot API response. error=${errDesc}", ex)
              IO.fail(ParseError)
            }
            case Right(resp) => IO.succeed(resp)
          }
        }

        def summonerByName(name: String)(implicit platform: Platform): IO[RiotApiError, Summoner] = {
          scribe.debug(s"Querying summoner by name=$name platform=$platform")
          combinedSttpBackend.sendCachedRateLimited(riotApi.summoner.byName(platform, name)).orDie.flatMap(liftErrors)
        }

        def summonerBySummonerId(id: String)(implicit platform: Platform): IO[RiotApiError, Summoner] = {
          scribe.debug(s"Querying summoner by id=$id platform=$platform")
          combinedSttpBackend.sendCachedRateLimited(riotApi.summoner.bySummonerId(platform, id))
            .orDie
            .flatMap(liftErrors)
        }

        def leaguesBySummonerId(id: String)(implicit platform: Platform): IO[RiotApiError, List[League]] = {
          scribe.debug(s"Querying leagues by id=$id platform=$platform")
          combinedSttpBackend.sendCachedRateLimited(riotApi.league.bySummonerId(platform, id))
            .orDie
            .flatMap(liftErrors)
        }

        def currentGameBySummonerId(summonerId: String)
                                   (implicit platform: Platform): IO[RiotApiError, CurrentGameInfo] = {
          scribe.debug(s"Querying current game by summonerId=$summonerId platform=$platform")
          combinedSttpBackend.sendRatelimited(riotApi.spectator.activeGameBySummoner(platform, summonerId))
            .orDie
            .flatMap(liftErrors)
        }

        def matchByMatchId(matchId: Long)(implicit platform: Platform): IO[RiotApiError, Match] = {
          scribe.debug(s"Querying match by matchId=$matchId platform=$platform")
          combinedSttpBackend.sendCachedRateLimited(riotApi.`match`.matchByMatchId(platform, matchId)).orDie
            .flatMap(liftErrors)
        }

        def matchHistoryBySummonerId(summonerId: String, gamesQueryCount: Int, queues: Set[GameQueueTypeEnum] = Set())
                                    (implicit platform: Platform): IO[RiotApiError, List[Match]] = {
          scribe.debug(
            s"Querying match history by " + s"summonerId=$summonerId " + s"gamesQueryCount=$gamesQueryCount " +
            s"queues=${queues.mkString(",")} " + s"platform=$platform")
          summonerBySummonerId(summonerId).flatMap { summoner =>
            combinedSttpBackend.sendCachedRateLimited(
              riotApi.`match`.matchlistByAccountId(platform, summoner.accountId, queues))
              .orDie
              .flatMap(liftErrors)
              .flatMap { ml =>
                ZIO.foreachPar(ml.matches.take(gamesQueryCount)) { reference =>
                  matchByMatchId(reference.gameId)
                }
              }
          }
        }

        // Returns hydrated match history for each summoner (last `gamesQueryCount` games)
        def matchHistoryByInGameSummonerSet(inGameSummonerSet: Set[InGameSummoner],
                                            gamesQueryCount: Int, queues: Set[GameQueueTypeEnum] = Set())
                                           (implicit platform: Platform): IO[RiotApiError,
          Set[SummonerMatchHistory]] = {
          ZIO.foreachPar(inGameSummonerSet.toList) { inGameSummoner =>
            matchHistoryBySummonerId(inGameSummoner.summonerId, gamesQueryCount, queues).map { history =>
              SummonerMatchHistory(inGameSummoner, history)
            }
          }.map(_.toSet)
        }

        // Groups `Summoner`, `League`, and `CurrentGameParticipant`
        def inGameSummonerByParticipant(participant: CurrentGameParticipant)
                                       (implicit platform: Platform): IO[RiotApiError, InGameSummoner] = {
          for {
            summoner <- this.summonerBySummonerId(participant.summonerId)
            leagues <- this.leaguesBySummonerId(participant.summonerId)
          } yield InGameSummoner(summoner, participant, leagues)
        }
      }
    }
  }
}
