package org.red.lolassistant

import java.util.logging.Level

import com.typesafe.scalalogging.LazyLogging
import net.rithms.riot.api.{RiotApi, RiotApiAsync}
import net.rithms.riot.api.endpoints.`match`.dto.{Match, MatchList}
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo
import net.rithms.riot.api.endpoints.summoner.dto.Summoner
import net.rithms.riot.constant.Platform
import org.red.lolassistant.util.AtomicRateLimitHandler
import org.red.lolassistant.util.FutureConverters.{requestToIOTask, requestToScalaFuture}
import cats.effect.{ContextShift, IO}
import cats.implicits._
import org.red.lolassistant.data.SummonerMatchHistory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class PremadeController(riotApiClient: RiotApiClient)(implicit ec: ExecutionContext) extends LazyLogging {
  case class GameStateHistory(friendlyTeamHistory: List[SummonerMatchHistory], enemyTeamHistory: List[SummonerMatchHistory])
  case class LobbyRecord(players: Set[String], gamesPlayed: Int)
  case class GameIdentifier(id: Long, teamId: Int)
  case class PremadeList(friendlyTeam: List[LobbyRecord], enemyTeam: List[LobbyRecord])

  // groups summoner data in the current game into friendlies and enemies
  def determineTeamState(summonerPerspectiveId: String,
                         gameInfo: CurrentGameInfo,
                         gameMatchHistory: Map[Int, List[SummonerMatchHistory]]): Either[Throwable, GameStateHistory] = {
    val opt = Option(gameInfo.getParticipantByParticipantId(summonerPerspectiveId)).map(_.getTeamId)
    Either.cond(opt.isDefined, opt.get, new Exception("No such summoner"))
      .map { teamId =>
        val friendlyTeam = gameMatchHistory.getOrElse(teamId, List())
        val enemyTeam = gameMatchHistory.filterNot(m => m._1 == teamId).values.flatten.toList
        GameStateHistory(friendlyTeamHistory = friendlyTeam, enemyTeamHistory = enemyTeam)
      }
  }

  // Converts match into a list of summoner ids on the team of summonerPerspectiveId
  def participatingFriendlySummonerIds(summonerPerspectiveId: String, m: Match): Either[Throwable, List[String]] = {
    val teamIdOpt = Option(m.getParticipantBySummonerId(summonerPerspectiveId).getTeamId)
    Either.cond(teamIdOpt.isDefined, teamIdOpt.get, new Exception("No such summoner")).map { teamId =>
      val partList = m.getParticipants.asScala.map(e => (e.getParticipantId, e)).toMap
      val partIdentList = m.getParticipantIdentities.asScala.map(e => (e.getParticipantId, e.getPlayer)).toMap
      partList
        .map(kv => (kv._1, (kv._2, partIdentList(kv._1))))
        .filter(_._2._1.getTeamId == teamId).values.map(_._2.getSummonerId)
        .toList
    }
  }

  def findPremadesInMatchHistory(team: List[SummonerMatchHistory]): List[LobbyRecord] = {
    // First, extract summoner ids
    val curTeam = team.map(_.summonerId)
    // Transform List of List of matches into List of sets of summoner ids, preserving game uniqueness
    val gameList = team.flatMap { game =>
      game.history.map { g =>
        (GameIdentifier(g.getGameId, g.getParticipantBySummonerId(game.summonerId).getTeamId),
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


  def getPremades(platform: Platform, name: String, gamesQueryCount: Int = 10): Future[PremadeList] = {
    implicit val p: Platform = platform
    for {
      summoner <- riotApiClient.summonerByName(name)
      game <- riotApiClient.currentGameBySummonerId(summoner.getId)
      participantHistory <- riotApiClient.matchHistoryByCurrentGameInfo(game, gamesQueryCount)
      state <- Future.fromTry(determineTeamState(summoner.getId, game, participantHistory).toTry)
      friendlyPremades <- Future.successful(findPremadesInMatchHistory(state.friendlyTeamHistory))
      enemyPremades <- Future.successful(findPremadesInMatchHistory(state.enemyTeamHistory))
    } yield PremadeList(friendlyPremades, enemyPremades)
  }
}
