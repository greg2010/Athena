package org.kys.athena.http.models.pregame

import org.kys.athena.http.models.common.RankedLeague


final case class PregameSummoner(name: String,
                                 summonerId: String,
                                 summonerLevel: Long,
                                 rankedLeagues: List[RankedLeague])