package org.kys.athena.riot.api.endpoints

import io.circe
import io.circe.generic.auto._
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.riot.api.dto.summoner.{Summoner => DTOSummoner}
import sttp.client3._
import sttp.client3.circe._


class Summoner(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "summoner/v4/summoners"

  def byName(platform: Platform,
             name: String): RequestT[Identity, Either[ResponseException[String, circe.Error], DTOSummoner], Any] = {
    val methodName: String = "by-name"

    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq(methodName, name))
    baseRequest.get(url).response(asJson[DTOSummoner])
  }

  def bySummonerId(platform: Platform,
                   summonerId: String): RequestT[Identity, Either[ResponseException[String, circe.Error],
    DTOSummoner], Any] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq(summonerId))
    baseRequest.get(url).response(asJson[DTOSummoner])
  }
}
