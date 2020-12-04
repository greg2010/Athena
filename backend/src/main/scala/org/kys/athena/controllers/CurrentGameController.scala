package org.kys.athena.controllers

import cats.effect.{ContextShift, IO}
import cats.implicits._
import org.kys.athena.http.models.current.{InGameSummoner, OngoingGameResponse, OngoingGameTeam, PositionEnum}
import org.kys.athena.meraki.api.MerakiApiClient
import org.kys.athena.riot.api.RiotApiClient
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform, SummonerSpellsEnum}
import org.kys.athena.riot.api.dto.currentgameinfo.{BannedChampion, CurrentGameInfo, CurrentGameParticipant}


class CurrentGameController(riotApiClient: RiotApiClient, merakiApiClient: MerakiApiClient)
                           (implicit cs: ContextShift[IO]) {
  case class ParticipantTuple(blue: List[CurrentGameParticipant], red: List[CurrentGameParticipant])
  case class BansTuple(blue: Option[Set[BannedChampion]], red: Option[Set[BannedChampion]])

  val blueTeamId = 100 // Hardcoded by Riot

  private def splitPlayers(cgi: CurrentGameInfo): ParticipantTuple = {
    val blue = cgi.participants.filter(_.teamId == blueTeamId)
    val red  = cgi.participants.filterNot(_.teamId == blueTeamId)
    ParticipantTuple(blue, red)
  }

  private def splitBans(currentGameInfo: CurrentGameInfo): BansTuple = {
    def mkOption[T](l: List[T]) = {
      l match {
        case Nil => None
        case l => Some(l)
      }
    }
    val blueBans = currentGameInfo.bannedChampions.filter(_.teamId == blueTeamId)

    val redBans = currentGameInfo.bannedChampions.filterNot(_.teamId == blueTeamId)
    BansTuple(mkOption(blueBans).map(_.toSet), mkOption(redBans).map(_.toSet))
  }


  def estimatePositions(gameQueueId: GameQueueTypeEnum,
                        team: Set[InGameSummoner]): IO[Option[Map[PositionEnum, String]]] = {
    // When generating possible team permutations, apply this filter
    // to force player(s) with smite to be in jungle position
    def summonersPred(arrangement: IndexedSeq[(PositionEnum, InGameSummoner)]): Boolean = {
      arrangement.foldRight(true) { (entry, soFar) =>
        soFar &&
        ((entry._1, entry._2.summonerSpells.spell1Id, entry._2.summonerSpells.spell2Id) match {
          case (p, SummonerSpellsEnum.Smite, _) if p != PositionEnum.Jungle => false
          case (p, _, SummonerSpellsEnum.Smite) if p != PositionEnum.Jungle => false
          case _ => true
        })
      }
    }

    // Reject non-summoner's rift games
    gameQueueId match {
      case q if q.in(GameQueueTypeEnum.SummonersRiftBlind,
                     GameQueueTypeEnum.SummonersRiftDraft,
                     GameQueueTypeEnum.SummonersRiftSoloRanked,
                     GameQueueTypeEnum.SummonersRiftFlexRanked,
                     GameQueueTypeEnum.SummonersRiftClash) => {
        merakiApiClient.playrates.map { playRate =>
          val posns = team
            .toList
            .permutations
            .map(PositionEnum.values.zip(_))
            .filter(summonersPred)
            .toList
            .map { perm =>
              val score = perm.map {
                case (posn, sum) =>
                  playRate.data.get(sum.championId.toInt).flatMap(_.get(posn)) match {
                    case Some(p) => p.playRate
                    case None => 0D
                  }
              }.sum
              (perm.toMap.view.mapValues(_.summonerId), score)
            }
            .maxBy(_._2)
            ._1
            .toMap
          Some(posns)
        }
      }
      case _ => IO.pure(None)
    }
  }


  def getCurrentGame(platform: Platform, name: String): IO[OngoingGameResponse] = {
    implicit val p: Platform = platform
    for {
      summoner <- riotApiClient.summonerByName(name)
      game <- riotApiClient.currentGameBySummonerId(summoner.id)
      summoners <- IO.pure(splitPlayers(game))
      bans <- IO.pure(splitBans(game))
      blueSummoners <- summoners.blue.map(riotApiClient.inGameSummonerByParticipant(_)).parSequence.map(_.toSet)
      redSummoners <- summoners.red.map(riotApiClient.inGameSummonerByParticipant(_)).parSequence.map(_.toSet)
      bluePositions <- estimatePositions(game.gameQueueConfigId, blueSummoners)
      redPositions <- estimatePositions(game.gameQueueConfigId, redSummoners)
    } yield OngoingGameResponse(game,
                                summoner,
                                OngoingGameTeam(blueSummoners, bluePositions, bans.blue),
                                OngoingGameTeam(redSummoners, redPositions, bans.red))
  }
}
