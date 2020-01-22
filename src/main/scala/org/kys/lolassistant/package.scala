package org.kys

import org.kys.lolassistant.util.Config
import org.kys.lolassistant.util.Config
import pureconfig.ConfigReader.Result

package object lolassistant {
  import pureconfig._
  import pureconfig.generic.auto._
  val LAConfig: Config = ConfigSource.default.load[Config] match {
    case Right(cfg) => cfg
    case Left(ex) => throw new RuntimeException(s"Failed to parse config: ${ex.prettyPrint()}")
  }
}
