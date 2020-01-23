package org.kys.athena.http.routes

import cats.effect.IO
import org.http4s.HttpRoutes
import org.kys.athena.PremadeController


object Base {

  def apply(premadeController: PremadeController): HttpRoutes[IO] = {
    val premades = Premades(premadeController)

    premades.toRoutes(identity)
  }
}
