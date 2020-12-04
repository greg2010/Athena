package org.kys.athena.http.routes

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._
import org.kys.athena.controllers.{CurrentGameController, GroupController}
import org.kys.athena.http.models.common.ErrorResponse
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.util.exceptions.BadRequestException

import java.util.UUID


object CurrentGame {

  def apply(currentGameController: CurrentGameController,
            groupController: GroupController): RhoRoutes[IO] = {
    new RhoRoutes[IO] {

      "Endpoint to get current game info for a player" ** GET / "current" / "by-summoner-name" /
      pathVar[String]("platform") /
      pathVar[String]("summonerName") +?
      param[Boolean]("fetchGroups", false) &
      param[Int]("gameDepth", 5) |>> {
        (platform: String, summonerName: String, fetchGroups: Boolean, gameDepth: Int) =>
          Platform.withNameInsensitiveOption(platform) match {
            case Some(p) => {
              for {
                game <- currentGameController.getCurrentGame(p, summonerName)
                uuidAdded <-
                  if (fetchGroups)
                    groupController.getGroupsForGameAsync(p, game, gameDepth).map(u => game.copy(groupUuid = Some(u)))
                  else IO.pure(game)
              } yield Ok(uuidAdded)
            }
            case None => IO.raiseError(BadRequestException("Bad platform"))
          }
      }

      "Endpoint to get premades in a game for a player" ** GET / "current" / "by-summoner-name" /
      pathVar[String]("platform") / pathVar[String]("summonerName") / "groups" +?
      param[Int]("gameDepth", 5) |>> { (platform: String, summonerName: String, gameDepth: Int) =>
        val gameDepthClamped = scala.math.min(scala.math.max(1, gameDepth), 15)
        Platform.withNameInsensitiveOption(platform) match {
          case Some(p) => {
            for {
              game <- currentGameController.getCurrentGame(p, summonerName)
              groups <- groupController.getGroupsForGame(p, game, gameDepthClamped)
            } yield Ok(groups)
          }
          case None => IO.raiseError(BadRequestException("Bad platform"))
        }
      }

      "Endpoint to get premades in a game for a player by UUID" ** GET / "current" / "by-uuid" /
      pathVar[UUID]("uuid") / "groups" |>> { (uuid: UUID) =>
        groupController.getGroupsByUUID(uuid).map {
          case Some(r) => Ok(r)
          case None => NotFound(ErrorResponse(1, "UUID not found"))
        }
      }
    }
  }
}
