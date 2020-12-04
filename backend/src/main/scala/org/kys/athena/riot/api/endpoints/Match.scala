package org.kys.athena.riot.api.endpoints

import io.circe
import io.circe.Decoder
import io.circe.generic.auto._
import org.kys.athena.riot.api.dto.`match`.{Match => DTOMatch}
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.riot.api.dto.matchlist.Matchlist
import sttp.client3._
import sttp.client3.circe._


class Match(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: String = "match/v4"

  def matchByMatchId(platform: Platform,
                     matchId: Long
                    ): RequestT[Identity, Either[ResponseException[String, circe.Error], DTOMatch], Any] = {
    implicit val gcd: Decoder[GameQueueTypeEnum] = org.kys.athena.riot.api.dto.common.GameQueueTypeEnum.circeDecoder

    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("matches", matchId.toString))
    baseRequest.get(url)
      .response(asJson[DTOMatch])
  }

  def matchlistByAccountId(platform: Platform,
                           accountId: String,
                           queues: Set[GameQueueTypeEnum] = Set())
  : RequestT[Identity, Either[ResponseException[String, circe.Error], Matchlist], Any] = {
    val ps  = queues.map(id => ("queue", id.value.toString)).toList
    val url = getBaseUri(platform).path(getBaseUri(platform).path ++ Seq("matchlists", "by-account", accountId))
      .params(ps: _*)
    baseRequest.get(url).response(asJson[Matchlist])
  }
}
