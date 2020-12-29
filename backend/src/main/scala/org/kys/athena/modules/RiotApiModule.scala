package org.kys.athena.modules

import io.circe.parser.parse
import org.kys.athena.data.SummonerMatchHistory
import org.kys.athena.http.models.current.InGameSummoner
import org.kys.athena.riot.api.dto.`match`.Match
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.riot.api.dto.currentgameinfo.{CurrentGameInfo, CurrentGameParticipant}
import org.kys.athena.riot.api.dto.league.League
import org.kys.athena.riot.api.dto.summoner.Summoner
import org.kys.athena.riot.api.errors._
import org.kys.athena.modules.ratelimiter.RateLimiter
import org.kys.athena.riot.api.{RequestError, RiotApi, RiotRequest, errors}
import sttp.client3.httpclient.zio.SttpClient
import sttp.client3.{DeserializationException, HttpError, Response}
import sttp.model.StatusCode
import zio.Schedule.Decision
import zio.clock.Clock
import zio.duration.Duration
import zio.macros.accessible
import zio._
import zio.duration._

import scala.reflect.ClassTag


@accessible
object RiotApiModule {
  type RiotApiClient = Has[RiotApiModule.Service]

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

  val live = {
    ZLayer.fromServices[ConfigModule.Service, SttpClient.Service, CacheModule.Service, RateLimiter,
      Clock.Service, Service] {
      (config, backend, cacheController, regionalRateLimiter, clock) =>
        new Service {
          val riotApi = new RiotApi(config.loaded.riotApiKey)

          def retried[T](rio: IO[RiotApiError, T],
                         s: Option[Schedule[Any, RiotApiError, ((Duration, Long), RiotApiError)]],
                         r: RiotRequest[T]): IO[RiotApiError, T] = {
            val defaultInitDelay    : Duration = 0.seconds
            val defaultRetryAttempts: Int      = 3

            val defaultRetryPolicy: Schedule[Any, RiotApiError, ((Duration, Long), RiotApiError)] =
              Schedule.exponential(defaultInitDelay) &&
              Schedule.recurs(defaultRetryAttempts) && Schedule.recurWhile[RiotApiError] {
                case _: errors.Retryable => true
                case _ => false
              }.onDecision {
                case Decision.Done(out: Retryable) =>
                  URIO.effectTotal(scribe.error(s"Reached retry end for " +
                                                s"reqKey=${requestKey(r)} with retryable", out))
                case Decision.Done(_) => UIO.unit
                case Decision.Continue(out, interval, next) =>
                  URIO.effectTotal(scribe.warn(s"Got retryiable error for " +
                                               s"reqKey=${requestKey(r)} " +
                                               s"nextAttemptAt=${interval.toString}", out))
              }

            val useSchedule = s.fold(defaultRetryPolicy)(identity)
            rio.retry(useSchedule).provide(Has(clock))
          }

          def requestKey[T](r: RiotRequest[T]): String = {
            (Seq(r.p.entryName, r.r.method.toString()) :++ r.method).mkString("-")
          }


          def dispatchRatelimited[T](r: RiotRequest[T]): IO[RiotApiError, T] = {
            val key = requestKey(r)
            regionalRateLimiter
              .executePlatform(key, r.p, backend.send(r.r).orDie, RiotApi.extractRR[T])
              .flatMap(liftErrors(r)(_))

          }

          // Call these to run requests with or without caching

          def dispatch[T](r: RiotRequest[T]): IO[RiotApiError, T] = {
            retried(dispatchRatelimited(r), None, r)
          }

          def dispatchCached[T](r: RiotRequest[T])(implicit ev: ClassTag[T]): IO[RiotApiError, T] = {
            val cacheKey = r.r.uri.toString()
            /*
            for {
              fromCache <- cacheController.get[T](cacheKey)
                .catchAll { err =>
                  scribe.error("Got error when pulling from cache service=riotApiClient err", err)
                  UIO.none
                }
              res <- fromCache.fold(dispatch(r))(t => UIO.succeed(t))
              _ <- fromCache.fold(cacheController.put[T](cacheKey, res))(_ => UIO.unit).catchAll { err =>
                scribe.error("Got error when pushing to cache service=riotApiClient err", err)
                UIO.none
              }
            } yield res
             */
            dispatch(r)
          }

          def liftErrors[T](req: RiotRequest[T])(r: Response[Either[RequestError, T]]): IO[RiotApiError, T] = {
            r.body match {
              case Left(HttpError(body, statusCode)) => {
                statusCode match {
                  case StatusCode.NotFound => {
                    scribe.debug(s"Riot API responded with 404")
                    IO.fail(NotFoundError)
                  }
                  case StatusCode.TooManyRequests => {
                    scribe.warn(s"Riot API responded with 429 for requestKey=${requestKey(req)} " +
                                s"rrStatus=${RiotApi.extractRR(r)}")
                    IO.fail(RateLimitError)
                  }
                  case StatusCode.BadRequest => {
                    scribe.error(s"Riot API responded with 400. Request was to ${r.request.toString()}")
                    IO.fail(BadRequestError)
                  }
                  case StatusCode.Forbidden => {
                    scribe.error(s"Riot API responded with 403 for url=${r.request.uri.toString()} body=${r.body}")
                    scribe.error("")
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
            dispatchCached(riotApi.summoner.byName(platform, name))
          }

          def summonerBySummonerId(id: String)(implicit platform: Platform): IO[RiotApiError, Summoner] = {
            scribe.debug(s"Querying summoner by id=$id platform=$platform")
            dispatchCached(riotApi.summoner.bySummonerId(platform, id))
          }

          def leaguesBySummonerId(id: String)(implicit platform: Platform): IO[RiotApiError, List[League]] = {
            scribe.debug(s"Querying leagues by id=$id platform=$platform")
            dispatchCached(riotApi.league.bySummonerId(platform, id))
          }

          def currentGameBySummonerId(summonerId: String)
                                     (implicit platform: Platform): IO[RiotApiError, CurrentGameInfo] = {
            scribe.debug(s"Querying current game by summonerId=$summonerId platform=$platform")
            dispatch(riotApi.spectator.activeGameBySummoner(platform, summonerId))
          }

          def matchByMatchId(matchId: Long)(implicit platform: Platform): IO[RiotApiError, Match] = {
            scribe.debug(s"Querying match by matchId=$matchId platform=$platform")
            dispatchCached(riotApi.`match`.matchByMatchId(platform, matchId))
          }

          def matchHistoryBySummonerId(summonerId: String, gamesQueryCount: Int, queues: Set[GameQueueTypeEnum] = Set())
                                      (implicit platform: Platform): IO[RiotApiError, List[Match]] = {
            scribe.debug(
              s"Querying match history by " + s"summonerId=$summonerId " + s"gamesQueryCount=$gamesQueryCount " +
              s"queues=${queues.mkString(",")} " + s"platform=$platform")
            summonerBySummonerId(summonerId).flatMap { summoner =>
              dispatchCached(riotApi.`match`.matchlistByAccountId(platform, summoner.accountId, queues))
                .flatMap { ml =>
                  ZIO.foreachPar(ml.matches.take(gamesQueryCount)) { reference =>
                    matchByMatchId(reference.gameId)
                  }
                }
            }
          }

          // Returns hydrated match history for each summoner (last `gamesQueryCount` games)
          def matchHistoryByInGameSummonerSet(inGameSummonerSet: Set[InGameSummoner],
                                              gamesQueryCount: Int,
                                              queues: Set[GameQueueTypeEnum] = Set())
                                             (implicit platform: Platform)
          : IO[RiotApiError, Set[SummonerMatchHistory]] = {
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
