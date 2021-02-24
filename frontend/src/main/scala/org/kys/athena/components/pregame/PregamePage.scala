package org.kys.athena.components.pregame

import com.raquo.laminar.api.L._
import org.kys.athena.http.BackendClient.{fetchPregameGameByName, fetchPregameGroupsByName, fetchPregameGroupsByUUID}
import org.kys.athena.http.DDClient
import org.kys.athena.http.dd.CombinedDD
import org.kys.athena.http.errors.{BackendApiError, InternalServerError}
import org.kys.athena.http.models.pregame.PregameResponse
import org.kys.athena.http.models.premade.PlayerGroup
import org.kys.athena.riot.api.dto.common.Platform
import org.kys.athena.routes.PregameRoute
import org.kys.athena.util.CSSUtil.{paletteContainer, paperCls}
import org.kys.athena.util.{DataState, Failed, Loading, Ready}
import zio.{IO, Runtime, UIO, ZIO}


object PregamePage {
  @SuppressWarnings(Array("org.wartremover.warts.Product", "org.wartremover.warts.Serializable"))
  def fetchAndWriteDDragon(ddObs: Observer[DataState[CombinedDD]]): IO[BackendApiError, Unit] = {
    for {
      dd <- {
        ZIO.tupledPar(DDClient.fetchCachedDDragonChampion(),
                      DDClient.fetchCachedDDragonRunes(),
                      DDClient.fetchCachedDDragonSummoners())
          .either.map {
          case Left(ex) => Failed(ex)
          case Right((c, r, s)) => Ready(CombinedDD(c, r, s))
        }
      }
      _ <- UIO.effectTotal(ddObs.onNext(dd))
    } yield ()
  }

  @SuppressWarnings(Array("org.wartremover.warts.Product", "org.wartremover.warts.Serializable"))
  def fetchAndWriteGameInfo(platform: Platform,
                            names: List[String],
                            ongoingObs: Observer[DataState[PregameResponse]],
                            groupsObs: Observer[DataState[Set[PlayerGroup]]]): IO[BackendApiError, Unit] = {
    for {
      _ <- UIO.succeed(ongoingObs.onNext(Loading))
      o <- fetchPregameGameByName(platform, names)
        .map(r => Ready(r))
        .catchAll(err => UIO.succeed(Failed(err)))
      _ <- UIO.effectTotal(ongoingObs.onNext(o))

      gr <- (o match {
        case Failed(_) => IO.fail(InternalServerError("Fetch for players failed, not fetching groups"))
        case _ => {
          o.map(_.groupUuid).toOption.flatten
            .fold(fetchPregameGroupsByName(platform, names))(uuid => fetchPregameGroupsByUUID(uuid))
        }
      }).map(r => Ready(r))
        .catchAll(err => UIO.succeed(Failed(err)))
      _ <- UIO.effectTotal(groupsObs.onNext(gr))
    } yield ()
  }


  def fetchAndWriteAll(platform: Platform,
                       names: List[String],
                       ddObs: Observer[DataState[CombinedDD]],
                       ongoingObs: Observer[DataState[PregameResponse]],
                       groupsObs: Observer[DataState[Set[PlayerGroup]]]): IO[BackendApiError, Unit] = {
    ZIO.tupledPar(fetchAndWriteDDragon(ddObs), fetchAndWriteGameInfo(platform, names, ongoingObs, groupsObs)).unit
  }


  def render(p: PregameRoute): HtmlElement = {
    lazy val ddVar      = Var[DataState[CombinedDD]](Loading)
    lazy val pregameVar = Var[DataState[PregameResponse]](Loading)
    lazy val groupsVar  = Var[DataState[Set[PlayerGroup]]](Loading)

    val runtime: Runtime[zio.ZEnv] = Runtime.default

    def refreshGame: Unit = {
      runtime.unsafeRunAsync_(fetchAndWriteGameInfo(p.realm,
                                                    p.names,
                                                    pregameVar.writer,
                                                    groupsVar.writer))
    }
    def refreshAll: Unit = {
      runtime.unsafeRunAsync_(fetchAndWriteAll(p.realm,
                                               p.names,
                                               ddVar.writer,
                                               pregameVar.writer,
                                               groupsVar.writer))
    }

    div(
      onMountCallback(_ => refreshAll),
      cls := s"flex flex-col items-center justify-center lg:px-12 mx-4 my-2 divide-y divide-gray-500 $paperCls",
      backgroundColor := paletteContainer,
      span(cls := "text-3xl p-2 text-center", s"Pregame lobby of ${p.names.mkString(", ")}"),
      div(
        cls := s"flex flex-col lg:flex-row items-center justify-center divide-x divide-gray-500",
        children <-- pregameVar.signal.map {
          case Loading => List(div())
          case Ready(data) =>
            data.summoners. map { summoner =>
              div(
                cls := "flex flex-col justify-center items-center",
                span(summoner.name),
                span(summoner.summonerLevel.toString),
                div(
                  summoner.rankedLeagues.map { rl =>
                    div(rl.leagueId)
                  }
                )
              )
            }.toList
        }
        ))
  }
}
