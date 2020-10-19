package org.kys.athena

import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import org.kys.athena.api.Platform
import org.kys.athena.data.{OngoingGameInfo, SummonerMatchHistory}
import org.kys.athena.http.models.{PlayerGroup, OngoingGameInfoWithGroups}


class GroupController(riotApiClient: RiotApiClient)
  extends LazyLogging {
  case class TeamTupleWithHistory(blueTeam: Set[SummonerMatchHistory], redTeam: Set[SummonerMatchHistory])
  case class TeamGroupsTuple(blueTeamGroups: Set[PlayerGroup], redTeamGroups: Set[PlayerGroup])

  // See http://static.developer.riotgames.com/docs/lol/queues.json
  val queues = Set(400, 420, 430, 440, 450, 700)

  def getTeamHistory(ongoingGameInfo: OngoingGameInfo, gameQueryCount: Int)
                    (implicit platform: Platform): IO[TeamTupleWithHistory] = {
    for {
      blueGames <- riotApiClient.matchHistoryByInGameSummonerSet(ongoingGameInfo.blueTeamSummoners, gameQueryCount,
                                                                 queues)
      redGames <- riotApiClient.matchHistoryByInGameSummonerSet(ongoingGameInfo.redTeamSummoners, gameQueryCount,
                                                                queues)
    } yield TeamTupleWithHistory(blueGames, redGames)
  }

  def determineGroups(teamTupleWithHistory: TeamTupleWithHistory): TeamGroupsTuple = {
    def determineGroupsImpl(searchSet: Set[SummonerMatchHistory]): Set[PlayerGroup] = {
      val curTeam = searchSet.map(_.inGameSummoner)

      val gameParticipants = searchSet.flatMap(_.history).map(_.participantsFused)

      // Log bad API response
      gameParticipants.count(_.isEmpty) match {
        case 0 => ()
        case c => {
          logger.error("Got bad response from API: Some of the participants don't have corresponding player. " +
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
    val blueGroups = determineGroupsImpl(teamTupleWithHistory.blueTeam)
    val redGroups  = determineGroupsImpl(teamTupleWithHistory.redTeam)

    TeamGroupsTuple(blueGroups, redGroups)
  }

  def getGroupsForGame(platform: Platform,
                       ongoingGameInfo: OngoingGameInfo,
                       gamesQueryCount: Int = 5): IO[OngoingGameInfoWithGroups] = {
    implicit val p: Platform = platform
    for {
      teamHistory <- getTeamHistory(ongoingGameInfo, gamesQueryCount)
      groupsTuple <- IO.pure(determineGroups(teamHistory))
    } yield OngoingGameInfoWithGroups(ongoingGameInfo, groupsTuple.blueTeamGroups, groupsTuple.redTeamGroups)
  }
}
