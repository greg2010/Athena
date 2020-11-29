package org.kys.athena.riot.api.endpoints

import io.circe
import io.circe.generic.auto._
import org.kys.athena.riot.api.Platform
import org.kys.athena.riot.api.dto.league.{League => DTOLeague}
import sttp.client._
import sttp.client.circe._


class League(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "league/v4"

  def bySummonerId(platform: Platform,
                   summonerId: String
                  ): RequestT[Identity, Either[ResponseException[String, circe.Error], List[DTOLeague]], Any] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("entries", "by-summoner", summonerId))
    baseRequest.get(url).response(asJson[List[DTOLeague]])
  }
}
