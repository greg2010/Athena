import {fetchUrlAs} from "../util/request";


export type RunesReforgedAPI = [RunesReforgedEntry & {
    slots: [
        {
            runes: [RunesReforgedEntry]
        }
    ]
}]

export type SummonerSpellAPI = RiotAPI<SummonerSpellEntry>
export type ChampionAPI = RiotAPI<ChampionEntry>


export type SummonerSpellEntry = {
    id: string,
    name: string,
    description: string,
    key: string
}

export type ChampionEntry = {
    id: string,
    name: string,
    key: string
}

export type RunesReforgedEntry = {
    id: number,
    key: string,
    icon: string,
    name: string,
}

interface RiotAPI<V> {
    type: string,
    version: string,
    data: Record<string, V>
}

export const fetchChampion = (): () =>  Promise<ChampionAPI> => {
    // @ts-ignore
    const url = `${window._env_.DDRAGON_BASE_URL + window._env_.DDRAGON_VERSION}/data/${window._env_.LOCALE}/champion.json`
    return () => fetchUrlAs<ChampionAPI>(url)
}

export const fetchSummonerSpell = (): () =>   Promise<SummonerSpellAPI> => {
    // @ts-ignore
    const url = `${window._env_.DDRAGON_BASE_URL + window._env_.DDRAGON_VERSION}/data/${window._env_.LOCALE}/summoner.json`
    return  () => fetchUrlAs<SummonerSpellAPI>(url)
}

export const fetchRunesReforged = ():  () => Promise<RunesReforgedAPI> => {
    // @ts-ignore
    const url = `${window._env_.DDRAGON_BASE_URL + window._env_.DDRAGON_VERSION}/data/${window._env_.LOCALE}/runesReforged.json`
    return  () => fetchUrlAs<RunesReforgedAPI>(url)
}

export const ddChampionKey = 'ddChampion'
export const championDataById = (champion: ChampionAPI, championId: number): ChampionEntry | undefined => {
    return Object.values(champion.data).find(ch => ch.key == championId.toString())
}

export const ddSummonerKey = 'ddSummoner'
export const summonerSpellDataById = (ss: SummonerSpellAPI, spellId: number): SummonerSpellEntry => {
    const ret = Object.values(ss.data).find(ch => ch.key == spellId.toString())
    if (ret) return ret
    else throw Error('Summoner spell ID resolution failed, spellId=' + spellId)
}

export const ddRuneKey = 'ddRune'
export const runesReforgedDataById = (rr: RunesReforgedAPI, treeId: number, keystoneId?: number): RunesReforgedEntry => {
    const tree = rr.find(r => r.id == treeId)
    if (!tree) throw Error('Rune ID resolution failed, treeId=' + treeId)
    let ret
    if (keystoneId) {
        ret = tree.slots[0].runes.find(r => r.id == keystoneId)
    } else {
        ret = tree
    }

    if (!ret) throw Error('Keystone ID resolution failed, treeId=' + treeId + ' keystoneId=' + keystoneId)
    return ret
}