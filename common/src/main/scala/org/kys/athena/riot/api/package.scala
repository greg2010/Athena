package org.kys.athena.riot

import io.circe
import org.kys.athena.riot.api.dto.common.Platform
import sttp.client3.{Identity, RequestT, ResponseException}

import scala.concurrent.duration.FiniteDuration


package object api {
  type RequestError = ResponseException[String, circe.Error]

  case class RateLimit(n: Int, t: FiniteDuration)
  case class RateLimitInitial(rl: RateLimit, initial: Int)
  case class ResponseLimitStatus(methodRateLimits: List[RateLimitInitial],
                                 platformRateLimits: List[RateLimitInitial])
  case class RiotRequest[T](r: RequestT[Identity, Either[RequestError, T], Any],
                            p: Platform,
                            method: Seq[String])

}
