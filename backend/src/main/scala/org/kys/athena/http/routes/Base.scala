package org.kys.athena.http.routes

import cats.effect.IO
import org.http4s.HttpRoutes
import org.kys.athena.controllers.{CurrentGameController, GroupController}


object Base {

  def apply(currentGameController: CurrentGameController,
            groupController: GroupController): HttpRoutes[IO] = {
    val currentGame = CurrentGame(currentGameController, groupController)

    currentGame.toRoutes(identity)
  }
}
