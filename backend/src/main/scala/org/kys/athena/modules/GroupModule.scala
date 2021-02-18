package org.kys.athena.modules

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import org.kys.athena.data.SummonerMatchHistory
import org.kys.athena.http.models.current.OngoingGameResponse
import org.kys.athena.http.models.premade.{PlayerGroup, PremadeResponse}
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.http
import zio._

import java.util.UUID
import scala.concurrent.duration.DurationInt


trait GroupModule {
  def getGroupsForGame(platform: Platform,
                       ongoingGameInfo: OngoingGameResponse,
                       gamesQueryCount: Int = 5)
                      (implicit reqId: String): IO[Throwable, PremadeResponse]

  def getGroupsForGameAsync(platform: Platform,
                            ongoingGameInfo: OngoingGameResponse,
                            gamesQueryCount: Int = 5)
                           (implicit reqId: String): IO[Throwable, UUID]

  def getGroupsByUUID(uuid: UUID): IO[Throwable, PremadeResponse]
}

object GroupModule {

  val live = ZLayer.fromService[RiotApiModule.Service, GroupModule] { riotApiClient =>
    new GroupModule {
      case class TeamTupleWithHistory(blueTeam: Set[SummonerMatchHistory], redTeam: Set[SummonerMatchHistory])

      val uuidCache: Cache[UUID, Promise[Throwable, PremadeResponse]] = Scaffeine()
        .recordStats()
        .expireAfterWrite(5.minutes)
        .build[UUID, Promise[Throwable, PremadeResponse]]()

      val queues: Set[GameQueueTypeEnum] = Set(GameQueueTypeEnum.SummonersRiftBlind,
                                               GameQueueTypeEnum.SummonersRiftDraft,
                                               GameQueueTypeEnum.SummonersRiftSoloRanked,
                                               GameQueueTypeEnum.SummonersRiftFlexRanked,
                                               GameQueueTypeEnum.HowlingAbyss,
                                               GameQueueTypeEnum.SummonersRiftClash)

      def getTeamHistory(ongoingGameInfo: OngoingGameResponse, gameQueryCount: Int, platform: Platform)
                        (implicit reqId: String): Task[TeamTupleWithHistory] = {
        for {
          blueGames <- riotApiClient.matchHistoryByInGameSummonerSet(ongoingGameInfo.blueTeam.summoners,
                                                                     gameQueryCount,
                                                                     queues,
                                                                     platform)
          redGames <- riotApiClient.matchHistoryByInGameSummonerSet(ongoingGameInfo.redTeam.summoners,
                                                                    gameQueryCount,
                                                                    queues,
                                                                    platform)
        } yield TeamTupleWithHistory(blueGames, redGames)
      }

      def determineGroups(teamTupleWithHistory: TeamTupleWithHistory): PremadeResponse = {
        def determineGroupsImpl(searchSet: Set[SummonerMatchHistory]): Set[PlayerGroup] = {
          val curTeam = searchSet.map(_.inGameSummoner)

          val gameParticipants = searchSet.flatMap(_.history).map(_.participantsFused)

          // Log bad API response
          gameParticipants.count(_.isEmpty) match {
            case 0 => ()
            case c => {
              scribe.error("Got bad response from API: " +
                           "Some of the participants don't have corresponding player. " +
                           "Flattening and proceeding. Please investigate " + curTeam)
            }
          }

          // Split participants into separate teams
          val uniqueGames = gameParticipants.flatten
            .flatMap(_.groupBy(_.teamId).values)
            .map(_.map(_.player.summonerId))

          uniqueGames.foldRight(Set[PlayerGroup]())((curGame, acc) => {

            val cursorGamePlayersFromCurrentGame = curTeam.filter { curPlayer =>
              curGame.contains(curPlayer.summonerId)
            }

            acc.find(_.summoners == cursorGamePlayersFromCurrentGame.map(_.summonerId)) match {
              case Some(lobby) => acc.excl(lobby).incl(lobby.copy(gamesPlayed = lobby.gamesPlayed + 1))
              case None if cursorGamePlayersFromCurrentGame.size > 1 => {
                acc.incl(PlayerGroup(cursorGamePlayersFromCurrentGame.map(_.summonerId), 1))
              }
              case _ => acc
            }
          })
        }
        val blueGroups = determineGroupsImpl(teamTupleWithHistory.blueTeam)
        val redGroups  = determineGroupsImpl(teamTupleWithHistory.redTeam)

        PremadeResponse(blueGroups, redGroups)
      }

      def getGroupsForGame(platform: Platform,
                           ongoingGameInfo: OngoingGameResponse,
                           gamesQueryCount: Int = 5)(implicit reqId: String): IO[Throwable, PremadeResponse] = {
        for {
          teamHistory <- getTeamHistory(ongoingGameInfo, gamesQueryCount, platform)
          groupsTuple <- Task.succeed(determineGroups(teamHistory))
        } yield groupsTuple
      }

      def getGroupsForGameAsync(platform: Platform,
                                ongoingGameInfo: OngoingGameResponse,
                                gamesQueryCount: Int = 5)(implicit reqId: String): IO[Throwable, UUID] = {
        val promise = Promise.make[Throwable, PremadeResponse]
        for {
          uuid <- Task.effect(UUID.randomUUID())
          p <- promise
          _ <- Task.effect(uuidCache.put(uuid, p))
          _ <- p
            .complete(getGroupsForGame(platform, ongoingGameInfo, gamesQueryCount))
            .forkDaemon
        } yield uuid
      }

      def getGroupsByUUID(uuid: UUID): IO[Throwable, PremadeResponse] = {
        for {
          entry <- IO.effect(uuidCache.getIfPresent(uuid))
          res <- entry match {
            case Some(v) => {
              v.await.either.flatMap {
                case Right(re) => IO.succeed(re)
                case Left(ex) =>
                  scribe.error(s"Get groups by UUID failed uuid=$uuid", ex)
                  IO.fail(ex)
              }
            }
            case None => IO.fail(http.errors.NotFoundError("UUID not found"))
          }
        } yield res
      }
    }
  }

  def getGroupsForGame(platform: Platform,
                       ongoingGameInfo: OngoingGameResponse,
                       gamesQueryCount: Int = 5)
                      (implicit reqId: String): ZIO[Has[GroupModule], Throwable, PremadeResponse] = {
    ZIO.accessM[Has[GroupModule]](_.get.getGroupsForGame(platform, ongoingGameInfo, gamesQueryCount)(reqId))
  }

  def getGroupsForGameAsync(platform: Platform,
                            ongoingGameInfo: OngoingGameResponse,
                            gamesQueryCount: Int = 5)
                           (implicit reqId: String): ZIO[Has[GroupModule], Throwable, UUID] = {
    ZIO.accessM[Has[GroupModule]](_.get.getGroupsForGameAsync(platform, ongoingGameInfo, gamesQueryCount)(reqId))
  }

  def getGroupsByUUID(uuid: UUID): ZIO[Has[GroupModule], Throwable, PremadeResponse] = {
    ZIO.accessM[Has[GroupModule]](_.get.getGroupsByUUID(uuid))
  }
}
