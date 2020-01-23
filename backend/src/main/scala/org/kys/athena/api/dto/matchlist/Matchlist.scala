package org.kys.athena.api.dto.matchlist

final case class Matchlist(matches: List[MatchReference], totalGames: Int, startIndex: Int, endIndex: Int)
