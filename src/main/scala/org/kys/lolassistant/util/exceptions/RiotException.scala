package org.kys.lolassistant.util.exceptions

import scala.util.control.NoStackTrace

case class RiotException(statusCode: Int, errorMessage: Option[String]) extends Throwable with NoStackTrace
