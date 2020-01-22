package org.kys.lolassistant.http.models

import org.kys.lolassistant.api.dto.`match`.Match

case class SummonerMatchHistory(inGameSummoner: InGameSummoner, history: List[Match])
