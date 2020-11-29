package org.kys.athena.http.models.current

import java.util.UUID

import org.kys.athena.riot.api.Platform
import org.kys.athena.riot.api.dto.common.GameQueueTypeEnum
import org.kys.athena.riot.api.dto.currentgameinfo.CurrentGameInfo


final case class OngoingGameResponse(gameId: Long,
                                     gameStartTime: Long,
                                     platformId: Platform,
                                     gameQueueId: GameQueueTypeEnum,
                                     blueTeam: OngoingGameTeam,
                                     redTeam: OngoingGameTeam,
                                     groupUuid: Option[UUID] = None)

object OngoingGameResponse {
  def apply(game: CurrentGameInfo, blueTeam: OngoingGameTeam, redTeam: OngoingGameTeam): OngoingGameResponse = {
    OngoingGameResponse(game.gameId, game.gameStartTime, game.platformId, game.gameQueueConfigId, blueTeam, redTeam)
  }
}