package org.kys.lolassistant.http.models

import org.kys.lolassistant.api.dto.summoner.Summoner

case class PlayerGroup(summoners: Set[InGameSummoner], gamesPlayed: Int)
