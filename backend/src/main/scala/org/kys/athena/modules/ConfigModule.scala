package org.kys.athena.modules

import org.kys.athena.config.Config
import pureconfig.ConfigSource
import zio.macros.accessible
import pureconfig.generic.auto._
import zio.{Has, Task}


@accessible
object ConfigModule {
  type ConfigModule = Has[Service]

  trait Service {
    val loaded: Config
  }

  val live = {
    Task.effect(ConfigSource.default.loadOrThrow[Config]).map(c => {
      new Service {
        override val loaded: Config = c
      }
    }).toLayer
  }
}
