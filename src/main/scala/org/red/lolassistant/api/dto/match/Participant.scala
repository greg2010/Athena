package org.red.lolassistant.api.dto.`match`


case class Participant(championId: Int,
                       highestAchievedSeasonTier: Option[String],
                       participantId: Int,
                       spell1Id: Int,
                       spell2Id: Int,
                       //stats: ParticipantStats,
                       teamId: Int,
                       //timeline: ParticipantTimeline
)
