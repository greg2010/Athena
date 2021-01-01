package org.kys.athena.meraki.api

import org.kys.athena.meraki.api.dto.ChampionRates
import org.kys.athena.meraki.api.errors.MerakiApiError
import sttp.client3.httpclient.zio.SttpClient
import zio._
import zio.macros.accessible


@accessible
object MerakiApiClient {
  type MerakiApiClient = Has[MerakiApiClient.Service]

  trait Service {
    def playrates: IO[MerakiApiError, ChampionRates]
  }

  val live = ZLayer.fromService[SttpClient.Service, Service] { backend =>
    new Service {
      val merakiApi = new MerakiApi

      override def playrates: IO[MerakiApiError, ChampionRates] = {
        backend.send(merakiApi.playRates).orDie.flatMap { r =>
          r.body match {
            case Right(re) => IO.succeed(re)
            case Left(ex) => IO.fail(MerakiApiError(ex))
          }
        }
      }
    }
  }
}
