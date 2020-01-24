package org.kys.athena.api.dto.`match`

final case class Participant(championId: Int,
                             highestAchievedSeasonTier: Option[String],
                             participantId: Int,
                             spell1Id: Int,
                             spell2Id: Int, //stats: ParticipantStats,
                             teamId: Int //timeline: ParticipantTimeline
                            )
