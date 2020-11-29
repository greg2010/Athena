package org.kys.athena.meraki.api.dto

import org.kys.athena.http.models.current.PositionEnum



case class ChampionRates(data: Map[Int, Map[PositionEnum, ChampionRolePlayrate]])
