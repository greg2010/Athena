package org.red.lolassistant.data

import org.red.lolassistant.api.dto.`match`.Match


case class SummonerMatchHistory(summonerId: String, history: List[Match])