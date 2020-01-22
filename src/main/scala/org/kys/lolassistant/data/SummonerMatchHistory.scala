package org.kys.lolassistant.data

import org.kys.lolassistant.api.dto.`match`.Match


case class SummonerMatchHistory(summonerId: String, history: List[Match])