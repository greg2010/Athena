package org.kys.athena.riot.api

import sttp.model.StatusCode

import scala.util.control.NoStackTrace


package object errors {
  sealed trait RiotApiError extends Throwable

  sealed trait RiotHttpError extends RiotApiError {
    val requestKey  : String
    val responseBody: String
    val code        : StatusCode
  }

  sealed trait Retryable

  case class BadRequestError(requestKey: String, responseBody: String) extends RiotHttpError {
    override val code: StatusCode = StatusCode.BadRequest
  }

  case class RateLimitError(requestKey: String, responseBody: String, rrStatus: ResponseLimitStatus)
    extends RiotHttpError {
    override val code: StatusCode = StatusCode.TooManyRequests
  }

  case class ServerError(requestKey: String, responseBody: String, code: StatusCode)
    extends RiotHttpError with Retryable

  case class ParseError(cause: Exception, description: String) extends RiotApiError

  case class NotFoundError(requestKey: String, responseBody: String) extends RiotHttpError {
    override val code: StatusCode = StatusCode.NotFound
  }

  case class ForbiddenError(requestKey: String, responseBody: String) extends RiotHttpError {
    override val code: StatusCode = StatusCode.Forbidden
  }

  case class OtherError(requestKey: String, responseBody: String, code: StatusCode, maybeReason: Option[String])
    extends RiotHttpError with Retryable
}
