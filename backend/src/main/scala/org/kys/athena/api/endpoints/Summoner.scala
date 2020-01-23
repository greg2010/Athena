package org.kys.athena.api.endpoints

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import org.kys.athena.api.Platform
import org.kys.athena.api.dto.summoner.{Summoner => DTOSummoner}


class Summoner(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "summoner/v4/summoners"

  def byName(platform: Platform,
             name: String): Request[Either[DeserializationError[io.circe.Error], DTOSummoner], Nothing] = {
    val methodName: String = "by-name"

    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq(methodName, name))
    baseRequest.get(url).response(asJson[DTOSummoner])
  }

  def bySummonerId(platform: Platform,
                   summonerId: String): Request[Either[DeserializationError[io.circe.Error], DTOSummoner], Nothing] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq(summonerId))
    baseRequest.get(url).response(asJson[DTOSummoner])
  }
}
