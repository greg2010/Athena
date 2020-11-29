package org.kys.athena.util.exceptions

import scala.util.control.NoStackTrace


final case class NotFoundException(reason: String) extends Throwable with NoStackTrace
