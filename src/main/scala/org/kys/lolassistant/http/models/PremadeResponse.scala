package org.kys.lolassistant.http.models


case class PremadeResponse(friendlyTeamSummoners: Option[Set[InGameSummoner]],
                           friendlyPlayerGroups: Option[Set[PlayerGroup]],
                           enemyTeamSummoners: Set[InGameSummoner],
                           enemyPlayerGroups: Set[PlayerGroup])