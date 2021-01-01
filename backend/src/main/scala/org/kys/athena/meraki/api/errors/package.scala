package org.kys.athena.meraki.api

import sttp.client3.ResponseException


package object errors {
  case class MerakiApiError(cause: ResponseException[String, io.circe.Error]) extends Throwable
}
