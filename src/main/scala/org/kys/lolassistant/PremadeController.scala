package org.kys.lolassistant

import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import org.kys.lolassistant.api.Platform
import org.kys.lolassistant.api.dto.`match`.Match
import org.kys.lolassistant.api.dto.currentgameinfo.{CurrentGameInfo, CurrentGameParticipant}
import org.kys.lolassistant.api.dto.summoner.Summoner
import org.kys.lolassistant.data.SummonerMatchHistory
import org.kys.lolassistant.http.models.{InGameSummoner, PlayerGroup, PremadeResponse, TeamState}
import org.kys.lolassistant.util.exceptions.InconsistentAPIException

class PremadeController(riotApiClient: RiotApiClient) extends LazyLogging {
  case class GameStateHistory(friendlyTeamHistory: List[SummonerMatchHistory], enemyTeamHistory: List[SummonerMatchHistory])
  case class TeamStateWithHistory(teamFriendly: Option[Set[SummonerMatchHistory]], teamEnemy: Set[SummonerMatchHistory])
  case class LobbyRecord(players: Set[InGameSummoner], gamesPlayed: Int)
  case class GameIdentifier(id: Long, teamId: Int)


  def splitGameParticipants(currentGameInfo: CurrentGameInfo, summonerPerspective: Summoner)
                           (implicit platform: Platform): IO[TeamState] = for {
    friendlyTeamId <- currentGameInfo.participants.find(_.summonerId == summonerPerspective.id) match {
      case Some(participant) => IO.pure(participant.teamId)
      case None => IO.raiseError(InconsistentAPIException("CurrentGameInfo", "perspective summoner isn't in the game"))
    }

    friendlyTeam <- currentGameInfo.participants.filter(_.teamId == friendlyTeamId)
      .map(riotApiClient.inGameSummonerByParticipant(_)).sequence

    hostileTeam <- currentGameInfo.participants.filterNot(_.teamId == friendlyTeamId)
      .map(riotApiClient.inGameSummonerByParticipant(_)).sequence

  } yield TeamState(friendlyTeam.toSet, hostileTeam.toSet)


  def getTeamHistory(teamState: TeamState, gameQueryCount: Int, queryFriendlyGames: Boolean)
                    (implicit platform: Platform): IO[TeamStateWithHistory] = for {
    friendlyGames <-
      if (queryFriendlyGames) riotApiClient.matchHistoryByInGameSummonerSet(teamState.teamFriendly, gameQueryCount).map(Some(_))
      else IO.pure(None)
    hostileGames <- riotApiClient.matchHistoryByInGameSummonerSet(teamState.teamEnemy, gameQueryCount)
  } yield TeamStateWithHistory(friendlyGames, hostileGames)


  def determinePremades(teamStateWithHistory: TeamStateWithHistory): PremadeResponse = {
    def determinePremadesImpl(searchSet: Set[SummonerMatchHistory]): Set[PlayerGroup] = {
      val curTeam = searchSet.map(_.inGameSummoner)

      def generateLobby(cursorGameParticipants: Seq[String]): Set[InGameSummoner] = {
        curTeam.filter {
          curPlayer => cursorGameParticipants.contains(curPlayer.summoner.id)
        }
      }

      // Split participants into separate teams
      val uniqueGames = searchSet.flatMap(_.history).flatMap(_.participantsFused.groupBy(_.teamId).values)

      uniqueGames.foldRight(Set[PlayerGroup]())((curGame, acc) => {
        val cursorGameParticipantSummonerIds = curGame.map(_.player.summonerId)

        val cursorGamePlayersFromCurrentGame = curTeam.filter {
          curPlayer => cursorGameParticipantSummonerIds.contains(curPlayer.summoner.id)
        }
        acc.find(_.summoners == cursorGamePlayersFromCurrentGame) match {
          case Some(lobby) => acc.excl(lobby).incl(lobby.copy(gamesPlayed = lobby.gamesPlayed + 1))
          case None if cursorGamePlayersFromCurrentGame.size > 1 => acc.incl(PlayerGroup(cursorGamePlayersFromCurrentGame, 1))
          case _ => acc
        }
      })
    }

    val friendlySummoners = teamStateWithHistory.teamFriendly.map(_.map(_.inGameSummoner))
    val enemySummoners = teamStateWithHistory.teamEnemy.map(_.inGameSummoner)
    val friendlyPremades = teamStateWithHistory.teamFriendly.map(determinePremadesImpl)
    val enemyPremades = determinePremadesImpl(teamStateWithHistory.teamEnemy)

    PremadeResponse(friendlySummoners, friendlyPremades, enemySummoners, enemyPremades)
  }


  /*
    def findPremadesInMatchHistory(team: List[SummonerMatchHistory]): List[LobbyRecord] = {
    // First, extract summoner ids
    val curTeam = team.map(_.summonerId)
    // Transform List of List of matches into List of sets of summoner ids, preserving game uniqueness
    val gameList = team.flatMap { game =>
      game.history.map { g =>
        (GameIdentifier(g.gameId, g.participantsFused.filter(_.player.summonerId == game.summonerId).getTeamId),
          participatingFriendlySummonerIds(game.summonerId, g))
      }.filter(_._2.isRight).map(i => (i._1, i._2.toOption.get))
    }.distinctBy(_._1).map(_._2)

    // Fold list of games to generate groups of existing players
    gameList.foldRight(List[LobbyRecord]())((curGame, acc) => {
      val intersectPlayers = curGame.intersect(curTeam)
      acc.find(_.players == intersectPlayers.toSet) match {
        case Some(lobby) =>
          val index = acc.indexWhere(_ == lobby)
          acc.updated(index, lobby.copy(gamesPlayed = lobby.gamesPlayed + 1))
        case None =>
          acc :+ LobbyRecord(players = intersectPlayers.toSet, gamesPlayed = 1)
      }
    })
  }
   */


  def getPremades(platform: Platform, name: String, queryFriendlyPremades: Boolean, gamesQueryCount: Int = 5): IO[PremadeResponse] = {
    implicit val p: Platform = platform
    for {
      summoner <- riotApiClient.summonerByName(name)
      game <- riotApiClient.currentGameBySummonerId(summoner.id)
      teamState <- splitGameParticipants(game, summoner)
      teamHistory <- getTeamHistory(teamState, gamesQueryCount, queryFriendlyPremades)
      premadeResponse <- IO.pure(determinePremades(teamHistory))
    } yield (premadeResponse)
  }
}
