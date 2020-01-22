package org.kys.lolassistant.http.models

import org.kys.lolassistant.api.dto.summoner.Summoner

case class PremadeResponse(friendlyTeamSummoners: Option[Set[InGameSummoner]],
                           friendlyPlayerGroups: Option[Set[PlayerGroup]],
                           enemyTeamSummoners: Set[InGameSummoner],
                           enemyPlayerGroups: Set[PlayerGroup])