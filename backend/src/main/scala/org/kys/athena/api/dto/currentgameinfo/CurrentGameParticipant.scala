package org.kys.athena.api.dto.currentgameinfo

final case class CurrentGameParticipant(profileIconId: Long,
                                        championId: Long,
                                        summonerName: String,
                                        gameCustomizationObjects: List[GameCustomizationObject],
                                        bot: Boolean,
                                        perks: Perks,
                                        spell2Id: Long,
                                        teamId: Long,
                                        spell1Id: Long,
                                        summonerId: String)
