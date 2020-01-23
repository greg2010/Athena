package org.kys.athena.http.models


case class PremadeResponse(friendlyTeamSummoners: Option[Set[InGameSummoner]],
                           friendlyPlayerGroups: Option[Set[PlayerGroup]],
                           enemyTeamSummoners: Set[InGameSummoner],
                           enemyPlayerGroups: Set[PlayerGroup])