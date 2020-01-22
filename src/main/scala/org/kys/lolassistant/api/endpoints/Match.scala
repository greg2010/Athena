package org.kys.lolassistant.api.endpoints

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.kys.lolassistant.api.Platform
import org.kys.lolassistant.api.Platform
import org.kys.lolassistant.api.dto.`match`.{Match => DTOMatch}
import org.kys.lolassistant.api.dto.matchlist.Matchlist

class Match(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "match/v4"

  def matchByMatchId(platform: Platform,
                     matchId: Long): Request[Either[DeserializationError[io.circe.Error], DTOMatch], Nothing] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("matches", matchId.toString))
    baseRequest.get(url).response(asJson[DTOMatch])
  }

  def matchlistByAccountId(platform: Platform,
                           accountId: String): Request[Either[DeserializationError[io.circe.Error], Matchlist], Nothing] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("matchlists", "by-account", accountId))
    baseRequest.get(url).response(asJson[Matchlist])
  }
}
