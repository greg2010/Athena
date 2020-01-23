package org.kys

import org.kys.athena.util.Config
import org.kys.athena.util.Config
import pureconfig.ConfigReader.Result

package object athena {
  import pureconfig._
  import pureconfig.generic.auto._
  val LAConfig: Config = ConfigSource.default.load[Config] match {
    case Right(cfg) => cfg
    case Left(ex) => throw new RuntimeException(s"Failed to parse config: ${ex.prettyPrint()}")
  }
}
