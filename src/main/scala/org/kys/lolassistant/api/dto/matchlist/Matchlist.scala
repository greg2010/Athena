package org.kys.lolassistant.api.dto.matchlist

case class Matchlist(matches: List[MatchReference],
                     totalGames: Int,
                     startIndex: Int,
                     endIndex: Int)