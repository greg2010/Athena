package org.kys.athena.http.routes

import cats.effect.IO
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._
import io.circe.generic.auto._
import org.kys.athena.{CurrentGameController, GroupController}
import org.kys.athena.api.Platform
import org.kys.athena.util.exceptions.BadRequestException


object Premades {

  def apply(currentGameController: CurrentGameController, groupController: GroupController): RhoRoutes[IO] = {
    new RhoRoutes[IO] {

      "Endpoint to get premades in a game for a user" ** GET / "premades" / "by-summoner-name" /
      pathVar[String]("summonerName") +? param[String]("platform") &
      param[Int]("gameDepth", 5) |>> { (summonerName: String, platform: String, gameDepth: Int) =>
        import org.http4s.circe.CirceEntityEncoder._
        val gameDepthClamped = scala.math.min(scala.math.max(1, gameDepth), 15)
        Platform.withNameInsensitiveOption(platform) match {
          case Some(p) => {
            for {
              game <- currentGameController.getCurrentGame(p, summonerName)
              gameWithGroups <- groupController.getGroupsForGame(p, game, gameDepthClamped)
            } yield Ok(gameWithGroups)
          }
          case None => IO.raiseError(BadRequestException("Bad platform"))
        }
      }
    }
  }
}
