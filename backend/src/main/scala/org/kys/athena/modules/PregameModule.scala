package org.kys.athena.modules

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import org.kys.athena.http
import org.kys.athena.http.models.common.RankedLeague
import org.kys.athena.http.models.pregame.{PregameResponse, PregameSummoner}
import org.kys.athena.http.models.premade.{PlayerGroup, PremadeResponse}
import org.kys.athena.riot.api.dto.common.Platform
import zio._

import java.util.UUID
import scala.concurrent.duration.DurationInt


trait PregameModule {
  def getPregameLobby(platform: Platform, names: Set[String])
                     (implicit reqId: String): IO[Throwable, Set[PregameSummoner]]

  def getGroupsForPregame(platform: Platform,
                          summoners: Set[PregameSummoner],
                          gamesQueryCount: Int = 5)
                         (implicit reqId: String): IO[Throwable, Set[PlayerGroup]]

  def getGroupsForPregameAsync(platform: Platform,
                               summoners: Set[PregameSummoner],
                               gamesQueryCount: Int = 5)
                              (implicit reqId: String): IO[Throwable, UUID]

  def getGroupsByUUID(uuid: UUID): IO[Throwable, Set[PlayerGroup]]
}

object PregameModule {
  val live = ZLayer.fromServices[RiotApiModule.Service, GroupModule, PregameModule] { (riotApiClient, groupModule) =>
    new PregameModule {

      val uuidCache: Cache[UUID, Promise[Throwable, Set[PlayerGroup]]] = Scaffeine()
        .recordStats()
        .expireAfterWrite(5.minutes)
        .build[UUID, Promise[Throwable, Set[PlayerGroup]]]()

      override def getPregameLobby(platform: Platform,
                                   names: Set[String])
                                  (implicit reqId: String): IO[Throwable, Set[PregameSummoner]] = {
        for {
          summoners <- ZIO.foreachPar(names)(name => riotApiClient.summonerByName(name, platform))
          leagues <- ZIO.foreachPar(summoners)(s => riotApiClient.leaguesBySummonerId(s.id, platform).map(l => (s, l)))
        } yield leagues.map(t => {
          PregameSummoner(t._1.name, t._1.id, t._1.summonerLevel,
                          t._2.map(RankedLeague(_)))
        })
      }

      override def getGroupsForPregame(platform: Platform,
                                       summoners: Set[PregameSummoner],
                                       gamesQueryCount: Int)
                                      (implicit reqId: String): IO[Throwable, Set[PlayerGroup]] = {
        groupModule.groupsForTeam(summoners.map(_.summonerId), gamesQueryCount, platform)
      }

      override def getGroupsForPregameAsync(platform: Platform,
                                            summoners: Set[PregameSummoner],
                                            gamesQueryCount: Int)
                                           (implicit reqId: String): IO[Throwable, UUID] = {
        val promise = Promise.make[Throwable, Set[PlayerGroup]]
        for {
          uuid <- Task.effect(UUID.randomUUID())
          p <- promise
          _ <- Task.effect(uuidCache.put(uuid, p))
          _ <- p
            .complete(getGroupsForPregame(platform, summoners, gamesQueryCount))
            .forkDaemon
        } yield uuid
      }

      override def getGroupsByUUID(uuid: UUID): IO[Throwable, Set[PlayerGroup]] = {
        for {
          entry <- IO.effect(uuidCache.getIfPresent(uuid))
          res <- entry match {
            case Some(v) => {
              v.await.either.flatMap {
                case Right(re) => IO.succeed(re)
                case Left(ex) => // TODO: change log text
                  scribe.error(s"Get groups by UUID failed uuid=$uuid", ex)
                  IO.fail(ex)
              }
            }
            case None => IO.fail(http.errors.NotFoundError("UUID not found"))
          }
        } yield res
      }
    }
  }

  def getPregameLobby(platform: Platform, names: Set[String])
                     (implicit reqId: String): ZIO[Has[PregameModule], Throwable, Set[PregameSummoner]] = {
    ZIO.accessM[Has[PregameModule]](_.get.getPregameLobby(platform, names)(reqId))
  }

  def getGroupsForPregame(platform: Platform,
                          summoners: Set[PregameSummoner],
                          gamesQueryCount: Int = 5)
                         (implicit reqId: String): ZIO[Has[PregameModule], Throwable, Set[PlayerGroup]] = {
    ZIO.accessM[Has[PregameModule]](_.get.getGroupsForPregame(platform, summoners, gamesQueryCount)(reqId))
  }

  def getGroupsForPregameAsync(platform: Platform,
                               summoners: Set[PregameSummoner],
                               gamesQueryCount: Int = 5)
                              (implicit reqId: String): ZIO[Has[PregameModule], Throwable, UUID] = {
    ZIO.accessM[Has[PregameModule]](_.get.getGroupsForPregameAsync(platform, summoners, gamesQueryCount))
  }

  def getGroupsByUUID(uuid: UUID): ZIO[Has[PregameModule], Throwable, Set[PlayerGroup]] = {
    ZIO.accessM[Has[PregameModule]](_.get.getGroupsByUUID(uuid))
  }
}