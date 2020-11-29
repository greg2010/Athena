package org.kys.athena.http.models.current

import org.kys.athena.riot.api.dto.currentgameinfo.BannedChampion


case class OngoingGameTeam(summoners: Set[InGameSummoner],
                           positions: Option[Map[PositionEnum, String]],
                           bans: Option[Set[BannedChampion]])
