package org.kys.lolassistant.util.exceptions

import scala.util.control.NoStackTrace

case class BadRequestException(reason: String) extends Throwable with NoStackTrace