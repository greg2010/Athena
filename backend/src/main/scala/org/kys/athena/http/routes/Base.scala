package org.kys.athena.http.routes

import cats.effect.IO
import org.http4s.HttpRoutes
import org.kys.athena.controllers.{CurrentGameController, GroupController, PositionHeuristicsController}


object Base {

  def apply(currentGameController: CurrentGameController,
            groupController: GroupController,
            heuristicsController: PositionHeuristicsController): HttpRoutes[IO] = {
    val currentGame = CurrentGame(currentGameController, groupController, heuristicsController)

    currentGame.toRoutes(identity)
  }
}
