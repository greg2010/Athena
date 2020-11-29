package org.kys.athena.riot.api

import org.kys.athena.riot.api.endpoints._


class RiotApi(apiKey: String) {
  val summoner  = new Summoner(apiKey)
  val spectator = new Spectator(apiKey)
  val `match`   = new Match(apiKey)
  val league    = new League(apiKey)
}
