package org.kys.athena.http.routes

import cats.effect.IO
import io.circe.{Encoder, KeyEncoder}
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._
import io.circe.generic.auto._
import org.kys.athena.api.Platform
import org.kys.athena.api.dto.common.{GameQueueTypeEnum, SummonerSpellsEnum}
import org.kys.athena.controllers.{CurrentGameController, GroupController, PositionHeuristicsController}
import org.kys.athena.data.PositionEnum
import org.kys.athena.http.models.PremadeResponse
import org.kys.athena.util.exceptions.BadRequestException


object Premades {

  def apply(currentGameController: CurrentGameController,
            groupController: GroupController,
            heuristicsController: PositionHeuristicsController): RhoRoutes[IO] = {
    new RhoRoutes[IO] {

      "Endpoint to get premades in a game for a user" ** GET / "premades" / "by-summoner-name" /
      pathVar[String]("summonerName") +? param[String]("platform") &
      param[Int]("gameDepth", 5) |>> { (summonerName: String, platform: String, gameDepth: Int) =>
        implicit val qIdEnc            : Encoder[GameQueueTypeEnum] = GameQueueTypeEnum.circeEncoder
        implicit val PositionKeyEncoder: KeyEncoder[PositionEnum]   = (position: PositionEnum) => position.toString
        import org.http4s.circe.CirceEntityEncoder._

        val gameDepthClamped = scala.math.min(scala.math.max(1, gameDepth), 15)
        Platform.withNameInsensitiveOption(platform) match {
          case Some(p) => {
            for {
              game <- currentGameController.getCurrentGame(p, summonerName)
              groups <- groupController.getGroupsForGame(p, game, gameDepthClamped)
              blueTeamPositions <- IO.pure(heuristicsController.estimatePositions(game, game.blueTeamSummoners))
              redTeamPositions <- IO.pure(heuristicsController.estimatePositions(game, game.redTeamSummoners))
            } yield Ok(PremadeResponse(game,
                                       groups.blueTeamGroups, groups.redTeamGroups,
                                       blueTeamPositions, redTeamPositions))
          }
          case None => IO.raiseError(BadRequestException("Bad platform"))
        }
      }
    }
  }
}
