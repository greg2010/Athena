package org.kys.athena.api

import org.kys.athena.api.endpoints.{Match, Spectator, Summoner}


class RiotApi(apiKey: String) {
  val summoner  = new Summoner(apiKey)
  val spectator = new Spectator(apiKey)
  val `match`   = new Match(apiKey)
}
