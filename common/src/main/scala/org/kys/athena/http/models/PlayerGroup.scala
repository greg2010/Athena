package org.kys.athena.http.models

final case class PlayerGroup(summoners: Set[InGameSummoner], gamesPlayed: Int)
