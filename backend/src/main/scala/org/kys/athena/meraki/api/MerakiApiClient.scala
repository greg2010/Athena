package org.kys.athena.meraki.api

import cats.effect.IO
import org.kys.athena.meraki.api.dto.ChampionRates
import org.kys.athena.riot.api.backends.CombinedSttpBackend


class MerakiApiClient(merakiApi: MerakiApi)(combinedSttpBackend: CombinedSttpBackend[IO, Any])  {
  def playrates: IO[ChampionRates] = {
    combinedSttpBackend.sendCached(merakiApi.playRates).flatMap { r =>
      r.body match {
        case Right(re) => IO.pure(re)
        case Left(err) => IO.raiseError(err)
      }
    }
  }
}
