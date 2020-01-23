package org.kys.athena.api.endpoints

import com.softwaremill.sttp._
import org.kys.athena.api.Platform
import org.kys.athena.api.Platform


abstract class BaseApi(apiKey: String) {
  val pathPrefix: String

  def getBaseUri(platform: Platform): Uri = {
    val s = s"https://${platform.getHost}/lol/${pathPrefix}"
    uri"$s"
  }

  val baseRequest = emptyRequest.header("X-Riot-Token", apiKey)
}
