package org.kys.athena.api.dto.`match`

import org.kys.athena.api.dto.common.SummonerSpellsEnum


final case class ParticipantFused(championId: Int,
                                  highestAchievedSeasonTier: Option[String],
                                  participantId: Int,
                                  spell1Id: SummonerSpellsEnum,
                                  spell2Id: SummonerSpellsEnum, //stats: ParticipantStats,
                                  teamId: Int, //timeline: ParticipantTimeline,
                                  player: Player)
