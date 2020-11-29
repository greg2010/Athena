package org.kys.athena.http.models.current

import enumeratum.EnumEntry.Uppercase
import enumeratum._
import io.circe.{KeyDecoder, KeyEncoder}


sealed abstract class PositionEnum extends EnumEntry with Uppercase

object PositionEnum extends Enum[PositionEnum] with CirceEnum[PositionEnum] {
  val values: IndexedSeq[PositionEnum] = findValues

  case object Top extends PositionEnum
  case object Jungle extends PositionEnum
  case object Middle extends PositionEnum
  case object Bottom extends PositionEnum
  case object Utility extends PositionEnum

  // This class is used as a key in json-encoded maps, hence codecs
  implicit val PositionKeyEncoder: KeyEncoder[PositionEnum] = (position: PositionEnum) => position.entryName
  implicit val PositionKeyDecoder: KeyDecoder[PositionEnum] = (v: String) => PositionEnum.withNameOption(v)
}
