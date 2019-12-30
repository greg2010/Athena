package org.red.lolassistant.data

import net.rithms.riot.api.endpoints.`match`.dto.Match


case class SummonerMatchHistory(summonerId: String, history: List[Match])