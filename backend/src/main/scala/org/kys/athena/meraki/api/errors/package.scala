package org.kys.athena.meraki.api

package object errors {
  sealed trait MerakiApiError extends Throwable

  case object ServerError extends MerakiApiError
}
