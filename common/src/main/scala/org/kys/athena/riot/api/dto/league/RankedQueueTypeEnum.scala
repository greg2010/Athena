package org.kys.athena.riot.api.dto.league

import enumeratum.values.{StringCirceEnum, StringEnum, StringEnumEntry}


sealed abstract class RankedQueueTypeEnum(override val value: String) extends StringEnumEntry

object RankedQueueTypeEnum extends StringEnum[RankedQueueTypeEnum] with StringCirceEnum[RankedQueueTypeEnum] {
  val values: IndexedSeq[RankedQueueTypeEnum] = findValues

  // See https://developer.riotgames.com/docs/lol#general_ranked-info
  case object SummonersRiftSoloRanked extends RankedQueueTypeEnum("RANKED_SOLO_5x5")
  case object SummonersRiftFlexRanked extends RankedQueueTypeEnum("RANKED_FLEX_SR")
}