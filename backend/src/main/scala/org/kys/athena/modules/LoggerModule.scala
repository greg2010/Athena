package org.kys.athena.modules

import scribe.filter.{level => flevel, _}
import scribe.{Level, Logger}
import scribe.format._


object LoggerModule {
  val live = {
    ConfigModule.loaded.map { config =>
      val fmt: Formatter = formatter"$level [$dateFull][$threadName] $positionAbbreviated - $message"
      Logger.root
        .clearHandlers()
        .clearModifiers()
        .withHandler(formatter = fmt, minimumLevel = Level.get(config.logLevel))
        .withModifier(select(packageName.startsWith("org.http4s")).exclude(flevel < Level.Info))
        .withModifier(select(packageName.startsWith("sttp.tapir.server.http4s")).exclude(flevel < Level.Info))
        .withModifier(select(packageName.startsWith("org.http4s.blaze.channel.nio1")).exclude(flevel < Level.Warn))
        .replace()
    }
  }.toLayer
}
