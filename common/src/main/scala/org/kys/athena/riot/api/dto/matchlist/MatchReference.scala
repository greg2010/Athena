package org.kys.athena.riot.api.dto.matchlist

final case class MatchReference(champion: Int,
                                gameId: Long,
                                lane: String,
                                platformId: String,
                                queue: Int,
                                role: String,
                                season: Int,
                                timestamp: Long)
