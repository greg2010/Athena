package org.kys.lolassistant.http.models

import org.kys.lolassistant.api.dto.`match`.Participant
import org.kys.lolassistant.api.dto.currentgameinfo.CurrentGameParticipant
import org.kys.lolassistant.api.dto.summoner.Summoner

case class InGameSummoner(summoner: Summoner, participant: CurrentGameParticipant)