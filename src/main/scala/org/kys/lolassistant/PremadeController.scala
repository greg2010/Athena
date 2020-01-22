package org.kys.lolassistant

import java.util.logging.Level

import com.typesafe.scalalogging.LazyLogging
import org.kys.lolassistant.util.AtomicRateLimitHandler
import org.kys.lolassistant.util.FutureConverters.{requestToIOTask, requestToScalaFuture}
import cats.effect.{ContextShift, IO}
import cats.implicits._
import org.kys.lolassistant.api.Platform
import org.kys.lolassistant.api.dto.`match`.Match
import org.kys.lolassistant.api.dto.currentgameinfo.CurrentGameInfo
import org.kys.lolassistant.data.SummonerMatchHistory
import org.kys.lolassistant.api.Platform
import org.kys.lolassistant.api.dto.`match`.Match
import org.kys.lolassistant.api.dto.currentgameinfo.CurrentGameInfo
import org.kys.lolassistant.data.SummonerMatchHistory
import org.kys.lolassistant.util.exceptions.InconsistentAPIException

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class PremadeController(riotApiClient: RiotApiClient) extends LazyLogging {
  case class GameStateHistory(friendlyTeamHistory: List[SummonerMatchHistory], enemyTeamHistory: List[SummonerMatchHistory])
  case class LobbyRecord(players: Set[String], gamesPlayed: Int)
  case class GameIdentifier(id: Long, teamId: Int)
  case class PremadeList(friendlyTeam: List[LobbyRecord], enemyTeam: List[LobbyRecord])

  // groups summoner data in the current game into friendlies and enemies
  def determineTeamState(summonerPerspectiveId: String,
                         gameInfo: CurrentGameInfo,
                         gameMatchHistory: Map[Long, List[SummonerMatchHistory]]): Either[Throwable, GameStateHistory] = {
    val opt = gameInfo.participants.find(_.summonerId == summonerPerspectiveId).map(_.teamId)
    Either.cond(opt.isDefined, opt.get,
      InconsistentAPIException("CurrentGameInfo", "Participant exists but corresponding player doesn't")).map { teamId =>
        val friendlyTeam = gameMatchHistory.getOrElse(teamId, List())
        val enemyTeam = gameMatchHistory.filterNot(m => m._1 == teamId).values.flatten.toList
        GameStateHistory(friendlyTeamHistory = friendlyTeam, enemyTeamHistory = enemyTeam)
      }
  }

  // Converts match into a list of summoner ids on the team of summonerPerspectiveId
  def participatingFriendlySummonerIds(summonerPerspectiveId: String, m: Match): Either[Throwable, List[String]] = {
    val teamIdOpt = m.participantsFused.find(_.player.summonerId == summonerPerspectiveId).map(_.teamId)
    Either.cond(teamIdOpt.isDefined, teamIdOpt.get,
      InconsistentAPIException("Match", "Participant exists but corresponding player doesn't")).map { teamId =>
      m.participantsFused.filter(_.teamId == teamId).map(_.player.summonerId).toList
    }
  }

  def findPremadesInMatchHistory(team: List[SummonerMatchHistory]): List[LobbyRecord] = {
    // First, extract summoner ids
    val curTeam = team.map(_.summonerId)
    // Transform List of List of matches into List of sets of summoner ids, preserving game uniqueness
    val gameList = team.flatMap { game =>
      game.history.map { g =>
       g.participantsFused.find(_.player.summonerId == game.summonerId) match {
          case Some(partFused) => (GameIdentifier(g.gameId, partFused.teamId),  participatingFriendlySummonerIds(game.summonerId, g))
          case None => throw new RuntimeException("No summoner found") // TODO: wtf
        }
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


  def getPremades(platform: Platform, name: String, gamesQueryCount: Int = 10): IO[PremadeList] = {
    implicit val p: Platform = platform
    for {
      summoner <- riotApiClient.summonerByName(name)
      game <- riotApiClient.currentGameBySummonerId(summoner.id)
      participantHistory <- riotApiClient.matchHistoryByCurrentGameInfo(game, gamesQueryCount)
      state <- IO.fromTry(determineTeamState(summoner.id, game, participantHistory).toTry)
      friendlyPremades <- IO.pure(findPremadesInMatchHistory(state.friendlyTeamHistory))
      enemyPremades <- IO.pure(findPremadesInMatchHistory(state.enemyTeamHistory))
    } yield PremadeList(friendlyPremades, enemyPremades)
  }
}
