package org.kys.athena.data

import org.kys.athena.http.models.premade.PlayerGroup


case class TeamGroupsTuple(blueTeamGroups: Set[PlayerGroup], redTeamGroups: Set[PlayerGroup])
