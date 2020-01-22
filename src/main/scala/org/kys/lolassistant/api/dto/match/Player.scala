package org.kys.lolassistant.api.dto.`match`

import org.kys.lolassistant.api.dto.summoner.Summoner

case class Player(currentPlatformId: String,
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
