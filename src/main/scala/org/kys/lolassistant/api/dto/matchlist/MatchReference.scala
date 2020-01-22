package org.kys.lolassistant.api.dto.matchlist

case class MatchReference(champion: Int,
                          gameId: Long,
                          lane: String,
                          platformId: String,
                          queue: Int,
                          role: String,
                          season: Int,
                          timestamp: Long)
