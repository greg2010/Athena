package org.kys.athena.util.exceptions

import scala.util.control.NoStackTrace


final case class BadRequestException(reason: String) extends Throwable with NoStackTrace
