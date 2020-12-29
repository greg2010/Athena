package org.kys.athena.riot.api.endpoints

import io.circe.Decoder
import io.circe.generic.auto._
import org.kys.athena.riot.api.RiotRequest
import org.kys.athena.riot.api.dto.`match`.{Match => DTOMatch}
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.riot.api.dto.matchlist.Matchlist
import sttp.client3.circe._


class Match(apiKey: String) extends BaseApi(apiKey) {
  override val pathPrefix: Seq[String] = Seq("match", "v4")

  def matchByMatchId(platform: Platform,
                     matchId: Long
                    ): RiotRequest[DTOMatch] = {
    implicit val gcd: Decoder[GameQueueTypeEnum] = org.kys.athena.riot.api.dto.common.GameQueueTypeEnum.circeDecoder

    val methodName = Seq("matches")
    val url        = getBaseUri(platform).addPath(methodName :+ matchId.toString)
    val req        = baseRequest.get(url).response(asJson[DTOMatch])
    RiotRequest(req, platform, pathPrefix :++ methodName)
  }

  def matchlistByAccountId(platform: Platform,
                           accountId: String,
                           queues: Set[GameQueueTypeEnum] = Set())
  : RiotRequest[Matchlist] = {
    val ps         = queues.map(id => ("queue", id.value.toString)).toList
    val methodName = Seq("matchlists", "by-account")
    val url        = getBaseUri(platform).addPath(methodName :+ accountId).addParams(ps: _*)
    val req        = baseRequest.get(url).response(asJson[Matchlist])
    RiotRequest(req, platform, pathPrefix :++ methodName)
  }
}
