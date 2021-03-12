package org.kys.athena.modules

import org.kys.athena.config.Config
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.{Has, Task, ZIO, ZLayer}


trait ConfigModule {
  val loaded: Config
}

object ConfigModule {

  val live: ZLayer[Any, Throwable, Has[ConfigModule]] = {
    Task.effect(ConfigSource.default.loadOrThrow[Config]).map(c => {
      new ConfigModule {
        override val loaded: Config = c
      }
    }).toLayer
  }

  def loaded: ZIO[Has[ConfigModule], Nothing, Config] = ZIO.access[Has[ConfigModule]](_.get.loaded)
}
