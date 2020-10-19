package org.kys.athena.http.models

import org.kys.athena.api.dto.currentgameinfo.BannedChampion
import org.kys.athena.data.OngoingGameInfo


final case class OngoingGameInfoWithGroups(gameId: Long,
                                           gameStartTime: Long,
                                           platformId: String,
                                           blueTeamSummoners: Set[InGameSummoner],
                                           redTeamSummoners: Set[InGameSummoner],
                                           blueTeamBans: Set[BannedChampion],
                                           redTeamBans: Set[BannedChampion],
                                           gameQueueId: Long,
                                           blueTeamGroups: Set[PlayerGroup],
                                           redTeamGroups: Set[PlayerGroup])


object OngoingGameInfoWithGroups {
  def apply(ongoingGameInfo: OngoingGameInfo,
           blueTeamGroups: Set[PlayerGroup],
           redTeamGroups: Set[PlayerGroup]): OngoingGameInfoWithGroups = {
    OngoingGameInfoWithGroups(
      gameId = ongoingGameInfo.gameId,
      gameStartTime = ongoingGameInfo.gameStartTime,
      platformId = ongoingGameInfo.platformId,
      blueTeamSummoners = ongoingGameInfo.blueTeamSummoners,
      redTeamSummoners = ongoingGameInfo.redTeamSummoners,
      blueTeamBans = ongoingGameInfo.blueTeamBans,
      redTeamBans = ongoingGameInfo.redTeamBans,
      gameQueueId = ongoingGameInfo.gameQueueId,
      blueTeamGroups = blueTeamGroups,
      redTeamGroups = redTeamGroups)
  }
}