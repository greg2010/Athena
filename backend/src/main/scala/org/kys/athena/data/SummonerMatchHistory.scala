package org.kys.athena.data

import org.kys.athena.api.dto.`match`.Match
import org.kys.athena.http.models.InGameSummoner


final case class SummonerMatchHistory(inGameSummoner: InGameSummoner, history: List[Match])
