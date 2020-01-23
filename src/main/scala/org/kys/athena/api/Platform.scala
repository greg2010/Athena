package org.kys.athena.api

import enumeratum._

sealed abstract class Platform(override val entryName: String) extends EnumEntry {
  def getHost: String = {
    s"${entryName.toLowerCase}.api.riotgames.com"
  }
}

object Platform extends Enum[Platform] {
  val values: IndexedSeq[Platform] = findValues

  case object NA extends Platform("NA1")
  case object BR extends Platform("BR1")
  case object EUNE extends Platform("EUN1")
  case object EUW extends Platform("EUW1")
  case object JP extends Platform("JP1")
  case object KR extends Platform("KR")
  case object LAN extends Platform("LA1")
  case object LAS extends Platform("LA2")
  case object OCE extends Platform("OC1")
  case object RU extends Platform("RU")
  case object TR extends Platform("TR1")
  // For testing purposes
  case object TEST extends Platform("TEST1")
}