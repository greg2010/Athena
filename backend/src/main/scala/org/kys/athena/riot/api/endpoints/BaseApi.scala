package org.kys.athena.riot.api.endpoints

import org.kys.athena.riot.api.dto.common.Platform
import sttp.client3._
import sttp.model.Uri


abstract class BaseApi(apiKey: String) {
  val pathPrefix: Seq[String]

  def getHost(p: Platform): String = {
    s"${p.entryName.toLowerCase}.api.riotgames.com"
  }

  def getBaseUri(platform: Platform): Uri = uri"https://${getHost(platform)}/lol/${pathPrefix}"

  val baseRequest: RequestT[Empty, Either[String, String], Any] = basicRequest.header("X-Riot-Token", apiKey)
}
