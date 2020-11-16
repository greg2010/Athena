import {fetchUrlAs} from "../util/request";

export type CurrentGameResponse = {
    gameId: number,
    gameStartTime: number,
    gameQueueId: number,
    platformId: string,
    blueTeamSummoners: [Summoner],
    blueTeamPositions: TeamPositions,
    blueTeamBans: [TeamBan],
    blueTeamGroups?: [SummonerGroupEntry] | [],
    redTeamSummoners: [Summoner],
    redTeamPositions: TeamPositions,
    redTeamBans: [TeamBan],
    redTeamGroups?: [SummonerGroupEntry] | []
}

export type Summoner = {
    name: string,
    summonerId: string,
    summonerLevel: number,
    championId: number,
    runes: {
        primaryPathId: number,
        secondaryPathId: number,
        keystone: number,
        runeIds: [number]
    },
    summonerSpells: {
        spell1Id: number,
        spell2Id: number
    },
    teamId: number,
    rankedLeagues: RankedLeague[]
}

export type TeamPositions = Record<SSPosition, string>

export enum QueueType {
    RANKED_SOLO_5x5 = 'RANKED_SOLO_5x5',
    RANKED_FLEX_SR = 'RANKED_FLEX_SR'
}

export enum SSPosition {
    TOP = 'TOP',
    JUNGLE = 'JUNGLE',
    MIDDLE = 'MIDDLE',
    BOTTOM = 'BOTTOM',
    UTILITY = 'UTILITY',
}

export type RankedLeague = {
    leagueId: string,
    queueType: QueueType,
    tier: string,
    rank: string,
    leaguePoints: number,
    wins: number,
    losses: number,
    miniSeries: null | Series
}

export type Series = {
    wins: number,
    losses: number,
    target: number,
    progress: string
}

export type TeamBan = {
    pickTurn: number,
    championId: number,
    teamId: number
}

export type SummonerGroupEntry = {
    summoners: [string],
    gamesPlayed: number
}

export const positionOrder = (position: SSPosition): number => {
    switch(position) {
        case SSPosition.TOP: {
            return 0
        }
        case SSPosition.JUNGLE: {
            return 1
        }
        case SSPosition.MIDDLE: {
            return 2
        }
        case SSPosition.BOTTOM: {
            return 3
        }
        case SSPosition.UTILITY: {
            return 4
        }
    }
}


export const currentGameKey = 'backendCurGame'
export const fetchCurrentGame = (realm: string, summonerName: string): () => Promise<CurrentGameResponse> => {
    // @ts-ignore
    const url = 'http://' +  window._env_.BACKEND_API_URL + '/premades/by-summoner-name/' + summonerName + '?platform=' + realm + '&gameDepth=5'
    return () => fetchUrlAs<CurrentGameResponse>(url)
}