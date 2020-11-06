package org.kys.athena.data

import enumeratum.EnumEntry.Lowercase
import enumeratum._


sealed abstract class PositionEnum extends EnumEntry with Lowercase

object PositionEnum extends Enum[PositionEnum] with CirceEnum[PositionEnum] {
  val values: IndexedSeq[PositionEnum] = findValues

  case object Top extends PositionEnum
  case object Jungle extends PositionEnum
  case object Mid extends PositionEnum
  case object ADC extends PositionEnum
  case object Support extends PositionEnum
}
