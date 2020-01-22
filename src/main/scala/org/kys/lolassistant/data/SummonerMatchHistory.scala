package org.kys.lolassistant.data

import org.kys.lolassistant.api.dto.`match`.Match
import org.kys.lolassistant.http.models.InGameSummoner


case class SummonerMatchHistory(inGameSummoner: InGameSummoner, history: List[Match])