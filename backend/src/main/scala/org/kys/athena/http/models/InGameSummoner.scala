package org.kys.athena.http.models

final case class InGameSummoner(name: String,
                                summonerId: String,
                                summonerLevel: Long,
                                championId: Long,
                                runes: RunesSelected,
                                summonerSpells: SummonerSpells,
                                teamId: Long,
                                rankedLeagues: List[RankedLeague])
