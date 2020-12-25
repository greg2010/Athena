package org.kys.athena.config

import pureconfig.ConfigSource
import zio.ZLayer
import zio.macros.accessible
import zio._
import pureconfig.generic.auto._

@accessible
object ConfigModule {
  type ConfigModule = Has[Service]

  trait Service {
    val loaded: Config
  }

  val live: ZLayer[Any, Throwable, Has[Service]] = {
    Task.effect(ConfigSource.default.loadOrThrow[Config]).map(c => new Service {
      override val loaded: Config = c
    }).toLayer
  }
}
