package org.kys.athena.http.routes

import org.kys.athena.controllers.{CurrentGameController, GroupController}
import org.kys.athena.controllers.CurrentGameController.CurrentGameController
import org.kys.athena.controllers.GroupController.GroupController
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.ztapir._
import zio._
import zio.interop.catz._

import java.net.URLDecoder


object LogicEndpoints extends Endpoints {
  // TODO: do something about this mess

  val currentGameByNameImpl = this.currentGameByName.zServerLogic {
    case (platform, name, fetchGroups) =>
      val fetchGroupsDefault = fetchGroups.getOrElse(false)
      val decodedName = URLDecoder.decode(name, "UTF-8")
      (for {
        game <- CurrentGameController.getCurrentGame(platform, decodedName)
        uuidAdded <-
          if (fetchGroupsDefault)
            GroupController.getGroupsForGameAsync(platform, game).map(u => game.copy(groupUuid = Some(u)))
          else IO.succeed(game)
      } yield uuidAdded)
  }

  type Env = CurrentGameController with GroupController
  val groupsByNameImpl = this.groupsByName.zServerLogic {
    case (platform, name) =>
      val decodedName = URLDecoder.decode(name, "UTF-8")
      (for {
        game <- CurrentGameController.getCurrentGame(platform, decodedName)
        groups <- GroupController.getGroupsForGame(platform, game)
      } yield groups)
  }

  val groupsByUUIDImpl = this.groupsByUUID.zServerLogic { uuid =>
    GroupController.getGroupsByUUID(uuid)
  }

  val routes = ZHttp4sServerInterpreter.from(List(currentGameByNameImpl.widen[Env],
                                                  groupsByNameImpl.widen[Env],
                                                  groupsByUUIDImpl.widen[Env])).toRoutes
}
