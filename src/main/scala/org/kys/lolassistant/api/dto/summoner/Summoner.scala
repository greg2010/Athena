package org.kys.lolassistant.api.dto.summoner

case class Summoner(profileIconId: Int,
                    name: String,
                    puuid: String,
                    summonerLevel: Long,
                    revisionDate: Long,
                    id: String,
                    accountId: String)
