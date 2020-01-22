package org.kys.lolassistant.api.endpoints

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.kys.lolassistant.api.Platform
import org.kys.lolassistant.api.Platform
import org.kys.lolassistant.api.dto.currentgameinfo.CurrentGameInfo


class Spectator(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "spectator/v4"

  def activeGameBySummoner(platform: Platform,
                           summonerId: String): Request[Either[DeserializationError[io.circe.Error], CurrentGameInfo], Nothing] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("active-games", "by-summoner", summonerId))
    baseRequest.get(url).response(asJson[CurrentGameInfo])
  }
}
