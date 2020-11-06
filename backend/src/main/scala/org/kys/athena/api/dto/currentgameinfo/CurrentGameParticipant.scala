package org.kys.athena.api.dto.currentgameinfo

import org.kys.athena.api.dto.common.SummonerSpellsEnum


final case class CurrentGameParticipant(profileIconId: Long,
                                        championId: Long,
                                        summonerName: String,
                                        gameCustomizationObjects: List[GameCustomizationObject],
                                        bot: Boolean,
                                        perks: Perks,
                                        spell2Id: SummonerSpellsEnum,
                                        teamId: Long,
                                        spell1Id: SummonerSpellsEnum,
                                        summonerId: String)
