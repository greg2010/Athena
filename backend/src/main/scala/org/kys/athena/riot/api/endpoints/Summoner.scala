package org.kys.athena.riot.api.endpoints

import io.circe.generic.auto._
import org.kys.athena.riot.api.RiotRequest
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.riot.api.dto.summoner.{Summoner => DTOSummoner}
import sttp.client3.circe._


class Summoner(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: Seq[String] = Seq("summoner", "v4", "summoners")

  def byName(platform: Platform,
             name: String): RiotRequest[DTOSummoner] = {
    val methodName: Seq[String] = Seq("by-name")

    val url = getBaseUri(platform).addPath(methodName :+ name)
    val req = baseRequest.get(url).response(asJson[DTOSummoner])
    RiotRequest(req, platform, pathPrefix :++ methodName)
  }

  def bySummonerId(platform: Platform,
                   summonerId: String): RiotRequest[DTOSummoner] = {
    val url = getBaseUri(platform).addPath(Seq(summonerId))
    val req = baseRequest.get(url).response(asJson[DTOSummoner])
    RiotRequest(req, platform, pathPrefix)
  }
}
