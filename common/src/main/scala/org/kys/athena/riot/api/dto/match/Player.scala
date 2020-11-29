package org.kys.athena.riot.api.dto.`match`

import org.kys.athena.riot.api.dto.summoner.Summoner


final case class Player(currentPlatformId: String,
                        summonerName: String,
                        matchHistoryUri: String,
                        platformId: String,
                        currentAccountId: String,
                        profileIcon: Int,
                        summonerId: String,
                        accountId: String) {

  def ==(other: Player): Boolean = {
    this.summonerId == other.summonerId
  }

  def ==(other: Summoner): Boolean = {
    this.summonerId == other.id
  }
}
