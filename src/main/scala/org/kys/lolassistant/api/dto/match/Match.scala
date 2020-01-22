package org.kys.lolassistant.api.dto.`match`

case class Match(seasonId: Int,
                 queueId: Int,
                 gameId: Long,
                 participantIdentities: List[ParticipantIdentity],
                 gameVersion: String,
                 platformId: String,
                 gameMode: String,
                 mapId: Int,
                 gameType: String,
                 teams: List[TeamStats],
                 participants: List[Participant],
                 gameDuration: Long,
                 gameCreation: Long) {
  lazy val participantsFused: Seq[ParticipantFused] = {
    participantIdentities.map { pId =>
      participants.find(_.participantId == pId.participantId) match {
        case Some(p) =>
          ParticipantFused(
            p.championId,
            p.highestAchievedSeasonTier,
            p.participantId,
            p.spell1Id,
            p.spell2Id,
            p.teamId,
            pId.player
          )
        case None => throw new RuntimeException("Not found participant")
      }
    }
  }
}














