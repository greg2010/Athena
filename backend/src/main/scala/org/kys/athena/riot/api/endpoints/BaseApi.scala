package org.kys.athena.riot.api.endpoints

import org.kys.athena.riot.api.dto.common.Platform
import sttp.client._
import sttp.model.Uri


abstract class BaseApi(apiKey: String) {
  val pathPrefix: String


  def getHost(p: Platform): String = {
    s"${p.entryName.toLowerCase}.api.riotgames.com"
  }

  def getBaseUri(platform: Platform): Uri = {
    val s = s"https://${getHost(platform)}/lol/${pathPrefix}"
    uri"$s"
  }

  val baseRequest = basicRequest.header("X-Riot-Token", apiKey)
}
