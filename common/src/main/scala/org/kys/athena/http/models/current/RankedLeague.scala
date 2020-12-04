package org.kys.athena.http.models.current

import org.kys.athena.riot.api.dto.league.{League, MiniSeries, RankedQueueTypeEnum, TierEnum}


final case class RankedLeague(leagueId: String,
                              queueType: RankedQueueTypeEnum,
                              tier: TierEnum,
                              rank: String,
                              leaguePoints: Int,
                              wins: Int,
                              losses: Int,
                              miniSeries: Option[MiniSeries]) {

  lazy val winRate: Double = wins.toDouble / (wins + losses).toDouble
}

object RankedLeague {
  def apply(league: League): RankedLeague = {
    RankedLeague(
      leagueId = league.leagueId,
      queueType = league.queueType,
      tier = league.tier,
      rank = league.rank,
      leaguePoints = league.leaguePoints,
      wins = league.wins,
      losses = league.losses,
      miniSeries = league.miniSeries
    )
  }
}