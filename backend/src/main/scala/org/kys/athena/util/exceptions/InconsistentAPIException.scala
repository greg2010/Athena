package org.kys.athena.util.exceptions

import scala.util.control.NoStackTrace

final case class InconsistentAPIException(dtoName: String, errorDesc: String) extends Throwable with NoStackTrace
