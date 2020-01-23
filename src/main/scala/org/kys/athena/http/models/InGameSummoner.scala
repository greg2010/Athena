package org.kys.athena.http.models

import org.kys.athena.api.dto.currentgameinfo.CurrentGameParticipant
import org.kys.athena.api.dto.summoner.Summoner

case class InGameSummoner(summoner: Summoner, participant: CurrentGameParticipant)