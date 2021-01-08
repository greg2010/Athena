package org.kys.athena.http.routes

import org.kys.athena.meraki.api.errors.MerakiApiError
import org.kys.athena.riot.api.errors.{NotFoundError, OtherError, ParseError, RateLimitError, RiotHttpError}
import org.kys.athena.{http, riot}
import scribe.Level
import zio.UIO


object ErrorHandler {
  def defaultErrorHandler(err: Throwable)(implicit scopeRequestId: String): UIO[http.errors.BackendApiError] = {
    UIO.effectTotal(err).tap {
      // Riot errors
      case OtherError(requestKey, responseBody, code, maybeReason) =>
        UIO.effectTotal(scribe.error(s"Got unknown error from Riot API: " +
                                     s"requestKey=$requestKey " +
                                     s"requestId=$scopeRequestId " +
                                     s"responseBody=$responseBody " +
                                     s"code=$code " +
                                     s"maybeReason=$maybeReason"))
      case RateLimitError(reqKey, respBody, rrStatus) =>
        UIO.effectTotal(scribe.error("Got ratelimit error from Riot API: " +
                                     s"requestKey=${reqKey} " +
                                     s"requestId=$scopeRequestId " +
                                     s"responseBody=${respBody} " +
                                     s"rrStatus=${rrStatus}"))
      case e: RiotHttpError =>
        val severity = e match {
          case _: NotFoundError => Level.Warn
          case _ => Level.Error
        }

        UIO.effectTotal(scribe.log[String](severity,
                                           "Got known error from Riot API: " +
                                           s"requestKey=${e.requestKey} " +
                                           s"requestId=$scopeRequestId " +
                                           s"responseBody=${e.responseBody} " +
                                           s"code=${e.code}", Some(e)))
      case ParseError(cause, description) =>
        UIO.effectTotal(scribe.error(s"Failed to parse riot API response: " +
                                     s"requestId=$scopeRequestId " +
                                     s"description=$description", cause))
      // Meraki errors
      case MerakiApiError(cause) =>
        UIO.effectTotal(scribe.error(s"Got unknown error from Meraki API: " +
                                     s"requestId=$scopeRequestId " +
                                     s"cause=$cause"))
      case e: http.errors.BackendApiError =>
        UIO.effectTotal(scribe.info(s"Got handled error: " +
                                    s"requestId=$scopeRequestId " +
                                    s"message=${e.message} " +
                                    s"code=${e.statusCode}", e))

      // Other errors
      case e: Throwable =>
        UIO.effectTotal(scribe.error(s"Caught unhandled exception: " +
                                     s"requestId=$scopeRequestId " +
                                     s"message=${e.getMessage}", e))
    }.map {
      case _: MerakiApiError => http.errors.InternalServerError("Meraki API is unavailable")
      case _: riot.api.errors.NotFoundError => http.errors.NotFoundError("Summoner not in game")
      case _: riot.api.errors.RiotApiError => http.errors.InternalServerError("Riot API is unavailable")
      case err: http.errors.BackendApiError => err
      case _ => http.errors.InternalServerError("Unknown Error")
    }
  }
}
