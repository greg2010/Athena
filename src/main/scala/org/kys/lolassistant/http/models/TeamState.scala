package org.kys.lolassistant.http.models

case class TeamState(teamFriendly: Set[InGameSummoner], teamEnemy: Set[InGameSummoner])
