package org.kys.athena.components.ongoing

import com.raquo.domtypes.generic.keys.{Style => CStyle}
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.kys.athena.http.Client._
import org.kys.athena.http.backend.BackendDataHelpers
import org.kys.athena.http.dd.CombinedDD
import org.kys.athena.http.errors.{BackendApiError, InternalServerError, NotFoundError}
import org.kys.athena.http.models.current._
import org.kys.athena.http.models.premade.{PlayerGroup, PremadeResponse}
import org.kys.athena.components.common
import org.kys.athena.components.common.{ChampionIcon, ImgSized, OpggLink, UggLink}
import org.kys.athena.routes.OngoingRoute
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform}
import org.kys.athena.riot.api.dto.currentgameinfo.BannedChampion
import org.kys.athena.util.CSSUtil._
import org.kys.athena.util.{Config, DataState, Failed, Infallible, Loading, Ready, Time}
import org.scalajs.dom.html
import zio._

import scala.concurrent.duration.DurationInt


object OngoingPage {

  private case class GroupHoverEvent(groupIds: List[String], isHovering: Boolean)

  // FETCH LOGIC

  @SuppressWarnings(Array("org.wartremover.warts.Product", "org.wartremover.warts.Serializable"))
  def fetchAndWriteDDragon(ddObs: Observer[DataState[CombinedDD]]): IO[BackendApiError, Unit] = {
    for {
      dd <- {
        ZIO.tupledPar(fetchCachedDDragonChampion(), fetchCachedDDragonRunes(), fetchCachedDDragonSummoners())
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
                            name: String,
                            ongoingObs: Observer[DataState[OngoingGameResponse]],
                            groupsObs: Observer[DataState[PremadeResponse]]): IO[BackendApiError, Unit] = {
    for {
      _ <- UIO.succeed(ongoingObs.onNext(Loading))
      o <- fetchOngoingGameByName(platform, name)
        .map(r => Ready(r))
        .catchAll(err => UIO.succeed(Failed(err)))
      _ <- UIO.effectTotal(ongoingObs.onNext(o))

      gr <- (o match {
        case Failed(_) => IO.fail(InternalServerError("Fetch for players failed, not fetching groups"))
        case _ => {
          o.map(_.groupUuid).toOption.flatten
            .fold(fetchGroupsByName(platform, name))(uuid => fetchGroupsByUUID(uuid))
        }
      }).map(r => Ready(r))
        .catchAll(err => UIO.succeed(Failed(err)))
      _ <- UIO.effectTotal(groupsObs.onNext(gr))
    } yield ()
  }

  def fetchAndWriteAll(platform: Platform,
                       name: String,
                       ddObs: Observer[DataState[CombinedDD]],
                       ongoingObs: Observer[DataState[OngoingGameResponse]],
                       groupsObs: Observer[DataState[PremadeResponse]]): IO[BackendApiError, Unit] = {
    ZIO.tupledPar(fetchAndWriteDDragon(ddObs), fetchAndWriteGameInfo(platform, name, ongoingObs, groupsObs)).unit
  }

  val runtime: Runtime[zio.ZEnv] = Runtime.default
  // RENDER LOGIC

  def render(p: OngoingRoute, hideSearchBar: Observer[Boolean]): HtmlElement = {

    lazy val ddVar      = Var[DataState[CombinedDD]](Loading)
    lazy val ongoingVar = Var[DataState[OngoingGameResponse]](Loading)
    lazy val groupsVar  = Var[DataState[PremadeResponse]](Loading)

    val playerNameSignal: Signal[String] = ongoingVar.signal.map {
      case Ready(v) => v.querySummonerName
      case _ => p.name
    }

    def refreshGame: Unit = {
      runtime.unsafeRunAsync_(fetchAndWriteGameInfo(p.realm,
                                                    p.name,
                                                    ongoingVar.writer,
                                                    groupsVar.writer))
    }
    def refreshAll: Unit = {
      runtime.unsafeRunAsync_(fetchAndWriteAll(p.realm,
                                               p.name,
                                               ddVar.writer,
                                               ongoingVar.writer,
                                               groupsVar.writer))
    }
    val localStateCombinator = ongoingVar.signal.combineWith(ddVar.signal).map {
      case (Failed(_: NotFoundError), _) =>
        hideSearchBar.onNext(true)
        OngoingNotFound.render(p, () => refreshGame)
      case (_, Failed(_)) =>
        OngoingError.render(p, () => refreshAll)
      case (Failed(_), _) =>
        OngoingError.render(p, () => refreshAll)
      case (a: Infallible[OngoingGameResponse], b: Infallible[CombinedDD]) =>
        hideSearchBar.onNext(false)
        renderGame(ongoingVar.signal.map(_ => a), groupsVar.signal, ddVar.signal.map(_ => b), playerNameSignal, p)
    }

    div(
      onMountCallback(_ => refreshAll),
      cls := s"flex flex-col items-center justify-center lg:px-12 mx-4 my-2 divide-y divide-gray-500 $paperCls",
      backgroundColor := paletteContainer,
      child <-- localStateCombinator)
  }

  private def renderGame(gameES: Signal[Infallible[OngoingGameResponse]],
                         premadeES: Signal[DataState[PremadeResponse]],
                         ddES: Signal[Infallible[CombinedDD]],
                         playerNameSignal: Signal[String], p: OngoingRoute) = {

    div(
      cls := s"flex flex-col items-center justify-center divide-y divide-gray-500",
      renderHeader(gameES, p.realm, playerNameSignal),
      div(
        cls := "flex flex-col lg:flex-row divide-y lg:divide-x lg:divide-y-0 divide-gray-500",
        renderTeam(gameES.map(_.map(_.blueTeam)), premadeES.map(_.map(_.blue)), ddES, "Blue", p.realm),
        renderTeam(gameES.map(_.map(_.redTeam)), premadeES.map(_.map(_.red)), ddES, "Red", p.realm)))
  }

  private def renderHeader(ongoingES: Signal[Infallible[OngoingGameResponse]],
                           platform: Platform,
                           playerName: Signal[String]) = {
    def renderQueueName(q: GameQueueTypeEnum): String = {
      q match {
        case GameQueueTypeEnum.SummonersRiftBlind => "Blind | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftDraft => "Draft | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftSoloRanked => "Ranked Solo | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftFlexRanked => "Ranked Flex | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftBotIntro => "Intro Bots | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftBotBeginner => "Beginner Bots | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftBotIntermediate => "Intermediate Bots | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftClash => "Clash | Summoner's Rift"
        case GameQueueTypeEnum.HowlingAbyss => "ARAM | Howling Abyss"
        case GameQueueTypeEnum.HowlingAbyssPoroKing => "Legend Of Poro King | Howling Abyss"
        case GameQueueTypeEnum.SummonersRiftDoomBotsStandard => "Doom Bots | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftDoomBotsVoting => "Doom Bots | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftNexusBlitz => "Nexus Blitz | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftNexusSiege => "Nexus Siege | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftOneForAll => "One for All | Summoner's Rift"
        case GameQueueTypeEnum.SummonersRiftURF => "URF | Summoner's Rift"
      }
    }

    div(cls := s"flex flex-col items-center m-1 mb-4",
        child <-- playerName.map{ n =>
          val nameCls = "text-5xl p-2 text-center"
          span(cls := nameCls, s"Live game of", OpggLink(n, platform, span(n, cls := nameCls)))

        },
        child <-- ongoingES.map {
          case Ready(g) =>
            val startTime = if (g.gameStartTime > 0) g.gameStartTime else {
              System.currentTimeMillis() + 3.minutes.toMillis
            }
            val timeES    = EventStream
              .periodic(1.seconds.toMillis.toInt)
              .map(_ => Time.renderMsInterval(System.currentTimeMillis() - startTime))
            span(s"${renderQueueName(g.gameQueueId)} | ", child.text <-- timeES, cls := "text-md")
          case Loading => span(height := "14px", width := "300px", cls := "animate-pulse bg-gray-500 mt-1")
        })
  }

  private def renderTeam(teamES: Signal[Infallible[OngoingGameTeam]],
                         groupsES: Signal[DataState[Set[PlayerGroup]]],
                         ddES: Signal[Infallible[CombinedDD]],
                         color: String, platform: Platform) = {
    val hoverEventBus = new EventBus[GroupHoverEvent]

    // Sort by position number, zip with index. Index serves as a key for rendering
    val sortedSummonersES = teamES.map { t =>
      t.map { tt =>
        tt.positions match {
          case Some(posns) => {
            tt.summoners
              .map(s => (s, posns.find(_._2 == s.summonerId).map(p => p._1.toOrder).getOrElse(9)))
              .toList.sortBy(_._2).map(_._1).zipWithIndex
          }
          case None => tt.summoners.toList.zipWithIndex
        }
      }
    }

    // Convert to signal with `Loading` guard value, to render loading state
    val maybeSummoners: Signal[Seq[(Infallible[(InGameSummoner, CombinedDD)], Int)]] = sortedSummonersES
      .combineWith(ddES).map(r => r._1.zip(r._2)).map {
      case Ready((l, d)) => l.map(si => (Ready((si._1, d)), si._2))
      case _ => Range(0, 5).map(i => (Loading, i))
    }

    // Find groups signal for each summoner and pass it to render function. Maps to list of render signals
    val renderSignal = maybeSummoners
      .split(_._2) {
        case (_, _, d) =>
          val ss = d.map(_._1)
          OngoingPlayerCard(ss,
                            platform,
                            cls := "px-2 py-1",
                            cls <-- hoverEventBus.events.toWeakSignal.combineWith(ss).map {
                              case (Some(GroupHoverEvent(ids, true)), Ready((sum, _))) if ids.contains(
                                sum.summonerId) => "bg-yellow-100"
                              case _ => ""
                            })
      }

    val bES = teamES.combineWith(ddES).map(r => r._1.zip(r._2)).map { e =>
      e.map {
        case (t, dd) => (t.bans, dd)
      }
    }

    div(
      cls := "flex flex-col items-center px-1 lg:px-8 my-1 divide-y divide-gray-500",
      renderTeamHeader(teamES, color),
      children <-- renderSignal,
      div(cls := "flex justify-center w-full px-2 py-1", renderBans(bES)),
      div(cls := "w-full py-1", renderPlaysWith(color, teamES, groupsES, ddES, hoverEventBus.writer)))
  }

  private def renderTeamHeader(team: Signal[Infallible[OngoingGameTeam]], teamName: String) = {
    case class WinrateSummary(average: Double, range: Double)

    val summary      = team.map { e =>
      e.map { t =>
        val winrates = t.summoners
          .toList
          .flatMap(s => BackendDataHelpers.relevantRankedLeague(s.rankedLeagues))
          .map(_.winRate)
          .sortBy(identity)
        winrates match {
          case ::(head, tail) => {
            val avg = winrates.sum / winrates.length
            Some(WinrateSummary(avg, tail.lastOption.map(_ - head).getOrElse(0D)))
          }
          case Nil => None
        }
      }
    }
    val teamNameElem = span(cls := "text-2xl font-medium text-center", s"${teamName} Team")
    div(
      cls := s"flex justify-around items-center p-1 my-1 w-full",
      children <-- summary.map {
        case Ready(Some(s)) =>
          List(
            div(cls := "flex flex-col items-center",
                span(cls := "text-center", "Average Winrate"),
                span(cls := "text-center",
                     color := BackendDataHelpers.winrateColor(s.average),
                     s"${BackendDataHelpers.roundWinrate(s.average)}%")),
            teamNameElem,
            div(cls := "flex flex-col items-center",
                span(cls := "text-center", "Winrate Range"),
                span(cls := "text-center", s"${BackendDataHelpers.roundWinrate(s.range)}%")))
        case Ready(None) => List(teamNameElem)
        case Loading =>
          List(
            div(
              cls := "flex flex-col items-center",
              div(height := "14px", width := "100px", cls := "animate-pulse bg-gray-500"),
              div(height := "14px", width := "50px", cls := "animate-pulse bg-gray-500 mt-1")),
            div(height := "40px", width := "150px", cls := "animate-pulse bg-gray-500 mx-1"),
            div(
              cls := "flex flex-col items-center",
              div(height := "14px", width := "100px", cls := "animate-pulse bg-gray-500"),
              div(height := "14px", width := "50px", cls := "animate-pulse bg-gray-500 mt-1")))
      })
  }

  private def renderBans(bansES: Signal[Infallible[(Option[Set[BannedChampion]], CombinedDD)]])
  : ReactiveHtmlElement[html.Div] = {
    div(
      cls := s"flex inline-flex my-1",
      children <-- bansES.map {
        case Ready((None, _)) =>
          inContext { ctx: ReactiveHtmlElement[html.Div] =>
            ctx.amend(cls := "hidden")
          }
          List()
        case Ready((Some(banned), dd)) =>
          banned.map { ch =>
            val bannedChamp = div(
              width := "64px",
              height := "64px",
              cls := "mx-1",
              div(
                position := "absolute",
                height := "64px",
                ChampionIcon(
                  ch.championId,
                  size = 64,
                  dd,
                  position := "relative",
                  zIndex := 1,
                  new CStyle("filter", "filter") := "grayscale(50%)",
                  cls := "rounded-lg"),
                ImgSized(s"${Config.FRONTEND_URL}/slash_red_256.png",
                         imgWidth = 64,
                         imgHeight = Some(64),
                         position := "relative",
                         top := "-64px",
                         zIndex := 2,
                         cls := "rounded-lg")))
            UggLink(ch.championId, dd, bannedChamp)
          }.toList
        case Loading =>
          Range(0, 5).map { _ =>
            div(width := "64px", height := "64px", cls := "rounded-lg animate-pulse bg-gray-500 mx-1")
          }
      })
  }

  private def renderPlaysWith(color: String,
                              teamES: Signal[Infallible[OngoingGameTeam]],
                              groupsES: Signal[DataState[Set[PlayerGroup]]],
                              ddES: Signal[Infallible[CombinedDD]],
                              hoverObserver: Observer[GroupHoverEvent]) = {
    def renderPlayerSet(s: Set[InGameSummoner], gamesPlayed: Int, dd: CombinedDD) = {
      div(
        cls := "inline-flex flex-col items-center justify-center mx-2 mb-1",
        div(
          cls := "flex flex-row items-center mx-1",
          s.map { ig =>
            common.UggLink(ig.championId, dd, ChampionIcon(ig.championId, 36, dd, cls := "mx-1"))
          }.toList),
        span(cls := "text-sm leading-tight", s"$gamesPlayed ${if (gamesPlayed == 1) "game" else "games"}"),
        onMouseOver.mapTo(GroupHoverEvent(s.map(_.summonerId).toList, isHovering = true)) --> hoverObserver,
        onMouseOut.mapTo(GroupHoverEvent(s.map(_.summonerId).toList, isHovering = false)) --> hoverObserver)
    }

    val renderSignal = teamES
      .combineWith(groupsES).map(lr => lr._1.zip(lr._2))
      .combineWith(ddES).map(lr => lr._1.zip(lr._2)).map {
      case Ready(((team, group), dd)) =>
        group.map { g =>
          val inGameSummoners = g.summoners.flatMap(ss => team.summoners.find(_.summonerId == ss))
          renderPlayerSet(inGameSummoners, g.gamesPlayed, dd)
        }.toList
      case Failed(_) =>
        List(span(cls := "text-lg", "Error fetching player groups"))
      case Loading => List(div(height := "50px", width := "400px", cls := "animate-pulse bg-gray-500 rounded-lg"))
    }.map { elems =>
      if (elems.isEmpty) {
        List(span(cls := "text-lg", "No groups"))
      } else {
        elems
      }
    }
    div(
      cls := "flex flex-col justify-center",
      span(cls := "text-xl font-medium leading-tight text-center my-1", s"$color team groups"),
      div(
        cls := s"flex flex-wrap items-center justify-center",
        maxWidth := "400px", // TODO: this is smelly
        children <-- renderSignal))
  }
}
