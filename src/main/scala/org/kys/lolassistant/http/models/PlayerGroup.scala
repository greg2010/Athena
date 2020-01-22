package org.kys.lolassistant.http.models


case class PlayerGroup(summoners: Set[InGameSummoner], gamesPlayed: Int)
