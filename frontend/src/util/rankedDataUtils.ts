import {QueueType, RankedLeague, Series} from "../api/backend";

export type ParsedRankedData = {
    totalGames: number,
    wins: number,
    losses: number,
    winrate: number,
    league: string,
    division: string,
    lp: number,
    series: null | Series,
    queueType: QueueType

}

export const rankedDataUtils = (rankedArr: RankedLeague[]): undefined | ParsedRankedData => {
    let data = rankedArr.find(r => r.queueType == QueueType.RANKED_SOLO_5x5)
    if (!data) data = rankedArr.find(r => r.queueType == QueueType.RANKED_FLEX_SR)
    if (!data) return undefined

    return {
        totalGames: data.wins + data.losses,
        wins: data.wins,
        losses: data.losses,
        winrate: data.wins / (data.wins + data.losses),
        league: data.tier.charAt(0) + data.tier.slice(1).toLowerCase(),
        division: data.rank,
        lp: data.leaguePoints,
        series: data.miniSeries,
        queueType: data.queueType
    }
}

export const renderWRPercentage = (winrate: number): number => {
    return Math.round(winrate * 1000) / 10
}