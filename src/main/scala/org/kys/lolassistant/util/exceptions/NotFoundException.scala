package org.kys.lolassistant.util.exceptions

import scala.util.control.NoStackTrace

case class NotFoundException(reason: String) extends Throwable with NoStackTrace
