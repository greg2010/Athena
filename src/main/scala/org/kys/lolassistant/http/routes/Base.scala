package org.kys.lolassistant.http.routes

import cats.effect.IO
import org.http4s.HttpRoutes
import org.kys.lolassistant.PremadeController

object Base {
  def apply(premadeController: PremadeController): HttpRoutes[IO] = {
    val premades = Premades(premadeController)

    premades.toRoutes(identity)
  }
}
