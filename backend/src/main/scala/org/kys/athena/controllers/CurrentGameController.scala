package org.kys.athena.controllers

import org.kys.athena.http.models.current.{InGameSummoner, OngoingGameResponse, OngoingGameTeam, PositionEnum}
import org.kys.athena.meraki.api.MerakiApiClient
import org.kys.athena.meraki.api.MerakiApiClient.MerakiApiClient
import org.kys.athena.meraki.api.errors.MerakiApiError
import org.kys.athena.riot.api.RiotApiClient
import org.kys.athena.riot.api.RiotApiClient.RiotApiClient
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform, SummonerSpellsEnum}
import org.kys.athena.riot.api.dto.currentgameinfo.{BannedChampion, CurrentGameInfo, CurrentGameParticipant}
import org.kys.athena.riot
import org.kys.athena.http
import zio._
import zio.macros.accessible

import scala.util.Try


@accessible
object CurrentGameController {

  type CurrentGameController = Has[CurrentGameController.Service]
  trait Service {
    def getCurrentGame(platform: Platform, name: String): IO[http.errors.BackendApiError, OngoingGameResponse]
  }

  val live: ZLayer[RiotApiClient with MerakiApiClient, Nothing, CurrentGameController] =
    ZLayer.fromServices[RiotApiClient.Service, MerakiApiClient.Service, CurrentGameController.Service] {
      (riotApiClient, merakiApiClient) =>
        new Service {
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
                                team: Set[InGameSummoner]): IO[MerakiApiError, Option[Map[PositionEnum, String]]] = {
            // When generating possible team permutations, apply this filter
            // to remove permutations without at least one player with smite in jungle position
            // to force player(s) with smite to be in jungle position
            def summonersPred(arrangement: IndexedSeq[(PositionEnum, InGameSummoner)]): Boolean = {
              arrangement.foldRight(false) { (entry, soFar) =>
                soFar ||
                ((entry._1, entry._2.summonerSpells.spell1Id, entry._2.summonerSpells.spell2Id) match {
                  case (p, SummonerSpellsEnum.Smite, _) if p != PositionEnum.Jungle => true
                  case (p, _, SummonerSpellsEnum.Smite) if p != PositionEnum.Jungle => true
                  case _ => false
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
                  val posns    = team.toList.permutations
                  val zipped   = posns.map(PositionEnum.values.zip(_)).toList
                  val filtered = zipped.filterNot(summonersPred) match {
                    case e if e.isEmpty => {
                      // If heuristic removed all options (for example team without smite), ignore it
                      zipped
                    }
                    case e => e
                  }

                  val processed = filtered
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
                  // `.maxBy` can throw. Wrap in Try to catch
                  Try(processed.maxBy(_._2)._1.toMap).toOption
                }
              }
              case _ => IO.none
            }
          }


          def getCurrentGame(platform: Platform, name: String): IO[http.errors.BackendApiError, OngoingGameResponse] = {
            implicit val p: Platform = platform
            val res = for {
              summoner <- riotApiClient.summonerByName(name)
              game <- riotApiClient.currentGameBySummonerId(summoner.id)
              summoners <- Task.succeed(splitPlayers(game))
              bans <- Task.succeed(splitBans(game))
              blueSummoners <- ZIO.foreachPar(summoners.blue)(riotApiClient.inGameSummonerByParticipant(_)).map(_.toSet)
              redSummoners <- ZIO.foreachPar(summoners.red)(riotApiClient.inGameSummonerByParticipant(_)).map(_.toSet)
              bluePositions <- estimatePositions(game.gameQueueConfigId, blueSummoners)
              redPositions <- estimatePositions(game.gameQueueConfigId, redSummoners)
            } yield OngoingGameResponse(game,
                                        summoner,
                                        OngoingGameTeam(blueSummoners, bluePositions, bans.blue),
                                        OngoingGameTeam(redSummoners, redPositions, bans.red))

            res.mapError(Shared.defaultErrorHandler)
          }
        }
    }

}
