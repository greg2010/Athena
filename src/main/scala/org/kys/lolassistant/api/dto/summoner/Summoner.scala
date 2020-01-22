package org.kys.lolassistant.api.dto.summoner

import org.kys.lolassistant.api.dto.`match`.Player

case class Summoner(profileIconId: Int,
                    name: String,
                    puuid: String,
                    summonerLevel: Long,
                    revisionDate: Long,
                    id: String,
                    accountId: String) {
  def ==(other: Summoner): Boolean = {
    this.id == other.id
  }

  def ==(other: Player): Boolean = {
    this.id == other.summonerId
  }
}