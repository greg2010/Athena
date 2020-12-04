package org.kys.athena.riot.api.endpoints

import io.circe
import io.circe.Decoder
import io.circe.generic.auto._
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.riot.api.dto.currentgameinfo.CurrentGameInfo
import sttp.client3._
import sttp.client3.circe._


class Spectator(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "spectator/v4"

  def activeGameBySummoner(platform: Platform,
                           summonerId: String)
  : RequestT[Identity, Either[ResponseException[String, circe.Error], CurrentGameInfo], Any] = {
    implicit val gcd: Decoder[GameQueueTypeEnum] = org.kys.athena.riot.api.dto.common.GameQueueTypeEnum.circeDecoder

    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("active-games", "by-summoner", summonerId))
    baseRequest.get(url).response(asJson[CurrentGameInfo])
  }
}
