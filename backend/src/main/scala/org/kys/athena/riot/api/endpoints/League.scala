package org.kys.athena.riot.api.endpoints


import io.circe.generic.auto._
import org.kys.athena.riot.api.RiotRequest
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.riot.api.dto.league.{League => DTOLeague}
import sttp.client3.circe._


class League(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: Seq[String] = Seq("league", "v4")

  def bySummonerId(platform: Platform,
                   summonerId: String
                  ): RiotRequest[List[DTOLeague]] = {
    val methodName = Seq("entries", "by-summoner")
    val url        = getBaseUri(platform).addPath(methodName :+ summonerId)
    val req        = baseRequest.get(url).response(asJson[List[DTOLeague]])
    RiotRequest(req, platform, pathPrefix :++ methodName)
  }
}
