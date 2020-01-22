package org.kys.lolassistant.http.routes

import cats.effect.IO
import io.circe.syntax._
import org.http4s.rho.RhoRoutes
import org.kys.lolassistant.PremadeController
import org.kys.lolassistant.api.Platform
import org.http4s.rho.swagger.syntax.io._
import io.circe.generic.auto._
import org.kys.lolassistant.PremadeController
import org.kys.lolassistant.api.Platform
import org.kys.lolassistant.util.exceptions.BadRequestException

object Premades {
  def apply(premadeController: PremadeController): RhoRoutes[IO] = new RhoRoutes[IO] {

    "Endpoint to get premades in a game for a user" **
      GET / "premades" / "by-summoner-name" / pathVar[String] +? param[String]("platform") |>>
      { (summonerName: String, platform: String) =>
        import org.http4s.circe.CirceEntityEncoder._
        Platform.withNameInsensitiveOption(platform) match {
          case Some(p) =>
            premadeController.getPremades(p, summonerName).flatMap(resp => Ok(resp))
          case None => IO.raiseError(BadRequestException("Bad platform"))
        }
      }
  }
}