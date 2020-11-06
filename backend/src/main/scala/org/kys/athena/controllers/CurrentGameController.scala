package org.kys.athena.controllers

import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import org.kys.athena.api.{Platform, RiotApiClient}
import org.kys.athena.api.dto.currentgameinfo.{BannedChampion, CurrentGameInfo}
import org.kys.athena.data.OngoingGameInfo
import org.kys.athena.http.models.InGameSummoner


class CurrentGameController(riotApiClient: RiotApiClient) extends LazyLogging {
  case class TeamsTuple(blueTeam: Set[InGameSummoner], redTeam: Set[InGameSummoner])
  case class BansTuple(blueBans: Set[BannedChampion], redBans: Set[BannedChampion])

  val blueTeamId = 100 // Hardcoded by Riot

  def enrichGameParticipants(currentGameInfo: CurrentGameInfo)
                            (implicit platform: Platform): IO[TeamsTuple] = {

    for {
      blueTeam <- currentGameInfo
        .participants
        .filter(_.teamId == blueTeamId)
        .map(riotApiClient.inGameSummonerByParticipant(_))
        .sequence
      redTeam <- currentGameInfo
        .participants
        .filterNot(_.teamId == blueTeamId)
        .map(riotApiClient.inGameSummonerByParticipant(_))
        .sequence
    } yield TeamsTuple(blueTeam.toSet, redTeam.toSet)
  }

  def splitBans(currentGameInfo: CurrentGameInfo): IO[BansTuple] = {
    val blueBans = currentGameInfo.bannedChampions.filter(_.teamId == blueTeamId)
    val redBans = currentGameInfo.bannedChampions.filterNot(_.teamId == blueTeamId)
    IO.pure(BansTuple(blueBans = blueBans.toSet, redBans = redBans.toSet))
  }

  def getCurrentGame(platform: Platform,
                     name: String): IO[OngoingGameInfo] = {
    implicit val p: Platform = platform
    for {
      summoner <- riotApiClient.summonerByName(name)
      game <- riotApiClient.currentGameBySummonerId(summoner.id)
      teams <- enrichGameParticipants(game)
      bans <- splitBans(game)
    } yield OngoingGameInfo(game, teams.blueTeam, teams.redTeam, bans.blueBans, bans.redBans)
  }
}
