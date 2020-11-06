package org.kys.athena.http.models

import org.kys.athena.api.dto.common.GameQueueTypeEnum
import org.kys.athena.api.dto.currentgameinfo.BannedChampion
import org.kys.athena.data.{OngoingGameInfo, PositionEnum}


final case class PremadeResponse(gameId: Long,
                                 gameStartTime: Long,
                                 platformId: String,
                                 blueTeamSummoners: Set[InGameSummoner],
                                 blueTeamPositions: Map[PositionEnum, String],
                                 redTeamSummoners: Set[InGameSummoner],
                                 redTeamPositions: Map[PositionEnum, String],
                                 blueTeamBans: Set[BannedChampion],
                                 redTeamBans: Set[BannedChampion],
                                 gameQueueId: GameQueueTypeEnum,
                                 blueTeamGroups: Set[PlayerGroup],
                                 redTeamGroups: Set[PlayerGroup])


object PremadeResponse {
  def apply(ongoingGameInfo: OngoingGameInfo,
            blueTeamGroups: Set[PlayerGroup],
            redTeamGroups: Set[PlayerGroup],
            blueTeamPositions: Map[PositionEnum, String],
            redTeamPositions: Map[PositionEnum, String]): PremadeResponse = {
    PremadeResponse(
      gameId = ongoingGameInfo.gameId,
      gameStartTime = ongoingGameInfo.gameStartTime,
      platformId = ongoingGameInfo.platformId,
      blueTeamSummoners = ongoingGameInfo.blueTeamSummoners,
      redTeamSummoners = ongoingGameInfo.redTeamSummoners,
      blueTeamBans = ongoingGameInfo.blueTeamBans,
      redTeamBans = ongoingGameInfo.redTeamBans,
      gameQueueId = ongoingGameInfo.gameQueueId,
      blueTeamGroups = blueTeamGroups,
      redTeamGroups = redTeamGroups,
      blueTeamPositions = blueTeamPositions,
      redTeamPositions = redTeamPositions)
  }
}