package org.kys.athena.http.routes

import org.kys.athena.http.models.pregame.PregameResponse
import org.kys.athena.modules.{CurrentGameModule, PregameModule}
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.ztapir._
import zio._
import zio.interop.catz._

import java.net.URLDecoder
import java.util.UUID


object LogicEndpoints extends Endpoints {
  // TODO: do something about this mess

  type Env = Has[CurrentGameModule] with Has[PregameModule]
  val currentGameByNameImpl = this.currentGameByName.zServerLogic { case (platform, name, fetchGroups, requestId) =>
    val fetchGroupsDefault = fetchGroups.getOrElse(false)
    val decodedName        = URLDecoder.decode(name, "UTF-8")
    implicit val getReqId: String = requestId.fold(UUID.randomUUID().toString)(identity)

    (for {
      game <- CurrentGameModule.getCurrentGame(platform, decodedName)
      uuidAdded <-
        if (fetchGroupsDefault)
          CurrentGameModule.getGroupsForGameAsync(platform, game).map(u => game.copy(groupUuid = Some(u)))
        else IO.succeed(game)
    } yield uuidAdded).resurrect.flatMapError(ErrorHandler.defaultErrorHandler)
  }

  val currentGameGroupsByNameImpl = this.currentGameGroupsByName.zServerLogic { case (platform, name, requestId) =>
    val decodedName = URLDecoder.decode(name, "UTF-8")
    implicit val getReqId: String = requestId.fold(UUID.randomUUID().toString)(identity)

    (for {
      game <- CurrentGameModule.getCurrentGame(platform, decodedName)
      groups <- CurrentGameModule.getGroupsForGame(platform, game)
    } yield groups).resurrect.flatMapError(ErrorHandler.defaultErrorHandler)
  }

  val currentGameGroupsByUUIDImpl = this.currentGameGroupsByUUID.zServerLogic { case (uuid, requestId) =>
    implicit val getReqId: String = requestId.fold(UUID.randomUUID().toString)(identity)

    (for {
      gg <- CurrentGameModule.getGroupsByUUID(uuid)
    } yield gg).resurrect.flatMapError(ErrorHandler.defaultErrorHandler)
  }

  val pregameByNameImpl = this.pregameByName.zServerLogic { case (platform, names, fetchGroups, requestId) =>
    implicit val getReqId: String = requestId.fold(UUID.randomUUID().toString)(identity)
    (for {
      pg <- PregameModule.getPregameLobby(platform, names)
    } yield PregameResponse(pg, None)).resurrect.flatMapError(ErrorHandler.defaultErrorHandler)
  }

  val pregameGroupsByNameImpl = this.pregameGroupsByName.zServerLogic { case (platform, names, requestId) =>
    implicit val getReqId: String = requestId.fold(UUID.randomUUID().toString)(identity)
    (for {
      pg <- PregameModule.getPregameLobby(platform, names)
      groups <- PregameModule.getGroupsForPregame(platform, pg)
    } yield groups).resurrect.flatMapError(ErrorHandler.defaultErrorHandler)
  }

  val pregameGroupsByUUIDImpl = this.pregameGameGroupsByUUID.zServerLogic { case (uuid, requestId) =>
    implicit val getReqId: String = requestId.fold(UUID.randomUUID().toString)(identity)

    (for {
      gg <- PregameModule.getGroupsByUUID(uuid)
    } yield gg).resurrect.flatMapError(ErrorHandler.defaultErrorHandler)
  }

  val healthzImpl = this.healthz.zServerLogic { _ =>
    UIO.succeed("Ok")
  }

  val publicRoutes = ZHttp4sServerInterpreter.from(List(currentGameByNameImpl.widen[Env],
                                                        currentGameGroupsByNameImpl.widen[Env],
                                                        currentGameGroupsByUUIDImpl.widen[Env],
                                                        pregameByNameImpl.widen[Env],
                                                        pregameGroupsByNameImpl.widen[Env],
                                                        pregameGroupsByUUIDImpl.widen[Env])).toRoutes

  val internalRoutes = ZHttp4sServerInterpreter.from(healthzImpl.widen[Env]).toRoutes
}
