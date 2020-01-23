package org.kys.athena.api.endpoints

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import org.kys.athena.api.Platform
import org.kys.athena.api.dto.`match`.{Match => DTOMatch}
import org.kys.athena.api.dto.matchlist.Matchlist

class Match(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "match/v4"

  def matchByMatchId(
      platform: Platform,
      matchId: Long
  ): Request[Either[DeserializationError[io.circe.Error], DTOMatch], Nothing] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("matches", matchId.toString))
    baseRequest.get(url).response(asJson[DTOMatch])
  }

  def matchlistByAccountId(
      platform: Platform,
      accountId: String
  ): Request[Either[DeserializationError[io.circe.Error], Matchlist], Nothing] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("matchlists", "by-account", accountId))
    baseRequest.get(url).response(asJson[Matchlist])
  }
}
