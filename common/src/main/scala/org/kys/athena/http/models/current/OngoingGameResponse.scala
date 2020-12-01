package org.kys.athena.http.models.current

import java.util.UUID

import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.riot.api.dto.currentgameinfo.CurrentGameInfo
import org.kys.athena.riot.api.dto.summoner.Summoner


final case class OngoingGameResponse(gameId: Long,
                                     querySummonerId: String,
                                     querySummonerName: String,
                                     gameStartTime: Long,
                                     platformId: Platform,
                                     gameQueueId: GameQueueTypeEnum,
                                     blueTeam: OngoingGameTeam,
                                     redTeam: OngoingGameTeam,
                                     groupUuid: Option[UUID] = None)

object OngoingGameResponse {
  def apply(game: CurrentGameInfo,
            querySummoner: Summoner,
            blueTeam: OngoingGameTeam,
            redTeam: OngoingGameTeam): OngoingGameResponse = {
    OngoingGameResponse(game.gameId,
                        querySummoner.id,
                        querySummoner.name,
                        game.gameStartTime, game.platformId, game.gameQueueConfigId, blueTeam, redTeam)
  }
}