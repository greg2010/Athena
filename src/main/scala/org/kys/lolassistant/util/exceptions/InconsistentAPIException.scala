package org.kys.lolassistant.util.exceptions

import scala.util.control.NoStackTrace

case class InconsistentAPIException(dtoName: String, errorDesc: String) extends Throwable with NoStackTrace