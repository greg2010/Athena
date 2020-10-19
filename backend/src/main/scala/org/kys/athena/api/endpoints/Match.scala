package org.kys.athena.api.endpoints

import io.circe
import sttp.client._
import sttp.client.circe._
import io.circe.generic.auto._
import org.kys.athena.api.Platform
import org.kys.athena.api.dto.`match`.{Match => DTOMatch}
import org.kys.athena.api.dto.matchlist.Matchlist


class Match(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "match/v4"

  def matchByMatchId(platform: Platform,
                     matchId: Long
                    ): RequestT[Identity, Either[ResponseException[String, circe.Error], DTOMatch], Any] = {
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("matches", matchId.toString))
    baseRequest.get(url)
      .response(asJson[DTOMatch])
  }

  def matchlistByAccountId(platform: Platform,
                           accountId: String,
                           queues: Set[Int] = Set())
  : RequestT[Identity, Either[ResponseException[String, circe.Error], Matchlist], Any] = {
    val ps  = queues.map(id => ("queue", id.toString)).toList
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("matchlists", "by-account", accountId))
      .params(ps: _*)
    baseRequest.get(url).response(asJson[Matchlist])
  }
}
