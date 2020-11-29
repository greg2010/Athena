package org.kys.athena.riot.api.dto.currentgameinfo

import org.kys.athena.riot.api.Platform
import org.kys.athena.riot.api.dto.common.GameQueueTypeEnum


final case class CurrentGameInfo(gameId: Long,
                                 gameStartTime: Long,
                                 platformId: Platform,
                                 gameMode: String,
                                 mapId: Long,
                                 gameType: String,
                                 bannedChampions: List[BannedChampion],
                                 observers: Observer,
                                 participants: List[CurrentGameParticipant],
                                 gameLength: Long,
                                 gameQueueConfigId: GameQueueTypeEnum)
