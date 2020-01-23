package org.kys.athena.api.dto.summoner

import org.kys.athena.api.dto.`match`.Player


final case class Summoner(profileIconId: Int,
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
