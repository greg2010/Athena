package org.kys.athena.api.dto.currentgameinfo

import org.kys.athena.api.dto.common.GameQueueTypeEnum


final case class CurrentGameInfo(gameId: Long,
                                 gameStartTime: Long,
                                 platformId: String,
                                 gameMode: String,
                                 mapId: Long,
                                 gameType: String,
                                 bannedChampions: List[BannedChampion],
                                 observers: Observer,
                                 participants: List[CurrentGameParticipant],
                                 gameLength: Long,
                                 gameQueueConfigId: GameQueueTypeEnum)
