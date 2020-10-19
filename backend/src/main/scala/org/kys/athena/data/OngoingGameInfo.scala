package org.kys.athena.data

import org.kys.athena.api.dto.currentgameinfo.{BannedChampion, CurrentGameInfo}
import org.kys.athena.http.models.InGameSummoner


final case class OngoingGameInfo(gameId: Long,
                                 gameStartTime: Long,
                                 platformId: String,
                                 blueTeamSummoners: Set[InGameSummoner],
                                 redTeamSummoners: Set[InGameSummoner],
                                 blueTeamBans: Set[BannedChampion],
                                 redTeamBans: Set[BannedChampion],
                                 gameQueueId: Long) {
}

object OngoingGameInfo {
  def apply(currentGameInfo: CurrentGameInfo,
            blueTeamSummoners: Set[InGameSummoner],
            redTeamSummoners: Set[InGameSummoner],
            blueTeamBans: Set[BannedChampion],
            redTeamBans: Set[BannedChampion]): OngoingGameInfo = {
    OngoingGameInfo(
      gameId = currentGameInfo.gameId,
      gameStartTime = currentGameInfo.gameStartTime,
      platformId = currentGameInfo.platformId,
      blueTeamSummoners = blueTeamSummoners,
      redTeamSummoners = redTeamSummoners,
      blueTeamBans = blueTeamBans,
      redTeamBans = redTeamBans,
      gameQueueId = currentGameInfo.gameQueueConfigId
      )
  }
}
