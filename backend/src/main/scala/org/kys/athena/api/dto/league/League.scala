package org.kys.athena.api.dto.league


final case class League(leagueId: String,
                        queueType: String,
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
