package org.kys.athena.http.models

import org.kys.athena.api.dto.currentgameinfo.CurrentGameParticipant
import org.kys.athena.api.dto.summoner.Summoner


package object extensions {

  implicit class RichInGameSummoner(val inGameSummoner: InGameSummoner.type) extends AnyVal {

    def apply(summoner: Summoner, currentGameParticipant: CurrentGameParticipant): InGameSummoner = {
      val runes          = RunesSelected(primaryPathId = currentGameParticipant.perks.perkStyle,
                                         secondaryPathId = currentGameParticipant.perks.perkSubStyle,
                                         runeIds = currentGameParticipant.perks.perkIds)
      val summonerSpells = SummonerSpells(spell1Id = currentGameParticipant.spell1Id,
                                          spell2Id = currentGameParticipant.spell2Id)
      InGameSummoner(name = currentGameParticipant.summonerName, summonerId = summoner.id,
                     summonerLevel = summoner.summonerLevel, championId = currentGameParticipant.championId,
                     runes = runes, summonerSpells = summonerSpells, teamId = currentGameParticipant.teamId)
    }
  }

}
