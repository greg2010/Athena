package org.kys.athena.riot.api

package object errors {
  sealed trait RiotApiError extends Throwable

  sealed trait Retryable

  case object BadRequestError extends RiotApiError
  case object RateLimitError extends RiotApiError
  case object ServerError extends RiotApiError with Retryable
  case object ParseError extends RiotApiError
  case object NotFoundError extends RiotApiError
  case object ForbiddenError extends RiotApiError
  case class OtherError(code: Int, maybeReason: Option[String]) extends RiotApiError with Retryable
}
