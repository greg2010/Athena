package org.kys.athena.config

import org.kys.athena.config.ConfigModule.ConfigModule
import scribe.filter.{level => flevel, _}
import scribe.{Level, Logger}
import scribe.format._
import zio._

object LoggerConfig {

  def layer: ZLayer[ConfigModule, Nothing, Has[Logger]] = {
    ConfigModule.loaded.map { config =>
      val fmt: Formatter = formatter"$level [$dateFull][$threadName] $positionAbbreviated - $message"

      Logger.root
        .clearHandlers()
        .clearModifiers()
        .withHandler(formatter = fmt, minimumLevel = Level.get(config.logLevel))
        .withModifier(select(packageName.startsWith("org.http4s")).exclude(flevel < Level.Info))
        .replace()
    }
  }.toLayer.orDie
}
