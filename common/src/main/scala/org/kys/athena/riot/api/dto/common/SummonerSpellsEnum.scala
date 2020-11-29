package org.kys.athena.riot.api.dto.common

import enumeratum.values.{IntCirceEnum, IntEnum, IntEnumEntry}


sealed abstract class SummonerSpellsEnum(override val value: Int) extends IntEnumEntry

object SummonerSpellsEnum extends IntEnum[SummonerSpellsEnum] with IntCirceEnum[SummonerSpellsEnum] {
  val values: IndexedSeq[SummonerSpellsEnum] = findValues

  // See http://ddragon.leagueoflegends.com/cdn/10.22.1/data/en_US/summoner.json
  case object Cleanse extends SummonerSpellsEnum(1)
  case object Exhaust extends SummonerSpellsEnum(3)
  case object Flash extends SummonerSpellsEnum(4)
  case object Ghost extends SummonerSpellsEnum(6)
  case object Heal extends SummonerSpellsEnum(7)
  case object Smite extends SummonerSpellsEnum(11)
  case object Teleport extends SummonerSpellsEnum(12)
  case object Clarity extends SummonerSpellsEnum(13)
  case object Ignite extends SummonerSpellsEnum(14)
  case object Barrier extends SummonerSpellsEnum(21)
  case object PoroRecall extends SummonerSpellsEnum(30)
  case object PoroThrow extends SummonerSpellsEnum(31)
  case object Snowball extends SummonerSpellsEnum(32)
  case object SnowballUrf extends SummonerSpellsEnum(39)
}