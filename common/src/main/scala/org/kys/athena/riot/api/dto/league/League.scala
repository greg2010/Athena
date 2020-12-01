package org.kys.athena.riot.api.dto.league


final case class League(leagueId: String,
                        queueType: RankedQueueTypeEnum,
                        tier: String,
                        rank: String,
                        summonerId: String,
                        summonerName: String,
                        leaguePoints: Int,
                        wins: Int,
                        losses: Int,
                        veteran: Boolean,
                        inactive: Boolean,
                        freshBlood: Boolean,
                        hotStreak: Boolean,
                        miniSeries: Option[MiniSeries])
