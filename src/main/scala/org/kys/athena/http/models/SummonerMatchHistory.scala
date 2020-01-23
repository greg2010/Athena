package org.kys.athena.http.models

import org.kys.athena.api.dto.`match`.Match

case class SummonerMatchHistory(inGameSummoner: InGameSummoner, history: List[Match])
