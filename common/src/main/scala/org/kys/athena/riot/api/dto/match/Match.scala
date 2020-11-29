package org.kys.athena.riot.api.dto.`match`

import cats.implicits._
import org.kys.athena.riot.api.dto.common.GameQueueTypeEnum


final case class Match(seasonId: Int,
                       queueId: GameQueueTypeEnum,
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

  lazy val participantsFused: Option[List[ParticipantFused]] = {
    participantIdentities.map { pId =>
      participants.find(_.participantId == pId.participantId).map { p =>
        ParticipantFused(p.championId, p.highestAchievedSeasonTier, p.participantId, p.spell1Id, p.spell2Id, p.teamId,
                         pId.player)
      } // Sequencing short circuit logic
    }.traverse(identity)
  }
}
