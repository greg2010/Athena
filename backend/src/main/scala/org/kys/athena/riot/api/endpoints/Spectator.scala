package org.kys.athena.riot.api.endpoints

import io.circe.Decoder
import io.circe.generic.auto._
import org.kys.athena.riot.api.RiotRequest
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.riot.api.dto.currentgameinfo.CurrentGameInfo
import sttp.client3.circe._


class Spectator(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: Seq[String] = Seq("spectator", "v4")

  def activeGameBySummoner(platform: Platform,
                           summonerId: String): RiotRequest[CurrentGameInfo] = {
    implicit val gcd: Decoder[GameQueueTypeEnum] = org.kys.athena.riot.api.dto.common.GameQueueTypeEnum.circeDecoder

    val methodName = Seq("active-games", "by-summoner")

    val url = getBaseUri(platform).addPath(methodName :+ summonerId)
    val req = baseRequest.get(url).response(asJson[CurrentGameInfo])
    RiotRequest(req, platform, pathPrefix :++ methodName)
  }
}
