package org.kys.athena.modules

import org.kys.athena.http.models.premade.PlayerGroup
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.riot.api.errors
import zio._


trait GroupModule {
  def groupsForTeam(summoners: Set[String], gameQueryCount: Int, platform: Platform)
                   (implicit reqId: String): ZIO[Any, errors.RiotApiError, Set[PlayerGroup]]
}

object GroupModule {

  val live = ZLayer.fromService[RiotApiModule.Service, GroupModule] { riotApiClient =>
    new GroupModule {
      case class Game(summoners: Set[String]) {
        def intersect(other: Game): Set[String] = summoners.intersect(other.summoners)
      }

      // Supported match history queues
      val queues: Set[GameQueueTypeEnum] = Set(GameQueueTypeEnum.SummonersRiftBlind,
                                               GameQueueTypeEnum.SummonersRiftDraft,
                                               GameQueueTypeEnum.SummonersRiftSoloRanked,
                                               GameQueueTypeEnum.SummonersRiftFlexRanked,
                                               GameQueueTypeEnum.HowlingAbyss,
                                               GameQueueTypeEnum.SummonersRiftClash)


      def groupsFromGames(searchFor: Game, history: List[Game]): Set[PlayerGroup] = {
        // Iterate over all games in history
        history.foldRight(Set[PlayerGroup]()) { (curGame, acc) =>
          // Compare each with the current, find intersections
          val intersect = curGame.intersect(searchFor)
          // If length of intersect is > 1, increment playedWith, or create a new PlayerGroup
          acc.find(_.summoners == intersect) match {
            case Some(lobby) => acc.excl(lobby).incl(lobby.copy(gamesPlayed = lobby.gamesPlayed + 1))
            case None if intersect.size > 1 => acc.incl(PlayerGroup(intersect, 1))
            case _ => acc
          }
        }
      }

      def groupsForTeam(summoners: Set[String], gameQueryCount: Int, platform: Platform)
                       (implicit reqId: String): ZIO[Any, errors.RiotApiError, Set[PlayerGroup]] = {
        ZIO.foreachPar(summoners) { id =>
          riotApiClient.matchHistoryBySummonerId(id, gameQueryCount, queues, platform)
        }.map { resp =>
          val gameParticipants = resp.flatten.toList.map(_.participantsFused)

          // Log bad API response
          gameParticipants.count(_.isEmpty) match {
            case 0 => ()
            case c => {
              scribe.error("Got bad response from API: " +
                           "Some of the participants don't have corresponding player. " +
                           "Flattening and proceeding. Please investigate " + summoners)
            }
          }

          // Split participants into separate teams
          val games = gameParticipants.flatten
            .flatMap(_.groupBy(_.teamId).values)
            .map(pFused => Game(pFused.map(_.player.summonerId).toSet))

          groupsFromGames(Game(summoners), games)
        }
      }
    }
  }

  def groupsForTeam(summoners: Set[String], gameQueryCount: Int, platform: Platform)
                   (implicit reqId: String): ZIO[Has[GroupModule], errors.RiotApiError, Set[PlayerGroup]] = {
    ZIO.accessM[Has[GroupModule]](_.get.groupsForTeam(summoners, gameQueryCount, platform))
  }
}
