package org.kys.athena.api.endpoints

import io.circe
import sttp.client._
import sttp.client.circe._
import io.circe.generic.auto._
import org.kys.athena.api.Platform
import org.kys.athena.api.dto.summoner.{Summoner => DTOSummoner}


class Summoner(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "summoner/v4/summoners"

  def byName(platform: Platform,
             name: String): RequestT[Identity, Either[ResponseError[circe.Error], DTOSummoner], Nothing] = {
    val methodName: String = "by-name"

    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq(methodName, name))
    baseRequest.get(url).response(asJson[DTOSummoner])
  }

  def bySummonerId(platform: Platform,
                   summonerId: String): RequestT[Identity, Either[ResponseError[circe.Error], DTOSummoner], Nothing] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq(summonerId))
    baseRequest.get(url).response(asJson[DTOSummoner])
  }
}
