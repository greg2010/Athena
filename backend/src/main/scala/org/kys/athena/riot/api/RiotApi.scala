package org.kys.athena.riot.api

import org.kys.athena.riot.api.endpoints._
import sttp.client3.Response

import scala.concurrent.duration._


class RiotApi(apiKey: String) {
  val summoner  = new Summoner(apiKey)
  val spectator = new Spectator(apiKey)
  val `match`   = new Match(apiKey)
  val league    = new League(apiKey)

}

object RiotApi {
  def extractRR[T](r: Response[Either[RequestError, T]]): ResponseLimitStatus = {
    def extractRRFromHeader(headerStr: String): List[RateLimit] = {
      headerStr.split(",").map(_.split(":")).map {
        case Array(n, t) if n.forall(_.isDigit) && t.forall(_.isDigit) =>
          Some(RateLimit(n.toInt, t.toInt.seconds))
        case other =>
          scribe.warn(s"Failed to parse rate limit header tuple ${other.mkString(":")}")
          None
      }.toList.flatten
    }

    def extractWithInitial(rr: Option[String], initRr: Option[String]): List[RateLimitInitial] = {
      (rr, initRr) match {
        case (Some(rrr), Some(iRr)) => {
          extractRRFromHeader(rrr).zip(extractRRFromHeader(iRr).map(_.n))
            .map(tpl => RateLimitInitial(tpl._1, tpl._2))
        }
        case (Some(rrr), _) => extractRRFromHeader(rrr).map(rl => RateLimitInitial(rl, 0))
        case _ => List()
      }
    }

    val mRR      = r.header("X-Method-Rate-Limit")
    val mRRCount = r.header("X-Method-Rate-Limit-Count")
    val pRR      = r.header("X-App-Rate-Limit")
    val pRRCount = r.header("X-App-Rate-Limit-Count")

    ResponseLimitStatus(extractWithInitial(mRR, mRRCount), extractWithInitial(pRR, pRRCount))
  }
}