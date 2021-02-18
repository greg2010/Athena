package org.kys.athena.modules

import org.kys.athena.http.models.common.RankedLeague
import org.kys.athena.http.models.pregame.PregameSummoner
import org.kys.athena.riot.api.dto.common.Platform
import zio._


trait PregameModule {
  def getPregameLobby(platform: Platform, names: Set[String])
                     (implicit reqId: String): IO[Throwable, Set[PregameSummoner]]
}

object PregameModule {
  val live = ZLayer.fromService[RiotApiModule.Service, PregameModule] { riotApiClient =>
    new PregameModule {
      override def getPregameLobby(platform: Platform,
                                   names: Set[String])
                                  (implicit reqId: String): IO[Throwable, Set[PregameSummoner]] = {
        for {
          summoners <- ZIO.foreachPar(names)(name => riotApiClient.summonerByName(name, platform))
          leagues <- ZIO.foreachPar(summoners)(s => riotApiClient.leaguesBySummonerId(s.id, platform).map(l => (s, l)))
        } yield leagues.map(t => PregameSummoner(t._1.name, t._1.id, t._1.summonerLevel,
                                                 t._2.map(RankedLeague(_))))
      }
    }
  }

  def getPregameLobby(platform: Platform, names: Set[String])
                     (implicit reqId: String): ZIO[Has[PregameModule], Throwable, Set[PregameSummoner]] = {
    ZIO.accessM[Has[PregameModule]](_.get.getPregameLobby(platform, names)(reqId))
  }
}
