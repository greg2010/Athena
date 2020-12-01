package org.kys.athena.views


import cats.effect.IO
import com.raquo.laminar.api.L._
import org.kys.athena.http.Client._
import org.kys.athena.pages.CurrentGamePage
import org.kys.athena.riot.api.dto.ddragon.champions.{ChampionEntry, Champions}
import org.kys.athena.riot.api.dto.ddragon.runes.{Rune, RuneTree, RunesEntry}
import org.kys.athena.riot.api.dto.ddragon.summonerspells.{SummonerSpells, SummonerSpellsEntry}
import cats.implicits._
import com.raquo.airstream._
import org.kys.athena.http.models.current.{InGameSummoner, OngoingGameResponse, OngoingGameTeam, PositionEnum, RankedLeague}
import org.kys.athena.http.models.premade.PremadeResponse
import org.kys.athena.riot.api.dto.common.{Platform, SummonerSpellsEnum}
import org.kys.athena.riot.api.dto.currentgameinfo.BannedChampion
import org.kys.athena.riot.api.dto.league.{MiniSeries, RankedQueueTypeEnum}
import org.kys.athena.util.Config
import org.kys.athena.util.IOToAS.writeIOToVar



object CurrentGameView extends View[CurrentGamePage] {
  private case class DData(c: Champions, r: List[RuneTree], s: SummonerSpells) {
    private lazy val flatRunes: List[RunesEntry] = r.flatMap(t => t.slots.flatMap(_.runes))

    def championById(id: Long): Option[ChampionEntry] = c.data.values.find(_.key == id)
    def summonerById(id: Int): Option[SummonerSpellsEntry] = s.data.values.find(_.key == id)
    def treeById(id: Long): Option[RuneTree] = r.find(_.id == id)
    def keystoneById(id: Long): Option[RunesEntry] = flatRunes.find(_.id == id)

    def championUrl(champion: ChampionEntry): String =
      s"${Config.DDRAGON_BASE_URL}${Config.DDRAGON_VERSION}/img/champion/${champion.id}.png"

    def summonerUrl(summonerSpellsEntry: SummonerSpellsEntry): String =
      s"${Config.DDRAGON_BASE_URL}${Config.DDRAGON_VERSION}/img/spell/${summonerSpellsEntry.id}.png"

    def summonerUrlById(id: Int): Option[String] = summonerById(id).map(summonerUrl)

    def runeUrl(rune: Rune) = s"${Config.DDRAGON_BASE_URL}img/${rune.icon}"

  }

  // FETCH LOGIC

  val debug = true

  private def fetchAndWriteDDragon(ddVar: Var[Option[DData]]): IO[Unit] = {
    val dd = (fetchCachedDDragonChampion(), fetchCachedDDragonRunes(), fetchCachedDDragonSummoners()).parMapN(DData)
    writeIOToVar(dd, ddVar)
  }

  private def fetchAndWriteGameInfo(ongoingVar: Var[Option[OngoingGameResponse]],
                            groupsVar: Var[Option[PremadeResponse]])
                           (platform: Platform, name: String): IO[Unit] = {
    for {
      g <- fetchOngoingGameByName(platform, name)(debug)
      _ <- writeIOToVar(IO.pure(g), ongoingVar)
      _ <- g.groupUuid match {
        case Some(u) => writeIOToVar(fetchGroupsByUUID(u)(debug), groupsVar)
        case None =>
          scribe.warn("Server did not return a UUID, querying groups manually")
          writeIOToVar(fetchGroupsByName(platform, name)(debug), groupsVar)
      }
    } yield ()
  }

  private def fetchAndWriteAll(ongoingVar: Var[Option[OngoingGameResponse]],
                               groupsVar: Var[Option[PremadeResponse]],
                               ddVar: Var[Option[DData]])
                              (platform: Platform, name: String): IO[Unit] = {
    (fetchAndWriteDDragon(ddVar), fetchAndWriteGameInfo(ongoingVar, groupsVar)(platform, name)).parTupled.map(_ => ())
  }

  // RENDER LOGIC

  val paperCls = "bg-white border border-gray-300 shadow-lg rounded-md"

  override def render(p: CurrentGamePage): HtmlElement = {
    val ddVar: Var[Option[DData]] = Var[Option[DData]](None)
    val ongoingVar: Var[Option[OngoingGameResponse]] = Var[Option[OngoingGameResponse]](None)
    val groupsVar: Var[Option[PremadeResponse]] = Var[Option[PremadeResponse]](None)
    val playerName = ongoingVar.signal.map(_.map(_.querySummonerName).getOrElse(p.name))

    div(onMountCallback(_ => fetchAndWriteAll(ongoingVar, groupsVar, ddVar)(p.realm, p.name).unsafeRunAsyncAndForget()),
        cls := "flex flex-col items-center container-md flex-grow justify-center p-4 " +
               s"$paperCls my-10 shadow-lg rounded-md",
        renderHeader(playerName),
        div(
          cls := "flex flex-col md:flex-row",
          renderTeam(ongoingVar.signal.map(_.map(_.blueTeam)), ddVar.signal, "Blue"),
          renderTeam(ongoingVar.signal.map(_.map(_.redTeam)), ddVar.signal, "Red")
        ))
  }

  def renderHeader(playerName: Signal[String]) = {
    div(cls := s"flex items-center $paperCls p-4 m-4",
        child <-- playerName.map(n => span(cls:= "text-center", s"Live game of $n", cls := "text-5xl")))
  }

  def renderTeam(team: Signal[Option[OngoingGameTeam]], ddSignal: Signal[Option[DData]], color: String) = {

    val sortedSummonersSignal = team.map {
      case Some(t) =>
        t.positions match {
            // No positions, generate random indices
          case None => t.summoners.map(_.some).zipWithIndex.toList.sortBy(_._2)
          case Some(posns) =>
            // Index players by positions
            val maybeIndexed = t.summoners
              .map(s => (s, posns.find(_._2 == s.summonerId).map(p => p._1.toOrder).getOrElse(9)))
              .toList.sortBy(_._2)
            // Make sure indices are unique for entires without position
            maybeIndexed.zipWithIndex.map {
              case ((s, 5), newi) => (s.some, newi)
              case ((s, oi), _) => (s.some, oi)
            }
        }
      case None => Range(0,5).map[(Option[InGameSummoner], Int)](i => (None, i)).toList
    }

    // Split signals by player and combine with ddata
    val playerSignals = sortedSummonersSignal
      .split(_._2)((_, _, v) => v.map(_._1).combineWith(ddSignal).map(_.mapN((a,b) => (a,b))))

    div(
      cls := "flex flex-col items-center justify-center mx-4",
      renderTeamHeader(team, color),
      div(
        children <-- playerSignals.map(_.map(renderPlayerCard))
      )
    )
  }

  def renderTeamHeader(team: Signal[Option[OngoingGameTeam]], color: String) = {
    div(
      cls := s"flex justify-around items-center p-4 w-full $paperCls",
      span(
        cls := "text-3xl",
        s"${color} Team"))
  }

  def renderBans(bans: Option[Set[BannedChampion]]) = {

  }

  def getRankedData(rd: List[RankedLeague]): Option[RankedLeague] = {
    rd.find(_.queueType == RankedQueueTypeEnum.SummonersRiftSoloRanked) match {
      case None => rd.find(_.queueType == RankedQueueTypeEnum.SummonersRiftFlexRanked)
      case o => o
    }
  }

  def renderPlayerCard(data: Signal[Option[(InGameSummoner, DData)]]) = {

    // Render helpers
    def renderChampionIcon(champion: Option[ChampionEntry], size: String)(implicit dd: DData)= {
      img(src := champion.map(dd.championUrl).getOrElse("/placeholder_champion.png"),
          cls := "rounded-lg", height := size, width := size)
    }
    def renderSummonerSpell(ss: SummonerSpellsEnum)(implicit dd: DData) = {
      img(src := dd.summonerUrlById(ss.value).getOrElse(""), height := "32px", width := "32px", minWidth := "32px", cls := "rounded-md")
    }

    def renderRune(rune: Option[Rune], iconSize: String)(implicit dd: DData) = {
      div(width := "32px", height := "32px", cls := "border border-gray-300 flex items-center justify-center rounded-md",
          img(src := rune.map(dd.runeUrl).getOrElse(""), width := iconSize, height := iconSize))
    }


    def wr(rl: RankedLeague): Double = rl.wins.toDouble / (rl.wins + rl.losses).toDouble

    def renderWinrateText(rl: Option[RankedLeague]) = {
      def roundWr(wr: Double): Double = Math.round(wr * 1000D) / 10D
      val qText = rl match {
        case Some(v) if v.queueType == RankedQueueTypeEnum.SummonersRiftSoloRanked =>
          s"Solo/Duo WR: ${roundWr(wr(v))}%"
        case Some(v) =>
          s"Flex WR:  ${roundWr(wr(v))}%"
        case None =>
          "Unranked"
      }
      List(
        span(cls := "text-center text-sm", qText),
        rl match {
          case Some(l) => span(cls := "text-center text-sm", s"(${l.wins + l.losses} Played)")
          case None => span()
        }
      )
    }

    def renderWrBar(rl: Option[RankedLeague]) = {
      rl.map { v =>
        val w = wr(v)
        val color = if (w < 0.5D) "#761616" else "#094523"
        div(
          width := "100px", height := "10px", border := s"1px solid $color",
          div(height := "100%", width := s"${w * 100}%", backgroundColor := color, borderRadius := "inherit"))
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
        cls := "flex flex-col items-center justify-center", width := "92px",
        rl match {
          case Some(l) =>
            val t = l.tier.toLowerCase.capitalize
            List(
              img(src := s"/Emblem_${t}.png", width := "40px"),
              span(cls := "text-sm", s"${t} ${l.rank}"),
              span(cls := "text-sm", s"${l.leaguePoints} LP"),
              l.miniSeries.map(renderMiniSeries).getOrElse(div()))
          case None =>
            List(
              img(src := "/Emblem_Unranked.png", width := "46px"),
              span("Unranked")
            )
        }
      )
    }

    def renderPlaysWith(implicit DData: DData) = {
      div(
        width := "72px",
        minWidth := "72px",
        height := "72px",
        cls := "flex flex-wrap items-center justify-center",
        div(renderChampionIcon(None, "32px"), cls := "mr-1"),
        div(renderChampionIcon(None, "32px"), cls := "mr-1"),
        div(renderChampionIcon(None, "32px"), cls := "mr-1"),
        div(renderChampionIcon(None, "32px"), cls := "mr-1")
      )
    }

    // Render body

    div(
      child <-- data.map {
        case Some((p, d)) =>
          implicit val dd: DData = d
          val ch = dd.championById(p.championId)
          val rankedData = getRankedData(p.rankedLeagues)
          div(
            height := "108px",
            cls := s"flex $paperCls my-1 p-1 items-center justify-around",
            renderChampionIcon(ch, "80px"),
            div(
              cls := "flex flex-col justify-around h-5/6 px-1",
              renderSummonerSpell(p.summonerSpells.spell1Id),
              renderSummonerSpell(p.summonerSpells.spell2Id)),
            div(
              cls := "flex flex-col justify-around h-5/6 px-1",
              renderRune(dd.keystoneById(p.runes.keystone), "32px"),
              renderRune(dd.treeById(p.runes.secondaryPathId), "26px")),
            div(
              cls := "flex flex-col items-center justify-center",
              span(cls := "text-center text-xl max-w-full truncate overflow-ellipsis font-bold", p.name),
              width := "200px",
              span(
                cls := "text-center",
                ch.map(_.name).getOrElse[String]("Unknown")
              ),
              renderWrBar(rankedData),
              renderWinrateText(rankedData)),
            renderRankedIcon(rankedData),
            renderPlaysWith)
        case None => span("I'm loading")
      }
    )
  }
}
