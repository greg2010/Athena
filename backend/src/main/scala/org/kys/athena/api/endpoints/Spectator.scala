package org.kys.athena.api.endpoints

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import org.kys.athena.api.Platform
import org.kys.athena.api.dto.currentgameinfo.CurrentGameInfo


class Spectator(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "spectator/v4"

  def activeGameBySummoner(platform: Platform,
                           summonerId: String): Request[Either[DeserializationError[io.circe.Error], CurrentGameInfo], Nothing] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("active-games", "by-summoner", summonerId))
    baseRequest.get(url).response(asJson[CurrentGameInfo])
  }
}
