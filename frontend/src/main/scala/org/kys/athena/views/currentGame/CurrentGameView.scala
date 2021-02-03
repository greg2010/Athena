package org.kys.athena.views.currentGame

import com.raquo.domtypes.generic.keys.{Style => CStyle}
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.kys.athena.components.{ChampionIcon, ImgSized, UggLink}
import org.kys.athena.http.Client._
import org.kys.athena.http.DData
import org.kys.athena.http.errors.{BackendApiError, InternalServerError, NotFoundError}
import org.kys.athena.http.models.current._
import org.kys.athena.http.models.premade.{PlayerGroup, PremadeResponse}
import org.kys.athena.pages.CurrentGamePage
import org.kys.athena.riot.api.dto.common.{GameQueueTypeEnum, Platform, SummonerSpellsEnum}
import org.kys.athena.riot.api.dto.currentgameinfo.BannedChampion
import org.kys.athena.riot.api.dto.ddragon.runes.Rune
import org.kys.athena.riot.api.dto.league.{MiniSeries, RankedQueueTypeEnum, TierEnum}
import org.kys.athena.util.{Config, Time}
import org.kys.athena.views.View
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.HTMLElement
import zio._

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt


object CurrentGameView extends View[CurrentGamePage] {


  // FETCH LOGIC
  val debug = Config.USE_FAKE_DATA match {
    case "true" => true
    case _ => false
  }

  def fetchAndWriteDDragon(ddObs: Observer[DataState[DData]]): IO[BackendApiError, Unit] = {
    for {
      dd <- ZIO.tupledPar(fetchCachedDDragonChampion(), fetchCachedDDragonRunes(), fetchCachedDDragonSummoners())
        .either.map {
        case Left(ex) => Failed(ex)
        case Right((c, r, s)) => Ready(DData(c, r, s))
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
      o <- fetchOngoingGameByName(platform, name)(debug)
        .map(r => Ready(r))
        .catchAll(err => UIO.succeed(Failed(err)))
      _ <- UIO.effectTotal(ongoingObs.onNext(o))

      gr <- (o match {
        case Failed(_) => IO.fail(InternalServerError("Fetch for players failed, not fetching groups"))
        case _ => {
          o.map(_.groupUuid).toOption.flatten
            .fold(fetchGroupsByName(platform, name)(debug))(uuid => fetchGroupsByUUID(uuid)(debug))
        }
      }).map(r => Ready(r))
        .catchAll(err => UIO.succeed(Failed(err)))
      _ <- UIO.effectTotal(groupsObs.onNext(gr))
    } yield ()
  }

  def fetchAndWriteAll(platform: Platform,
                       name: String,
                       ddObs: Observer[DataState[DData]],
                       ongoingObs: Observer[DataState[OngoingGameResponse]],
                       groupsObs: Observer[DataState[PremadeResponse]]): IO[BackendApiError, Unit] = {
    ZIO.tupledPar(fetchAndWriteDDragon(ddObs), fetchAndWriteGameInfo(platform, name, ongoingObs, groupsObs)).unit
  }

  val runtime: Runtime[zio.ZEnv] = Runtime.default
  // RENDER LOGIC

  val paperCls = "bg-white border border-gray-300 shadow-lg rounded-lg"

  override def render(p: CurrentGamePage): HtmlElement = {

    lazy val ddVar      = Var[DataState[DData]](Loading)
    lazy val ongoingVar = Var[DataState[OngoingGameResponse]](Loading)
    lazy val groupsVar  = Var[DataState[PremadeResponse]](Loading)

    val playerNameSignal: Signal[String] = ongoingVar.signal.map {
      case Ready(v) => v.querySummonerName
      case _ => p.name
    }

    def bodySignal = {
      ongoingVar.signal.combineWith(ddVar.signal).map {
        case (Failed(_: NotFoundError), _) => List(renderNotFound(p, ongoingVar.writer, groupsVar.writer))
        case (Failed(err), _) =>
          scribe.error(s"huh? msg=${err.message}", err)
          List(renderError(p, ddVar.writer, ongoingVar.writer, groupsVar.writer))
        case (_, Failed(_)) => List(renderError(p, ddVar.writer, ongoingVar.writer, groupsVar.writer))
        case (a: Infallible[OngoingGameResponse], b: Infallible[DData]) =>
          renderNoError(ongoingVar.signal.map(_ => a),
                        groupsVar.signal, ddVar.signal.map(_ => b), playerNameSignal, p)
      }
    }
    div(
      onMountCallback { _ =>
        runtime.unsafeRunAsync_(fetchAndWriteAll(p.realm, p.name, ddVar.writer, ongoingVar.writer, groupsVar.writer))
      },
      cls := "flex flex-col items-center container-md flex-grow justify-center p-4 " +
             s"$paperCls my-10 bg-opacity-50",
      children <-- bodySignal)
  }

  private def renderNotFound(p: CurrentGamePage,
                             ongoingObs: Observer[DataState[OngoingGameResponse]],
                             groupsObs: Observer[DataState[PremadeResponse]]) = {
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
          onNext = _ => runtime.unsafeRunAsync_(fetchAndWriteGameInfo(p.realm, p.name, ongoingObs, groupsObs)))))
  }

  private def renderError(p: CurrentGamePage,
                          ddObs: Observer[DataState[DData]],
                          ongoingObs: Observer[DataState[OngoingGameResponse]],
                          groupsObs: Observer[DataState[PremadeResponse]]) = {
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
          onNext = _ => runtime.unsafeRunAsync_(fetchAndWriteAll(p.realm, p.name, ddObs, ongoingObs, groupsObs)))))
  }

  private def renderNoError(gameES: Signal[Infallible[OngoingGameResponse]],
                            premadeES: Signal[DataState[PremadeResponse]],
                            ddES: Signal[Infallible[DData]],
                            playerNameSignal: Signal[String], p: CurrentGamePage) = {

    List(
      renderHeader(gameES, playerNameSignal),
      div(
        cls := "flex flex-col lg:flex-row",
        renderTeam(gameES.map(_.map(_.blueTeam)), premadeES.map(_.map(_.blue)), ddES, "Blue",p.realm),
        renderTeam(gameES.map(_.map(_.redTeam)), premadeES.map(_.map(_.red)), ddES, "Red",p.realm)))
  }

  private def renderHeader(ongoingES: Signal[Infallible[OngoingGameResponse]], playerName: Signal[String]) = {
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
          case Ready(g) =>
            val startTime = if (g.gameStartTime > 0) g.gameStartTime else {
              System.currentTimeMillis() + 3.minutes.toMillis
            }
            val timeES    = EventStream
              .periodic(1.seconds.toMillis.toInt)
              .map(_ => Time.renderMsInterval(System.currentTimeMillis() - startTime))
            span(s"${renderQueueName(g.gameQueueId)} | ", child.text <-- timeES, cls := "mt-4 text-md")
          case Loading => span(height := "14px", width := "300px", cls := "animate-pulse bg-gray-500 mt-4")
        })
  }

  private def renderTeam(teamES: Signal[Infallible[OngoingGameTeam]],
                         groupsES: Signal[DataState[Set[PlayerGroup]]],
                         ddES: Signal[Infallible[DData]],
                         color: String, platform: Platform) = {

    @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
    def transitiveJoinSets[T](s: Set[Set[T]]): Set[Set[T]] = {
      @tailrec
      def mergeSets(cum: Set[Set[T]], sets: Set[Set[T]]): Set[Set[T]] = {
        import scala.language.postfixOps
        if (sets.isEmpty) {
          cum
        } else {
          val cur               = sets.head
          val (hasCommon, rest) = cum.partition(_ & cur nonEmpty)
          mergeSets(rest + (cur ++ hasCommon.flatten), sets.tail)
        }
      }
      mergeSets(Set.empty[Set[T]], s)
    }

    def findGroupBySummoner(g: Set[Set[InGameSummoner]], s: InGameSummoner): Option[Set[InGameSummoner]] = {
      g.find(_.contains(s)).map(_.filterNot(_.summonerId == s.summonerId))
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
    val expandedGroupsES = teamES.combineWith(groupsES).map(r => r._1.zip(r._2)).map { e =>
      e.map {
        case (t, g) => {
          val sums = g.map(_.summoners.flatMap(s => t.summoners.find(_.summonerId == s)))
          transitiveJoinSets(sums)
        }
      }
    }

    // Convert to signal with `None` guard value, to render loading state
    val maybeSummoners: Signal[Seq[(Infallible[(InGameSummoner, DData)], Int)]] = sortedSummonersES
      .combineWith(ddES).map(r => r._1.zip(r._2)).map {
      case Ready((l, d)) => l.map(si => (Ready((si._1, d)), si._2))
      case _ => Range(0, 5).map(i => (Loading, i))
    }

    // Find groups signal for each summoner and pass it to render function. Maps to list of render signals
    val renderSignal = maybeSummoners
      .split(_._2) {
        case (_, _, d) =>
          val ss = d.map(_._1)
          val gs = ss
            .combineWith(expandedGroupsES)
            .map(r => {
              r._2.zip(r._1).map {
                case (sset, (sum, _)) => findGroupBySummoner(sset, sum)
              }
            })
          renderPlayerCard(ss, gs, platform)
      }

    val bES = teamES.combineWith(ddES).map(r => r._1.zip(r._2)).map { e =>
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

  private def renderTeamHeader(team: Signal[Infallible[OngoingGameTeam]], teamName: String) = {
    case class WinrateSummary(average: Double, range: Double)

    val summary      = team.map { e =>
      e.map { t =>
        val winrates = t.summoners.toList.flatMap(s => getRankedData(s.rankedLeagues)).map(_.winRate).sortBy(identity)
        winrates match {
          case ::(head, tail) => {
            val avg = winrates.sum / winrates.length
            Some(WinrateSummary(avg, tail.lastOption.map(_ - head).getOrElse(0D)))
          }
          case Nil => None
        }
      }
    }
    val teamNameElem = span(cls := "text-3xl font-medium tracking-wider", s"${teamName} Team")
    div(
      cls := s"flex justify-around items-center p-4 mb-4 w-full $paperCls",
      children <-- summary.map {
        case Ready(Some(s)) =>
          List(
            div(cls := "flex flex-col items-center", span("Average Winrate"),
                span(color := winrateColor(s.average), s"${roundWinrate(s.average)}%")),
            teamNameElem,
            div(cls := "flex flex-col items-center", span("Winrate Range"), span(s"${roundWinrate(s.range)}%")))
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

  private def renderBans(bansES: Signal[Infallible[(Option[Set[BannedChampion]], DData)]])
  : ReactiveHtmlElement[html.Div] = {
    div(
      cls := s"flex inline-flex py-1 mt-2 $paperCls",
      children <-- bansES.map {
        case Ready((None, _)) =>
          inContext { ctx: ReactiveHtmlElement[html.Div] =>
            ctx.amend(cls := "hidden")
          }
          List()
        case Ready((Some(banned), dd)) =>
          banned.map { ch =>
            val url = dd.championUrl(dd.championById(ch.championId))
            val bannedChamp = div(
              width := "64px",
              height := "64px",
              cls := "mx-1",
              div(
                position := "absolute",
                ChampionIcon.render(ch.championId, 64, dd).amend(
                  position := "relative",
                  zIndex := 1,
                  new CStyle("filter", "filter") := "grayscale(50%)",
                  cls := "rounded-lg"
                  ),
                ImgSized(s"${Config.FRONTEND_URL}/slash_red_256.png", 64, Some(64)).amend(
                  position := "relative",
                  top := "-64px",
                  zIndex := 2,
                  cls := "rounded-lg"
                  )))
            UggLink(ch.championId, bannedChamp, dd)
          }.toList
        case Loading =>
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

  private def renderPlayerCard(data: Signal[Infallible[(InGameSummoner, DData)]],
                               playsWith: Signal[DataState[Option[Set[InGameSummoner]]]],
                               platform:Platform
                              ): ReactiveHtmlElement[HTMLElement] = {
    /*
    val summonerName = dd.


    val opggSummonerURL = summonerName match {
      case Some(value) => "https://${platform}.op.gg/summoner/userName=${p.name}"
      case None => ""
    }
    */


    // HELPERS


    def renderSummonerSpell(ss: SummonerSpellsEnum)(implicit dd: DData) = {
      val url = dd.summonerUrlById(ss.value).getOrElse("")
      ImgSized(url, 32, Some(32)).amend(
        minWidth := "32px", cls := "rounded-md")
    }

    def renderRune(rune: Option[Rune], iconSize: Int)(implicit dd: DData) = {
      div(width := "32px", height := "32px",
          cls := "border border-gray-300 flex items-center justify-center rounded-md",
          ImgSized(rune.map(dd.runeUrl).getOrElse(""), 32, Some(32)))
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
        cls := "flex flex-col items-center justify-center", width := "86px",
        rl match {
          case Some(l) => {
            val t   = l.tier.entryName.toLowerCase.capitalize
            val url = s"${Config.FRONTEND_URL}/Emblem_${t}.png"
            List(
              ImgSized(url, 40, None),
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
            val url = s"${Config.FRONTEND_URL}/Emblem_Unranked.png"
            List(
              ImgSized(url, 46, None),
              span("Unranked"))
          }
        })

    }

    // BODY

    val boxHeight = "108px"
    val boxCls    = s"flex $paperCls mt-1 p-1 items-center justify-center"
    val sumCls    = "flex flex-col justify-around h-5/6 px-1"
    val runeCls   = "flex flex-col justify-around h-5/6"
    val textWidth = "180px"

    val rankedData: Signal[Infallible[Option[RankedLeague]]] = data.map {
      case Ready((p, _)) => Ready(getRankedData(p.rankedLeagues))
      case _ => Loading
    }

    div(
      height := boxHeight,
      cls := boxCls,
      child <-- data.map {
       // case Ready((p, dd)) => renderChampionIcon(p.championId, 80, Some("rounded-lg ml-1"))(dd)

        case Ready((p, dd)) =>
          UggLink(p.championId,ChampionIcon.render(p.championId, 80, dd).amend(cls :="rounded-lg ml-1"), dd)
        case Loading => div(cls := "animate-pulse bg-gray-500 rounded-lg ml-1",
                            width := "80px",
                            height := "80px")
      },
      div(
        cls := sumCls,
        children <-- data.map {
          case Ready((p, dd)) =>
            List(
              renderSummonerSpell(p.summonerSpells.spell1Id)(dd),
              renderSummonerSpell(p.summonerSpells.spell2Id)(dd))
          case Loading => Range(0, 2).map(
            _ => div(width := "32px", height := "32px", cls := "animate-pulse bg-gray-500 rounded-md"))
        }),
      div(
        cls := runeCls,
        children <-- data.map {
          case Ready((p, dd)) =>
            p.runes match {
              case Some(r) => {
                List(
                  renderRune(dd.keystoneById(r.keystone), 32)(dd),
                  renderRune(dd.treeById(r.secondaryPathId), 26)(dd))
              }
              case None => List() // If riot api for some reason did not return runes, do not render
            }
          case Loading => Range(0, 2).map(
            _ => div(width := "32px", height := "32px", cls := "animate-pulse bg-gray-500 rounded-md"))
        }),
      div(
        cls := "flex flex-col items-center justify-center",
        width := textWidth,
        child <-- data.map {
          case Ready((p, _)) =>
            //span(cls := "text-center text-xl max-w-full truncate overflow-ellipsis font-medium", p.name)
            //<a href="https://REGION.op.gg/summoner/userName=p.name">p.name</a> triggers0 was here
            a(cls := "text-center text-xl max-w-full truncate overflow-ellipsis font-medium",
              href := s"https://${platform}.op.gg/summoner/userName=${p.name}", target :="_blank", p.name)


          case Loading => div(width := "120px", height := "14px", cls := "animate-pulse bg-gray-500")
        },
        child <-- data.map {
          case Ready((p, dd)) =>
            span(dd.championById(p.championId).map(_.name).getOrElse[String]("Unknown"), cls := "font-normal")
          case Loading => div(width := "90px", height := "14px", cls := "animate-pulse bg-gray-500 mt-1")
        },
        children <-- rankedData.map {
          case Ready(rd) => List(renderWrBar(rd)).flatten :++ renderWinrateText(rd)
          case Loading => List(div(width := "100px", height := "12px", cls := "animate-pulse bg-gray-500 mt-3"),
                               div(width := "60px", height := "14px", cls := "animate-pulse bg-gray-500 mt-1"))
        }),
      child <-- rankedData.map {
        case Ready(rd) => renderRankedIcon(rd)
        case Loading =>
          div(cls := "flex flex-col items-center", width := "86px",
              div(width := "46px", height := "60px", cls := "animate-pulse bg-gray-500"),
              div(width := "46px", height := "14px", cls := "animate-pulse bg-gray-500 mt-1"))
      }
      )
  }
}
