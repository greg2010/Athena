package org.kys.athena.meraki.api

import org.kys.athena.meraki.api.dto.ChampionRates
import org.kys.athena.meraki.api.errors.{MerakiApiError, ServerError}
import org.kys.athena.riot.api.backends.CombinedSttpBackend
import zio._
import zio.macros.accessible


@accessible
object MerakiApiClient {
  type MerakiApiClient = Has[MerakiApiClient.Service]

  trait Service {
    def playrates: IO[MerakiApiError, ChampionRates]
  }

  val live: ZLayer[Has[CombinedSttpBackend[Any]], Nothing, MerakiApiClient] = ZLayer
    .fromService[CombinedSttpBackend[Any], Service] { combinedSttpBackend =>
      new Service {
        val merakiApi = new MerakiApi

        override def playrates: IO[MerakiApiError, ChampionRates] = {
          combinedSttpBackend.sendCached(merakiApi.playRates).orDie.flatMap { r =>
            r.body match {
              case Right(re) => IO.succeed(re)
              case Left(_) => IO.fail(ServerError)
            }
          }
        }
      }
    }
}
