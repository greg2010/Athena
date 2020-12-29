package org.kys.athena.http.routes

import org.kys.athena.modules.{CurrentGameModule, GroupModule}
import org.kys.athena.modules.CurrentGameModule.CurrentGameController
import org.kys.athena.modules.GroupModule.GroupController
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
        game <- CurrentGameModule.getCurrentGame(platform, decodedName)
        uuidAdded <-
          if (fetchGroupsDefault)
            GroupModule.getGroupsForGameAsync(platform, game).map(u => game.copy(groupUuid = Some(u)))
          else IO.succeed(game)
      } yield uuidAdded)
  }

  type Env = CurrentGameController with GroupController
  val groupsByNameImpl = this.groupsByName.zServerLogic {
    case (platform, name) =>
      val decodedName = URLDecoder.decode(name, "UTF-8")
      (for {
        game <- CurrentGameModule.getCurrentGame(platform, decodedName)
        groups <- GroupModule.getGroupsForGame(platform, game)
      } yield groups)
  }

  val groupsByUUIDImpl = this.groupsByUUID.zServerLogic { uuid =>
    GroupModule.getGroupsByUUID(uuid)
  }

  val healthzImpl = this.healthz.zServerLogic { _ =>
    UIO.succeed("Ok")
  }

  val publicRoutes = ZHttp4sServerInterpreter.from(List(currentGameByNameImpl.widen[Env],
                                                        groupsByNameImpl.widen[Env],
                                                        groupsByUUIDImpl.widen[Env])).toRoutes

  val internalRoutes = ZHttp4sServerInterpreter.from(healthzImpl.widen[Env]).toRoutes
}
