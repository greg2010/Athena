package org.kys.athena.api.endpoints

import sttp.client._
import org.kys.athena.api.Platform
import sttp.model.Uri


abstract class BaseApi(apiKey: String) {
  val pathPrefix: String

  def getBaseUri(platform: Platform): Uri = {
    val s = s"https://${platform.getHost}/lol/${pathPrefix}"
    uri"$s"
  }

  val baseRequest = basicRequest.header("X-Riot-Token", apiKey)
}
