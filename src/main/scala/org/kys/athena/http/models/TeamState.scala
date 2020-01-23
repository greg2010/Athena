package org.kys.athena.http.models

case class TeamState(teamFriendly: Set[InGameSummoner], teamEnemy: Set[InGameSummoner])
