package org.kys.athena.riot.api.dto.common

import enumeratum.values.{IntCirceEnum, IntEnum, IntEnumEntry}


sealed abstract class GameQueueTypeEnum(override val value: Int) extends IntEnumEntry

object GameQueueTypeEnum extends IntEnum[GameQueueTypeEnum] with IntCirceEnum[GameQueueTypeEnum] {
  val values: IndexedSeq[GameQueueTypeEnum] = findValues

  // See http://static.developer.riotgames.com/docs/lol/queues.json
  case object SummonersRiftBlind extends GameQueueTypeEnum(430)
  case object SummonersRiftDraft extends GameQueueTypeEnum(400)
  case object SummonersRiftSoloRanked extends GameQueueTypeEnum(420)
  case object SummonersRiftFlexRanked extends GameQueueTypeEnum(440)
  case object SummonersRiftBotIntro extends GameQueueTypeEnum(820)
  case object SummonersRiftBotBeginner extends GameQueueTypeEnum(840)
  case object SummonersRiftBotIntermediate extends GameQueueTypeEnum(850)
  case object SummonersRiftClash extends GameQueueTypeEnum(700)
  case object HowlingAbyss extends GameQueueTypeEnum(450)
  case object Other extends GameQueueTypeEnum(-1)

  // Adds a default value (Other)
  def withValueDefault(i: Int): GameQueueTypeEnum =
    super.withValueOpt(i) match {
      case Some(v) => v
      case None => this.Other
    }
}