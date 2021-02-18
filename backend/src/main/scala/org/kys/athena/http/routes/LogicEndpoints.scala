package org.kys.athena.http.routes

import org.kys.athena.http.models.pregame.PregameResponse
import org.kys.athena.modules.{CurrentGameModule, GroupModule, PregameModule}
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.ztapir._
import zio._
import zio.interop.catz._

import java.net.URLDecoder
import java.util.UUID


object LogicEndpoints extends Endpoints {
  // TODO: do something about this mess

  type Env = Has[CurrentGameModule] with Has[GroupModule] with Has[PregameModule]
  val currentGameByNameImpl = this.currentGameByName.zServerLogic { case (platform, name, fetchGroups, requestId) =>
    val fetchGroupsDefault = fetchGroups.getOrElse(false)
    val decodedName        = URLDecoder.decode(name, "UTF-8")
    implicit val getReqId: String = requestId.fold(UUID.randomUUID().toString)(identity)

    (for {
      game <- CurrentGameModule.getCurrentGame(platform, decodedName)
      uuidAdded <-
        if (fetchGroupsDefault)
          GroupModule.getGroupsForGameAsync(platform, game).map(u => game.copy(groupUuid = Some(u)))
        else IO.succeed(game)
    } yield uuidAdded).resurrect.flatMapError(ErrorHandler.defaultErrorHandler)
  }

  val groupsByNameImpl = this.groupsByName.zServerLogic { case (platform, name, requestId) =>
    val decodedName = URLDecoder.decode(name, "UTF-8")
    implicit val getReqId: String = requestId.fold(UUID.randomUUID().toString)(identity)

    (for {
      game <- CurrentGameModule.getCurrentGame(platform, decodedName)
      groups <- GroupModule.getGroupsForGame(platform, game)
    } yield groups).resurrect.flatMapError(ErrorHandler.defaultErrorHandler)
  }

  val groupsByUUIDImpl = this.groupsByUUID.zServerLogic { case (uuid, requestId) =>
    implicit val getReqId: String = requestId.fold(UUID.randomUUID().toString)(identity)

    (for {
      gg <- GroupModule.getGroupsByUUID(uuid)
    } yield gg).resurrect.flatMapError(ErrorHandler.defaultErrorHandler)
  }

  val pregameByNameImpl = this.pregameByName.zServerLogic { case (platform, names, fetchGroups, requestId) =>
    implicit val getReqId: String = requestId.fold(UUID.randomUUID().toString)(identity)
    (for {
      pg <- PregameModule.getPregameLobby(platform, names)
    } yield PregameResponse(pg, None)).resurrect.flatMapError(ErrorHandler.defaultErrorHandler)
  }

  val healthzImpl = this.healthz.zServerLogic { _ =>
    UIO.succeed("Ok")
  }

  val publicRoutes = ZHttp4sServerInterpreter.from(List(currentGameByNameImpl.widen[Env],
                                                        groupsByNameImpl.widen[Env],
                                                        groupsByUUIDImpl.widen[Env],
                                                        pregameByNameImpl.widen[Env])).toRoutes

  val internalRoutes = ZHttp4sServerInterpreter.from(healthzImpl.widen[Env]).toRoutes
}
