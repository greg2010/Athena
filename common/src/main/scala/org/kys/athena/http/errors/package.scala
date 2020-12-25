package org.kys.athena.http

import sttp.model.StatusCode

package object errors {

  sealed trait BackendApiError extends Throwable {
    val statusCode: StatusCode
    val message: String
  }

  case class BadRequestError(override val message: String) extends BackendApiError {
    override val statusCode: StatusCode = StatusCode.BadRequest
  }

  case class NotFoundError(override val message: String) extends BackendApiError {
    override val statusCode: StatusCode = StatusCode.NotFound
  }

  case class InternalServerError(override val message: String) extends BackendApiError {
    override val statusCode: StatusCode = StatusCode.InternalServerError
  }
}
