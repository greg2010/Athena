package org.kys.lolassistant.api

import org.kys.lolassistant.api.endpoints.{Match, Spectator, Summoner}

class RiotApi(apiKey: String) {
  val summoner = new Summoner(apiKey)
  val spectator = new Spectator(apiKey)
  val `match` = new Match(apiKey)
}
