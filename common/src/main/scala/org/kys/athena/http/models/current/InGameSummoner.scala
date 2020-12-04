package org.kys.athena.http.models.current

import org.kys.athena.http.models.current
import org.kys.athena.riot.api.dto.currentgameinfo.CurrentGameParticipant
import org.kys.athena.riot.api.dto.league.League
import org.kys.athena.riot.api.dto.summoner.Summoner


final case class InGameSummoner(name: String,
                                summonerId: String,
                                summonerLevel: Long,
                                championId: Long,
                                runes: RunesSelected,
                                summonerSpells: SummonerSpells,
                                teamId: Long,
                                rankedLeagues: List[RankedLeague]) {
  override def equals(obj: Any): Boolean = {
    obj match {
      case s: InGameSummoner => s.summonerId == summonerId
      case _ => false
    }
  }
}

object InGameSummoner {
  def apply(summoner: Summoner,
            currentGameParticipant: CurrentGameParticipant,
            rankedLeagues: List[League]): InGameSummoner = {
    val runes          = RunesSelected(primaryPathId = currentGameParticipant.perks.perkStyle,
                                       secondaryPathId = currentGameParticipant.perks.perkSubStyle,
                                       keystone = currentGameParticipant.perks.perkIds.head,
                                       runeIds = currentGameParticipant.perks.perkIds)
    val summonerSpells = current.SummonerSpells(spell1Id = currentGameParticipant.spell1Id,
                                                spell2Id = currentGameParticipant.spell2Id)
    InGameSummoner(name = currentGameParticipant.summonerName,
                   summonerId = summoner.id,
                   summonerLevel = summoner.summonerLevel,
                   championId = currentGameParticipant.championId,
                   runes = runes, summonerSpells = summonerSpells,
                   teamId = currentGameParticipant.teamId,
                   rankedLeagues = rankedLeagues.map(l => RankedLeague(l)))
  }
}