package org.kys.athena.util

package object errors {
  trait CacheError extends Throwable

  case class CastError(message: String) extends CacheError
  case class UnknownError(err: Throwable) extends CacheError

}
