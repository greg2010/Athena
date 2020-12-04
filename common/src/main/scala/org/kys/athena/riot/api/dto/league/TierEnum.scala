package org.kys.athena.riot.api.dto.league

import enumeratum.EnumEntry.Uppercase
import enumeratum._

sealed abstract class TierEnum extends EnumEntry with Uppercase

object TierEnum extends Enum[TierEnum] with CirceEnum[TierEnum] {
  val values: IndexedSeq[TierEnum] = findValues

  case object Iron extends TierEnum
  case object Bronze extends TierEnum
  case object Silver extends TierEnum
  case object Gold extends TierEnum
  case object Platinum extends TierEnum
  case object Diamond extends TierEnum
  case object Master extends TierEnum
  case object Grandmaster extends TierEnum
  case object Challenger extends TierEnum
}
