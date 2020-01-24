package org.kys.athena

import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import org.kys.athena.api.Platform
import org.kys.athena.api.dto.currentgameinfo.CurrentGameInfo
import org.kys.athena.api.dto.summoner.Summoner
import org.kys.athena.data.SummonerMatchHistory
import org.kys.athena.http.models.{InGameSummoner, PlayerGroup, PremadeResponse}
import org.kys.athena.util.exceptions.InconsistentAPIException


class PremadeController(riotApiClient: RiotApiClient) extends LazyLogging {
  case class TeamState(teamFriendly: Set[InGameSummoner], teamEnemy: Set[InGameSummoner])
  case class TeamStateWithHistory(teamFriendly: Option[Set[SummonerMatchHistory]], teamEnemy: Set[SummonerMatchHistory])

  def splitGameParticipants(currentGameInfo: CurrentGameInfo, summonerPerspective: Summoner)
                           (implicit platform: Platform): IO[TeamState] = {
    for {
      friendlyTeamId <- currentGameInfo.participants.find(_.summonerId == summonerPerspective.id) match {
        case Some(participant) => IO.pure(participant.teamId)
        case None => {
          IO.raiseError(InconsistentAPIException("CurrentGameInfo", "perspective summoner isn't in the game"))
        }
      }
      friendlyTeam <- currentGameInfo
        .participants
        .filter(_.teamId == friendlyTeamId)
        .map(riotApiClient.inGameSummonerByParticipant(_))
        .sequence
      hostileTeam <- currentGameInfo
        .participants
        .filterNot(_.teamId == friendlyTeamId)
        .map(riotApiClient.inGameSummonerByParticipant(_))
        .sequence
    } yield TeamState(friendlyTeam.toSet, hostileTeam.toSet)
  }

  def getTeamHistory(teamState: TeamState, gameQueryCount: Int, queryFriendlyGames: Boolean)
                    (implicit platform: Platform): IO[TeamStateWithHistory] = {
    for {
      friendlyGames <- if (queryFriendlyGames) {
        riotApiClient.matchHistoryByInGameSummonerSet(teamState.teamFriendly, gameQueryCount).map(Some(_))
      } else {
        IO.pure(None)
      }
      hostileGames <- riotApiClient.matchHistoryByInGameSummonerSet(teamState.teamEnemy, gameQueryCount)
    } yield TeamStateWithHistory(friendlyGames, hostileGames)
  }

  def determinePremades(teamStateWithHistory: TeamStateWithHistory): PremadeResponse = {
    def determinePremadesImpl(searchSet: Set[SummonerMatchHistory]): Set[PlayerGroup] = {
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

        acc.find(_.summoners == cursorGamePlayersFromCurrentGame) match {
          case Some(lobby) => acc.excl(lobby).incl(lobby.copy(gamesPlayed = lobby.gamesPlayed + 1))
          case None if cursorGamePlayersFromCurrentGame.size > 1 => {
            acc.incl(PlayerGroup(cursorGamePlayersFromCurrentGame, 1))
          }
          case _ => acc
        }
      })
    }

    val friendlySummoners = teamStateWithHistory.teamFriendly.map(_.map(_.inGameSummoner))
    val enemySummoners    = teamStateWithHistory.teamEnemy.map(_.inGameSummoner)
    val friendlyPremades  = teamStateWithHistory.teamFriendly.map(determinePremadesImpl)
    val enemyPremades     = determinePremadesImpl(teamStateWithHistory.teamEnemy)

    PremadeResponse(friendlySummoners, friendlyPremades, enemySummoners, enemyPremades)
  }

  def getPremades(platform: Platform,
                  name: String,
                  queryFriendlyPremades: Boolean,
                  gamesQueryCount: Int = 5): IO[PremadeResponse] = {
    implicit val p: Platform = platform
    for {
      summoner <- riotApiClient.summonerByName(name)
      game <- riotApiClient.currentGameBySummonerId(summoner.id)
      teamState <- splitGameParticipants(game, summoner)
      teamHistory <- getTeamHistory(teamState, gamesQueryCount, queryFriendlyPremades)
      premadeResponse <- IO.pure(determinePremades(teamHistory))
    } yield premadeResponse
  }
}
