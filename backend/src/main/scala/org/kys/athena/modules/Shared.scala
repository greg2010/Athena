package org.kys.athena.modules

import org.kys.athena.{http, riot}
import org.kys.athena.meraki.api.errors.MerakiApiError


object Shared {
  def defaultErrorHandler(err: Throwable): http.errors.BackendApiError = {
    err match {
      case _: MerakiApiError => http.errors.InternalServerError("Meraki API is unavailable")
      case _: riot.api.errors.NotFoundError.type => http.errors.NotFoundError("Summoner not in game")
      case err: http.errors.BackendApiError => err
      case _ => http.errors.InternalServerError("Unknown Error")
    }
  }
}
