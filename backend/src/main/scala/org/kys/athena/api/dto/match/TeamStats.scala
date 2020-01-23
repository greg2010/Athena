package org.kys.athena.api.dto.`match`

final case class TeamStats(bans: List[TeamBans],
                           baronKills: Int,
                           dominionVictoryScore: Int,
                           dragonKills: Int,
                           firstBaron: Boolean,
                           firstBlood: Boolean,
                           firstDragon: Boolean,
                           firstInhibitor: Boolean,
                           firstRiftHerald: Boolean,
                           firstTower: Boolean,
                           inhibitorKills: Int,
                           riftHeraldKills: Int,
                           teamId: Int,
                           towerKills: Int,
                           vilemawKills: Int,
                           win: String)
