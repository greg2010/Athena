package org.kys.athena.data

import enumeratum.EnumEntry.Uppercase
import enumeratum._


sealed abstract class PositionEnum extends EnumEntry with Uppercase

object PositionEnum extends Enum[PositionEnum] with CirceEnum[PositionEnum] {
  val values: IndexedSeq[PositionEnum] = findValues

  case object Top extends PositionEnum
  case object Jungle extends PositionEnum
  case object Middle extends PositionEnum
  case object Bottom extends PositionEnum
  case object Utility extends PositionEnum
}
