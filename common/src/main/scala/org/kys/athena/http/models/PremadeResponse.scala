package org.kys.athena.http.models

final case class PremadeResponse(friendlyTeamSummoners: Option[Set[InGameSummoner]],
                                 friendlyPlayerGroups: Option[Set[PlayerGroup]],
                                 enemyTeamSummoners: Set[InGameSummoner],
                                 enemyPlayerGroups: Set[PlayerGroup])
