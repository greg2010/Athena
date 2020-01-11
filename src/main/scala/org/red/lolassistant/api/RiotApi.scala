package org.red.lolassistant.api

import org.red.lolassistant.api.endpoints.{Match, Spectator, Summoner}

class RiotApi(apiKey: String) {
  val summoner = new Summoner(apiKey)
  val spectator = new Spectator(apiKey)
  val `match` = new Match(apiKey)
}
