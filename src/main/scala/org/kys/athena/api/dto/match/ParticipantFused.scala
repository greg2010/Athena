package org.kys.athena.api.dto.`match`

case class ParticipantFused(championId: Int,
                            highestAchievedSeasonTier: Option[String],
                            participantId: Int,
                            spell1Id: Int,
                            spell2Id: Int,
                            //stats: ParticipantStats,
                            teamId: Int,
                            //timeline: ParticipantTimeline,
                            player: Player)
