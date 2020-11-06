package org.kys.athena.controllers

import org.kys.athena.api.dto.common.{GameQueueTypeEnum, SummonerSpellsEnum}
import org.kys.athena.data.{OngoingGameInfo, PositionEnum}
import org.kys.athena.http.models.InGameSummoner


class PositionHeuristicsController {
  private case class PositionProbability(position: PositionEnum, summonerId: String, probability: Double)


  def estimatePositions(ongoingGameInfo: OngoingGameInfo, team: Set[InGameSummoner]): Map[PositionEnum, String] = {
    ongoingGameInfo.gameQueueId match {
      case q if q.in(GameQueueTypeEnum.SummonersRiftBlind,
                     GameQueueTypeEnum.SummonersRiftDraft,
                     GameQueueTypeEnum.SummonersRiftSoloRanked,
                     GameQueueTypeEnum.SummonersRiftFlexRanked,
                     GameQueueTypeEnum.SummonersRiftClash) => {
        val bySummoner1 = team.groupBy(_.summonerSpells.spell1Id)
        val bySummoner2 = team.groupBy(_.summonerSpells.spell2Id)

        val bySummoners: Map[SummonerSpellsEnum, Seq[InGameSummoner]] =
          (bySummoner1.toSeq ++ bySummoner2.toSeq).groupMap(_._1)(_._2)
            .map(r => (r._1, r._2.flatten))


        val posnMap = bySummoners.flatMap {
          case (SummonerSpellsEnum.Smite, ss) =>
            ss.map(s => PositionProbability(PositionEnum.Jungle, s.summonerId, 1.0D / ss.length.toDouble))
          case (SummonerSpellsEnum.Teleport, ss) =>
            ss.map(s => PositionProbability(PositionEnum.Top, s.summonerId, 1.0D / ss.length.toDouble))
          case (SummonerSpellsEnum.Heal, ss) =>
            ss.map(s => PositionProbability(PositionEnum.ADC, s.summonerId, 1.0D / ss.length.toDouble))
          case (SummonerSpellsEnum.Exhaust, ss) =>
            ss.map(s => PositionProbability(PositionEnum.Support, s.summonerId, 1.0D / ss.length.toDouble))
          case (_, ss) => Seq()
        }.toSeq.sortBy(_.probability)(Ordering[Double].reverse)

        posnMap.foldRight(Map[PositionEnum, String]()) { (elem, soFar) =>
          elem.position match {
            case p if !soFar.keys.toSeq.contains(elem.position) =>
              soFar + ((p, elem.summonerId))
            case _ => soFar
          }
        }
      }
      case _ => Map()
    }

  }
}
