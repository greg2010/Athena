package org.kys

import org.kys.athena.util.Config
import scribe.filter.{level => flevel, _}
import scribe.{Level, Logger}
import scribe.format._
import pureconfig._
import pureconfig.generic.auto._


package object athena {


  @SuppressWarnings(Array("org.wartremover.warts.Throw")) val LAConfig: Config = ConfigSource
    .default
    .load[Config] match {
    case Right(cfg) => cfg
    case Left(ex) => throw new RuntimeException(s"Failed to parse config: ${ex.prettyPrint()}")
  }


  val myFormatter: Formatter = formatter"$level [$date][$threadName] $positionAbbreviated - $message"
  Logger.root
    .clearHandlers()
    .clearModifiers()
    .withHandler(formatter = myFormatter, minimumLevel = Some(Level.Debug))
    .withModifier(select(packageName.startsWith("org.http4s")).exclude(flevel < Level.Info))
    .replace()
}
