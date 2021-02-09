package org.kys.athena.components.ongoing

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.kys.athena.http.backend.BackendDataHelpers
import org.kys.athena.http.dd.CombinedDD
import org.kys.athena.http.models.current.{InGameSummoner, RankedLeague}
import org.kys.athena.components.common
import org.kys.athena.components.common.{ChampionIcon, ImgSized, OpggLink, UggLink}
import org.kys.athena.riot.api.dto.common.{Platform, SummonerSpellsEnum}
import org.kys.athena.riot.api.dto.ddragon.runes.Rune
import org.kys.athena.riot.api.dto.league.{MiniSeries, RankedQueueTypeEnum, TierEnum}
import org.kys.athena.util.{Config, Infallible, Loading, Ready}
import org.scalajs.dom.html

object OngoingPlayerCard {

  private def renderSummonerSpell(ss: SummonerSpellsEnum)(implicit dd: CombinedDD) = {
    val url = dd.summonerUrlById(ss.value).getOrElse("")
    ImgSized(url, 32, Some(32), minWidth := "32px", cls := "rounded-md")
  }

  private def renderRune(rune: Option[Rune], iconSize: Int)(implicit dd: CombinedDD) = {
    div(width := "32px", height := "32px",
        cls := "border border-gray-300 flex items-center justify-center rounded-md",
        ImgSized(rune.map(dd.runeUrl).getOrElse(""), iconSize, Some(iconSize)))
  }

  private def renderWinrateText(rl: Option[RankedLeague]) = {
    val commonCls   = "text-center leading-tight mt-1"
    val rankedCls   = commonCls + " text-xs"
    val unrankedCls = commonCls + " text-base"

    val qSpan = rl match {
      case Some(v) if v.queueType == RankedQueueTypeEnum.SummonersRiftSoloRanked => {
        span(cls := rankedCls, s"Solo/Duo WR: ${BackendDataHelpers.roundWinrate(v.winRate)}%")
      }
      case Some(v) => {
        span(cls := rankedCls, s"Flex WR:  ${BackendDataHelpers.roundWinrate(v.winRate)}%")
      }
      case None => {
        span(cls := unrankedCls, "Unranked")

      }
    }
    List(
      qSpan,
      rl match {
        case Some(l) => span(cls := "text-center text-xs leading-tight", s"(${l.wins + l.losses} Played)")
        case None => span()
      })
  }

  private def renderWrBar(rl: Option[RankedLeague]): Option[ReactiveHtmlElement[html.Div]] = {
    rl.map { v =>
      div(
        width := "100px", height := "10px", border := s"1px solid ${BackendDataHelpers.winrateColor(v.winRate)}",
        div(height := "100%",
            width := s"${v.winRate * 100}%",
            backgroundColor :=
            BackendDataHelpers.winrateColor(v.winRate),
            borderRadius := "inherit"))
    }
  }

  private def renderRankedIcon(rl: Option[RankedLeague]) = {
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
      cls := "flex flex-col items-center justify-center mr-1", width := "86px",
      rl match {
        case Some(l) => {
          val t   = l.tier.entryName.toLowerCase.capitalize
          val url = s"${Config.FRONTEND_URL}/Emblem_${t}.png"
          List(
            ImgSized(url, 40, None),
            l.tier match {
              case t if t.in(TierEnum.Master, TierEnum.Grandmaster, TierEnum.Challenger) => {
                span(cls := "text-xs leading-tight mt-1", s"${t}")
              }
              case _ => span(cls := "text-xs leading-tight mt-1", s"${t} ${l.rank}")
            },
            span(cls := "text-xs leading-tight", s"${l.leaguePoints} LP"),
            l.miniSeries.map(renderMiniSeries).getOrElse(div()))
        }
        case None => {
          val url = s"${Config.FRONTEND_URL}/Emblem_Unranked.png"
          List(
            ImgSized(url, 46, None),
            span(cls := "text-sm leading-tight mt-1", "Unranked"))
        }
      })
  }

  def apply(data: Signal[Infallible[(InGameSummoner, CombinedDD)]],
            platform: Platform,
            mods: Modifier[HtmlElement]*): ReactiveHtmlElement[html.Div] = {
    val boxHeight = "104px"
    val boxCls    = s"flex items-center justify-center"
    val sumCls    = "flex flex-col justify-around h-5/6 mx-1"
    val runeCls   = "flex flex-col justify-around h-5/6 mr-1"
    val textWidth = "140px"

    val rankedData: Signal[Infallible[Option[RankedLeague]]] = data.map {
      case Ready((p, _)) => Ready(BackendDataHelpers.relevantRankedLeague(p.rankedLeagues))
      case _ => Loading
    }

    div(
      height := boxHeight,
      cls := boxCls,
      child <-- data.map {
        case Ready((p, dd)) =>
          val champImg = div(
            width := "80px",
            height := "80px",
            cls := "ml-1",
            div(
              position := "absolute",
              height := "80px",
              div(ChampionIcon(p.championId, 80, dd, cls := "rounded-lg", zIndex := 1)),
              div(
                position := "relative",
                top := "-30px",
                left := "50px",
                zIndex := 2,
                cls := "rounded-lg flex items-center justify-center",
                width := "26px",
                height := "26px",
                backgroundColor := "rgba(0, 0, 0, 0.7)",
                span(
                  cls := "text-center text-xs",
                  color := "white",
                  p.summonerLevel.toString))))


          UggLink(p.championId, dd, champImg)
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
            val nameCls = "text-center text-lg leading-tight max-w-full truncate overflow-ellipsis font-medium"
            OpggLink(p.name, platform, span(p.name, cls := nameCls))

          case Loading => div(width := "120px", height := "14px", cls := "animate-pulse bg-gray-500")
        },
        child <-- data.map {
          case Ready((p, dd)) =>
            common.UggLink(p.championId,
                           dd,
                           dd.championById(p.championId).map(_.name).getOrElse[String]("Unknown"),
                           cls := "font-normal leading-tight mb-1")
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
      }, mods)
  }
}
