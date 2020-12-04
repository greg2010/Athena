package org.kys.athena.views


import cats.effect.IO
import cats.implicits._
import com.raquo.domtypes.generic.keys.{Style => CStyle}
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.kys.athena.http.Client._
import org.kys.athena.http.DData
import org.kys.athena.http.models.current._
import org.kys.athena.http.models.premade.{PlayerGroup, PremadeResponse}
import org.kys.athena.pages.CurrentGamePage
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform, SummonerSpellsEnum}
import org.kys.athena.riot.api.dto.currentgameinfo.BannedChampion
import org.kys.athena.riot.api.dto.ddragon.runes.Rune
import org.kys.athena.riot.api.dto.league.{MiniSeries, RankedQueueTypeEnum, TierEnum}
import org.kys.athena.util.IOToAS.{DataState, writeIOToObserver}
import org.kys.athena.util.exceptions.NotFoundException
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.duration.DurationInt


object CurrentGameView extends View[CurrentGamePage] {
  type LoadState[T] = Option[T]

  def inProgress[T]: Either[Throwable, Option[T]] = Right(None)

  // FETCH LOGIC
  val debug = false

  lazy val ddVar      = Var[DataState[DData]](inProgress[DData])
  lazy val ongoingVar = Var[DataState[OngoingGameResponse]](inProgress[OngoingGameResponse])
  lazy val groupsVar  = Var[DataState[PremadeResponse]](inProgress[PremadeResponse])

  def fetchAndWriteDDragon: IO[Unit] = {
    val dd = (fetchCachedDDragonChampion(), fetchCachedDDragonRunes(), fetchCachedDDragonSummoners()).parMapN(DData)
    writeIOToObserver(dd, ddVar.writer)
  }

  def fetchAndWriteGameInfo(platform: Platform, name: String): IO[Unit] = {
    for {
      g <- {
        val fg = fetchOngoingGameByName(platform, name)(debug)
        writeIOToObserver(fg, ongoingVar.writer).flatMap(_ => fg)
      }
      _ <- g.groupUuid match {
        case Some(u) => writeIOToObserver(fetchGroupsByUUID(u)(debug), groupsVar.writer)
        case None => {
          scribe.warn("Server did not return a UUID, querying groups manually")
          writeIOToObserver(fetchGroupsByName(platform, name)(debug), groupsVar.writer)
        }
      }
    } yield ()
  }

  def fetchAndWriteAll(platform: Platform, name: String): IO[Unit] = {
    (fetchAndWriteDDragon, fetchAndWriteGameInfo(platform, name)).parTupled.map(_ => ())
  }

  // RENDER LOGIC

  val paperCls = "bg-white border border-gray-300 shadow-lg rounded-lg"

  override def render(p: CurrentGamePage): HtmlElement = {

    def bodySignal = {
      ongoingVar.signal.combineWith(ddVar.signal).map {
        case (Right(_), Right(_)) =>
          renderNoError(ongoingVar.signal.map(_.getOrElse(None)),
                        groupsVar.signal.map(_.getOrElse(None)),
                        ddVar.signal.map(_.getOrElse(None)), p)
        case (Left(ex: NotFoundException), _) => List(renderNotFound(p))
        case _ => List(renderError(p))
      }
    }
    div(
      onMountCallback(_ => fetchAndWriteAll(p.realm, p.name).unsafeRunAsyncAndForget()),
      cls := "flex flex-col items-center container-md flex-grow justify-center p-4 " +
             s"$paperCls my-10 bg-opacity-50",
      children <-- bodySignal)
  }

  private def renderNotFound(p: CurrentGamePage) = {
    div(
      cls := s"flex flex-col container-md $paperCls items-center p-8",
      img(
        src := "/blitzcrank_logo.png"
        ),
      span(
        cls := "text-xl mt-4",
        "Summoner ",
        b(s"${p.realm.toString}/${p.name}"),
        " is not currently in game."),
      button(
        cls := "bg-gray-300, border border-gray-300 rounded-lg p-2 mt-4",
        "Retry",
        onClick --> Observer[dom.MouseEvent](
          onNext = _ => fetchAndWriteGameInfo(p.realm, p.name).unsafeRunAsyncAndForget())))
  }

  private def renderError(p: CurrentGamePage) = {
    div(
      cls := s"flex flex-col container-md $paperCls items-center p-8",
      img(
        src := "/amumu_error.png"
        ),
      span(
        cls := "text-xl mt-4",
        "Server error occurred."),
      button(
        cls := "bg-gray-300, border border-gray-300 rounded-lg p-2 mt-4",
        "Retry",
        onClick --> Observer[dom.MouseEvent](
          onNext = _ => fetchAndWriteAll(p.realm, p.name).unsafeRunAsyncAndForget())))
  }

  private def renderNoError(gameES: Signal[LoadState[OngoingGameResponse]],
                            premadeES: Signal[LoadState[PremadeResponse]],
                            ddES: Signal[LoadState[DData]], p: CurrentGamePage) = {


    val playerName: Signal[String] = ongoingVar.signal.map {
      case Right(Some(v)) => v.querySummonerName
      case _ => p.name
    }

    List(
      renderHeader(gameES, playerName),
      div(
        cls := "flex flex-col lg:flex-row",
        renderTeam(gameES.map(_.map(_.blueTeam)), premadeES.map(_.map(_.blue)), ddES, "Blue"),
        renderTeam(gameES.map(_.map(_.redTeam)), premadeES.map(_.map(_.red)), ddES, "Red")))
  }

  private def renderHeader(ongoingES: Signal[LoadState[OngoingGameResponse]], playerName: Signal[String]) = {
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
        case GameQueueTypeEnum.HowlingAbyss => "ARAM"
        case GameQueueTypeEnum.Other => "Other"
      }
    }

    div(cls := s"flex flex-col items-center $paperCls p-4 m-4",
        child <-- playerName.map(n => span(cls := "text-center", s"Live game of $n", cls := "text-5xl p-2")),
        child <-- ongoingES.map {
          case Some(g) =>
            val timeES = EventStream.periodic(1.seconds.toMillis.toInt).map { _ =>
              val rawDiff = System.currentTimeMillis() - g.gameStartTime
              val sign    = if (rawDiff > 0) "" else "-"
              val diff    = Math.abs(rawDiff)
              val h       = diff / (3600 * 1000)
              val m       = (diff - h * 3600 * 1000) / (60 * 1000)
              val s       = (diff - (h * (3600 * 1000) + m * (60 * 1000))) / 1000
              val hstr    = if (h > 0) s"$h:" else ""
              s"$sign$hstr$m:$s"
            }
            span(s"${renderQueueName(g.gameQueueId)} | ", child.text <-- timeES, cls := "mt-4 text-md")
          case None =>
            span(height := "14px", width := "300px", cls := "animate-pulse bg-gray-500 mt-4")
        })
  }

  private def renderTeam(teamES: Signal[LoadState[OngoingGameTeam]],
                         groupsES: Signal[LoadState[Set[PlayerGroup]]],
                         ddES: Signal[LoadState[DData]],
                         color: String) = {

    def transitiveJoinSets[T](s: Set[Set[T]]): Set[Set[T]] = {
      s.foldLeft(Set.empty[Set[T]])((cum, cur) => {
        val (hasCommon, rest) = cum.partition(_.nonEmpty & cur.nonEmpty)
        rest + (cur ++ hasCommon.flatten)
      })
    }

    def findGroupBySummoner(g: Set[Set[InGameSummoner]], s: InGameSummoner): Option[Set[InGameSummoner]] = {
      g.find(_.contains(s))
    }

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


    // Transitively expand sets. Guarantees each player occurs in at most one set.
    val expandedGroupsES = teamES.combineWith(groupsES).map(_.tupled).map { e =>
      e.map {
        case (t, g) => {
          val sums = g.map(_.summoners.flatMap(s => t.summoners.find(_.summonerId == s)))
          transitiveJoinSets(sums)
        }
      }
    }

    // Convert to signal with `None` guard value, to render loading state
    val maybeSummoners = sortedSummonersES
      .combineWith(ddES).map(_.tupled).map {
      case Some((l, d)) => l.map(si => ((si._1, d).some, si._2))
      case None => Range(0, 5).map(i => (None, i))
    }

    // Find groups signal for each summoner and pass it to render function. Maps to list of render signals
    val renderSignal = maybeSummoners
      .split(_._2) {
        case (_, _, d) =>
          val ss = d.map(_._1)
          val gs = ss
            .combineWith(expandedGroupsES)
            .map(_.mapN((s, g) => findGroupBySummoner(g, s._1)))
          renderPlayerCard(ss, gs)
      }

    val bES = teamES.combineWith(ddES).map(_.tupled).map { e =>
      e.map {
        case (t, dd) => (t.bans, dd)
      }
    }


    div(
      cls := "flex flex-col items-center justify-center mx-4 my-1",
      renderTeamHeader(teamES, color),
      children <-- renderSignal,
      renderBans(bES))
  }

  private def renderTeamHeader(team: Signal[LoadState[OngoingGameTeam]], teamName: String) = {
    case class WinrateSummary(average: Double, range: Double)

    val summary      = team.map { e =>
      e.map { t =>
        val winrates = t.summoners.toList.flatMap(s => getRankedData(s.rankedLeagues)).map(_.winRate).sortBy(identity)
        winrates match {
          case ::(head, tail) => {
            val avg = winrates.sum / winrates.length
            WinrateSummary(avg, tail.lastOption.map(_ - head).getOrElse(0D)).some
          }
          case Nil => None
        }
      }
    }
    val teamNameElem = span(cls := "text-3xl", s"${teamName} Team")
    div(
      cls := s"flex justify-around items-center p-4 mb-4 w-full $paperCls",
      children <-- summary.map {
        case Some(Some(s)) =>
          List(
            div(cls := "flex flex-col items-center", span("Average Winrate"),
                span(color := winrateColor(s.average), s"${roundWinrate(s.average)}%")),
            teamNameElem,
            div(cls := "flex flex-col items-center", span("Winrate Range"), span(s"${roundWinrate(s.range)}%")))
        case Some(None) => List(teamNameElem)
        case None =>
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

  private def renderBans(bansES: Signal[LoadState[(Option[Set[BannedChampion]], DData)]])
  : ReactiveHtmlElement[html.Div] = {
    div(
      cls := s"flex inline-flex py-1 mt-2 $paperCls",
      children <-- bansES.map {
        case Some((None, _)) =>
          inContext { ctx: ReactiveHtmlElement[html.Div] =>
            ctx.amend(cls := "hidden")
          }
          List()
        case Some((Some(banned), dd)) =>
          banned.map { ch =>
            val url = dd.championUrl(dd.championById(ch.championId))
            div(
              width := "64px",
              height := "64px",
              cls := "mx-1",
              div(
                position := "absolute",
                img(
                  position := "relative",
                  width := "64px",
                  height := "64px",
                  src := url,
                  zIndex := 1,
                  new CStyle("filter", "filter") := "grayscale(50%)",
                  cls := "rounded-lg"),
                img(
                  position := "relative",
                  width := "64px",
                  height := "64px",
                  top := "-64px",
                  src := "/slash_red_256.png",
                  zIndex := 2,
                  cls := "rounded-lg")))
          }.toList
        case None =>
          Range(0, 5).map { _ =>
            div(width := "64px", height := "64px", cls := "rounded-lg animate-pulse bg-gray-500 mx-1")
          }
      })
  }

  private def getRankedData(rd: List[RankedLeague]): Option[RankedLeague] = {
    rd.find(_.queueType == RankedQueueTypeEnum.SummonersRiftSoloRanked) match {
      case None => rd.find(_.queueType == RankedQueueTypeEnum.SummonersRiftFlexRanked)
      case o => o
    }
  }


  def roundWinrate(wr: Double): Double = Math.round(wr * 1000D) / 10D

  def winrateColor(wr: Double): String = if (wr < 0.5D) "#761616" else "#094523"

  private def renderPlayerCard(data: Signal[LoadState[(InGameSummoner, DData)]],
                               playsWith: Signal[LoadState[Option[Set[InGameSummoner]]]]
                              ): ReactiveHtmlElement[HTMLElement] = {

    // HELPERS

    def renderChampionIcon(championId: Long, size: String, clsAttrs: Option[String])
                          (implicit dd: DData) = {
      img(src := dd.championUrl(dd.championById(championId)),
          cls := clsAttrs.getOrElse(""), height := size, width := size)
    }
    def renderSummonerSpell(ss: SummonerSpellsEnum)(implicit dd: DData) = {
      img(src := dd.summonerUrlById(ss.value).getOrElse(""), height := "32px", width := "32px", minWidth := "32px",
          cls := "rounded-md")
    }

    def renderRune(rune: Option[Rune], iconSize: String)(implicit dd: DData) = {
      div(width := "32px", height := "32px",
          cls := "border border-gray-300 flex items-center justify-center rounded-md",
          img(src := rune.map(dd.runeUrl).getOrElse(""), width := iconSize, height := iconSize))
    }

    def renderWinrateText(rl: Option[RankedLeague]) = {
      val qText = rl match {
        case Some(v) if v.queueType == RankedQueueTypeEnum.SummonersRiftSoloRanked => {
          s"Solo/Duo WR: ${roundWinrate(v.winRate)}%"
        }
        case Some(v) => {
          s"Flex WR:  ${roundWinrate(v.winRate)}%"
        }
        case None => {
          "Unranked"
        }
      }
      List(
        span(cls := "text-center text-sm", qText),
        rl match {
          case Some(l) => span(cls := "text-center text-sm", s"(${l.wins + l.losses} Played)")
          case None => span()
        })
    }

    def renderWrBar(rl: Option[RankedLeague]): Option[ReactiveHtmlElement[html.Div]] = {
      rl.map { v =>
        div(
          width := "100px", height := "10px", border := s"1px solid ${winrateColor(v.winRate)}",
          div(height := "100%",
              width := s"${v.winRate * 100}%",
              backgroundColor :=
              winrateColor(v.winRate),
              borderRadius := "inherit"))
      }
    }

    def renderRankedIcon(rl: Option[RankedLeague]) = {
      def renderMiniSeries(miniSeries: MiniSeries) = {
        def renderTargetChar(c: Char) = {
          val color = c match {
            case 'W' => "#094523"
            case 'L' => "#761616"
            case _ => "#bbb"
          }
          div(width := "12px",
              height := "12px",
              borderRadius := "50%",
              backgroundColor := color,
              marginLeft := "1px",
              marginRight := "1px")
        }

        div(
          cls := "flex justify-center",
          width := "92px", height := "16px",
          miniSeries.progress.map(renderTargetChar))
      }

      div(
        cls := "flex flex-col items-center justify-center mx-1", width := "92px",
        rl match {
          case Some(l) => {
            val t = l.tier.entryName.toLowerCase.capitalize
            List(
              img(src := s"/Emblem_${t}.png", width := "40px"),
              l.tier match {
                case t if t.in(TierEnum.Master, TierEnum.Grandmaster, TierEnum.Challenger) => {
                  span(cls := "text-sm", s"${t}")
                }
                case _ => span(cls := "text-sm", s"${t} ${l.rank}")
              },
              span(cls := "text-sm", s"${l.leaguePoints} LP"),
              l.miniSeries.map(renderMiniSeries).getOrElse(div()))
          }
          case None => {
            List(
              img(src := "/Emblem_Unranked.png", width := "46px"),
              span("Unranked"))
          }
        })
    }

    def renderPlaysWith(pwData: LoadState[(Option[Set[InGameSummoner]], DData)]) = {
      div(
        width := "72px",
        minWidth := "72px",
        height := "72px",
        cls := "flex flex-wrap items-center justify-center mx-1",
        pwData match {
          case Some((Some(g), dd)) => {
            g.map { p =>
              div(renderChampionIcon(p.championId, "32px", None)(dd), cls := "mr-1")
            }.toList
          }
          case Some((None, _)) => {
            div(
              cls := "flex flex-grow flex-col border border-gray-300 rounded-lg items-center justify-center mr-1",
              minWidth := "72px",
              minHeight := "72px",
              span(cls := "font-semibold", "Plays"),
              span(cls := "font-semibold", "solo"))
          }
          case None => {
            Range(0, 4).map(
              _ => div(width := "32px", height := "32px", cls := "animate-pulse bg-gray-500 mr-1"))
          }
        })
    }

    // BODY

    val boxHeight  = "108px"
    val boxCls     = s"flex $paperCls mt-1 p-1 items-center justify-center"
    val sumRuneCls = "flex flex-col justify-around h-5/6 px-1"
    val textWidth  = "200px"

    val rankedData: Signal[LoadState[Option[RankedLeague]]] = data.map {
      case Some((p, _)) => getRankedData(p.rankedLeagues).some
      case _ => None
    }

    div(
      height := boxHeight,
      cls := boxCls,
      child <-- data.map {
        case Some((p, dd)) => renderChampionIcon(p.championId, "80px", "rounded-lg".some)(dd)
        case None => div(cls := "animate-pulse bg-gray-500 rounded-lg",
                         width := "80px",
                         height := "80px")
      },
      div(
        cls := sumRuneCls,
        children <-- data.map {
          case Some((p, dd)) =>
            List(
              renderSummonerSpell(p.summonerSpells.spell1Id)(dd),
              renderSummonerSpell(p.summonerSpells.spell2Id)(dd))
          case None => Range(0, 2).map(
            _ => div(width := "32px", height := "32px", cls := "animate-pulse bg-gray-500 rounded-md"))
        }),
      div(
        cls := sumRuneCls,
        children <-- data.map {
          case Some((p, dd)) =>
            List(
              renderRune(dd.keystoneById(p.runes.keystone), "32px")(dd),
              renderRune(dd.treeById(p.runes.secondaryPathId), "26px")(dd))
          case None => Range(0, 2).map(
            _ => div(width := "32px", height := "32px", cls := "animate-pulse bg-gray-500 rounded-md"))
        }),
      div(
        cls := "flex flex-col items-center justify-center",
        width := textWidth,
        child <-- data.map {
          case Some((p, _)) =>
            span(cls := "text-center text-xl max-w-full truncate overflow-ellipsis font-bold", p.name)
          case None => div(width := "120px", height := "14px", cls := "animate-pulse bg-gray-500")
        },
        child <-- data.map {
          case Some((p, dd)) =>
            dd.championById(p.championId).map(_.name).getOrElse[String]("Unknown")
          case None => div(width := "90px", height := "14px", cls := "animate-pulse bg-gray-500 mt-1")
        },
        children <-- rankedData.map {
          case Some(rd) => List(renderWrBar(rd)).flatten :++ renderWinrateText(rd)
          case None => List(div(width := "100px", height := "12px", cls := "animate-pulse bg-gray-500 mt-3"),
                            div(width := "60px", height := "14px", cls := "animate-pulse bg-gray-500 mt-1"))
        }),
      child <-- rankedData.map {
        case Some(rd) => renderRankedIcon(rd)
        case None =>
          div(cls := "flex flex-col items-center",
              div(width := "46px", height := "60px", cls := "animate-pulse bg-gray-500"),
              div(width := "46px", height := "14px", cls := "animate-pulse bg-gray-500 mt-1"))
      },
      child <-- data.map(_.map(_._2))
        .combineWith(playsWith)
        .map(_.mapN((a, b) => (b, a))).map(renderPlaysWith))
  }
}
