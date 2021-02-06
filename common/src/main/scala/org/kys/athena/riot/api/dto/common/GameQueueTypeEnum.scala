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
  case object SummonersRiftURF extends GameQueueTypeEnum(900)
  case object SummonersRiftNexusSiege extends GameQueueTypeEnum(940)
  case object SummonersRiftDoomBotsVoting extends GameQueueTypeEnum(950)
  case object SummonersRiftDoomBotsStandard extends GameQueueTypeEnum(960)
  case object SummonersRiftOneForAll extends GameQueueTypeEnum(1020)
  case object SummonersRiftNexusBlitz extends GameQueueTypeEnum(1300)
  case object HowlingAbyss extends GameQueueTypeEnum(450)
  case object HowlingAbyssPoroKing extends GameQueueTypeEnum(920)
}