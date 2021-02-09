package org.kys.athena.http.backend

import org.kys.athena.http.models.current.RankedLeague
import org.kys.athena.riot.api.dto.league.RankedQueueTypeEnum


object BackendDataHelpers {
  def roundWinrate(wr: Double): Double = Math.round(wr * 1000D) / 10D

  def winrateColor(wr: Double): String = if (wr < 0.5D) "#761616" else "#094523"

  def relevantRankedLeague(rd: List[RankedLeague]): Option[RankedLeague] =
    rd.find(_.queueType == RankedQueueTypeEnum.SummonersRiftSoloRanked) match {
      case None => rd.find(_.queueType == RankedQueueTypeEnum.SummonersRiftFlexRanked)
      case o => o
    }
}
