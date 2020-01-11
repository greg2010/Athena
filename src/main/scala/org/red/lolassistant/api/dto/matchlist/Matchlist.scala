package org.red.lolassistant.api.dto.matchlist

case class Matchlist(matches: List[MatchReference],
                     totalGames: Int,
                     startIndex: Int,
                     endIndex: Int)