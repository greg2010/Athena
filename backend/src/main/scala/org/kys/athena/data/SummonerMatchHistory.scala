package org.kys.athena.data

import org.kys.athena.http.models.current.InGameSummoner
import org.kys.athena.riot.api.dto.`match`.Match


final case class SummonerMatchHistory(inGameSummoner: InGameSummoner, history: List[Match])
