package org.kys.athena.controllers

import java.util.UUID

import cats.effect.{Async, Blocker, Concurrent, ContextShift, IO}
import cats.effect.concurrent.Deferred
import cats.implicits._
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import org.kys.athena.riot.api.{Platform, RiotApiClient}
import org.kys.athena.riot.api.dto.common.GameQueueTypeEnum
import org.kys.athena.data.SummonerMatchHistory
import org.kys.athena.http.models.current.OngoingGameResponse
import org.kys.athena.http.models.premade.{PlayerGroup, PremadeResponse}
import sttp.client.Response

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt


class GroupController(riotApiClient: RiotApiClient)(implicit cs: ContextShift[IO]) {
  case class TeamTupleWithHistory(blueTeam: Set[SummonerMatchHistory], redTeam: Set[SummonerMatchHistory])

  private val uuidCache: Cache[UUID, Deferred[IO, PremadeResponse]] = Scaffeine().recordStats()
    .expireAfterWrite(1.minute)
    .build[UUID, Deferred[IO, PremadeResponse]]()

  val queues: Set[GameQueueTypeEnum] = Set(GameQueueTypeEnum.SummonersRiftBlind,
                                           GameQueueTypeEnum.SummonersRiftDraft,
                                           GameQueueTypeEnum.SummonersRiftSoloRanked,
                                           GameQueueTypeEnum.SummonersRiftFlexRanked,
                                           GameQueueTypeEnum.HowlingAbyss,
                                           GameQueueTypeEnum.SummonersRiftClash)

  def getTeamHistory(ongoingGameInfo: OngoingGameResponse, gameQueryCount: Int)
                    (implicit platform: Platform): IO[TeamTupleWithHistory] = {
    for {
      blueGames <- riotApiClient.matchHistoryByInGameSummonerSet(ongoingGameInfo.blueTeam.summoners, gameQueryCount,
                                                                 queues)
      redGames <- riotApiClient.matchHistoryByInGameSummonerSet(ongoingGameInfo.redTeam.summoners, gameQueryCount,
                                                                queues)
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
          scribe.error("Got bad response from API: Some of the participants don't have corresponding player. " +
                       "Flattening and proceeding. Please investigate " + curTeam)
          ()
        }
      }

      // Split participants into separate teams
      val uniqueGames = gameParticipants.flatten.flatMap(_.groupBy(_.teamId).values)

      uniqueGames.foldRight(Set[PlayerGroup]())((curGame, acc) => {
        val cursorGameParticipantSummonerIds = curGame.map(_.player.summonerId)

        val cursorGamePlayersFromCurrentGame = curTeam.filter { curPlayer =>
          cursorGameParticipantSummonerIds.contains(curPlayer.summonerId)
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
    val blueGroups = determineGroupsImpl(teamTupleWithHistory.blueTeam) match {
      case s if s.nonEmpty => Some(s)
      case _ => None
    }
    val redGroups  = determineGroupsImpl(teamTupleWithHistory.redTeam) match {
      case s if s.nonEmpty => Some(s)
      case _ => None
    }

    PremadeResponse(blueGroups, redGroups)
  }

  def getGroupsForGame(platform: Platform,
                       ongoingGameInfo: OngoingGameResponse,
                       gamesQueryCount: Int = 5): IO[PremadeResponse] = {
    implicit val p: Platform = platform
    for {
      teamHistory <- getTeamHistory(ongoingGameInfo, gamesQueryCount)
      groupsTuple <- IO.pure(determineGroups(teamHistory))
    } yield groupsTuple

  }

  def getGroupsForGameAsync(platform: Platform,
                            ongoingGameInfo: OngoingGameResponse,
                            gamesQueryCount: Int = 5): IO[UUID] = {
    val deferred = Deferred[IO, PremadeResponse]
    for {
      uuid <- IO.delay(UUID.randomUUID())
      d <- deferred
      _ <- IO.delay(uuidCache.put(uuid, d))
      _ <- Async[IO]
        .liftIO(getGroupsForGame(platform, ongoingGameInfo, gamesQueryCount).flatMap(r => d.complete(r)))
        .start
    } yield uuid

  }

  def getGroupsByUUID(uuid: UUID): IO[Option[PremadeResponse]] = {
    for {
      entry <- IO.delay(uuidCache.getIfPresent(uuid))
      res <- entry.map(_.get).sequence
    } yield res

  }
}
