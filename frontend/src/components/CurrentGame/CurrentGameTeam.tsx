import {createStyles, makeStyles} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import React from "react";
import TeamHeader from "./TeamHeader";
import CurrentGameCard from "./CurrentGameCard";
import Bans from "./Bans";
import {rankedDataUtils} from "../../util/rankedDataUtils";
import {positionOrder, SSPosition, Summoner, SummonerGroupEntry, TeamBan, TeamPositions} from "../../api/backend";
import {ChampionAPI, RunesReforgedAPI, SummonerSpellAPI} from "../../api/riot";





interface Props {
    teamSummoners?: [Summoner],
    teamPositions?: TeamPositions,
    teamGroups?: SummonerGroupEntry[],
    teamBans?: [TeamBan],
    teamName: string,

    championData?: ChampionAPI,
    summonerSpellData?: SummonerSpellAPI,
    runesReforgedData?: RunesReforgedAPI
}

const CurrentGameTeam: React.FC<Props> = (props: Props) => {
    type TeamStats = {
        winrateAverage: number,
        winrateRange: number
    }

    const processTeam = (team: [Summoner]): TeamStats => {
        const rankedStats = team.map(s => rankedDataUtils(s.rankedLeagues))
        const teamWrs = rankedStats.flatMap(s => s ? [s.winrate] : []).sort()
        const wrAverage = teamWrs.reduce((l,r) => l+r) / rankedStats.length
        const wrRange = teamWrs.slice(-1)[0] - teamWrs[0]

        return {
            winrateAverage: wrAverage,
            winrateRange: wrRange
        }
    }

    const outerJoin = <T extends unknown>(key: T, arr: [T][]): T[] => {
        const ret = new Set<T>([key])
        let prevSize = 1
        do {
            prevSize = ret.size
            const newKeys = arr.filter(subarr => subarr.some(elem => ret.has(elem))).flat()
            newKeys.forEach(elem => ret.add(elem))
        } while (prevSize != ret.size)

        return Array.from(ret)
    }

    const determinePlayedWith = (summoners: [Summoner], groups: SummonerGroupEntry[]) => {
        // @ts-ignore
        return summoners.map(summoner => {
            return {
                summonerId: summoner.summonerId,
                playedWith: outerJoin(summoner.summonerId, groups.map(e => e.summoners))
                    .filter(p => p != summoner.summonerId)
                    .map(id => {
                        // We always expect this to return a value
                        // @ts-ignore
                        return summoners.find(e => e.summonerId == id).championId
                    })
            }
        })
    }

    const styles = createStyles({
        teamBox: {
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            marginTop: '8px',
            marginBottom: '8px'
        }
    })

    const classes = makeStyles(() => styles)();

    const isLoading = !props.teamSummoners || !props.championData || !props.runesReforgedData || !props.summonerSpellData

    const teamAverages = props.teamSummoners ? processTeam(props.teamSummoners) : {}

    let sortedSummoners = props.teamSummoners

    if (sortedSummoners && props.teamPositions) {
        sortedSummoners = sortedSummoners.sort((sumL, sumR) => {
            const posns = Object.entries(props.teamPositions!) as [SSPosition, string][]
            // @ts-ignore position always guaranteed to be in array
            const sumLPosn = posns.find(kv => kv[1] == sumL.summonerId)[0]
            // @ts-ignore position always guaranteed to be in array
            const sumRPosn = posns.find(kv => kv[1] == sumR.summonerId)[0]
            return positionOrder(sumLPosn) - positionOrder(sumRPosn)
        })
    }

    let playedWith: { summonerId: string, playedWith: number[] }[] | undefined
    if (sortedSummoners && props.teamGroups) {
        playedWith = determinePlayedWith(sortedSummoners, props.teamGroups)
    }


    return (
        <Box className={classes.teamBox}>
            <TeamHeader isLoading={isLoading} {...teamAverages} teamName={props.teamName}/>
            {!sortedSummoners ?
                [0, 1, 2, 3, 4].map(i => <CurrentGameCard key={i}/>)
                : sortedSummoners.map((s) =>
                    <CurrentGameCard playerData={s}
                                     key={s.summonerId}
                                     championData={props.championData}
                                     runesData={props.runesReforgedData}
                                     ssData={props.summonerSpellData}
                                     playedWith={playedWith?.find(g => g.summonerId == s.summonerId)?.playedWith}/>)}
            <Bans isLoading={isLoading} bans={props.teamBans} championData={props.championData}/>
        </Box>
        );
}

export default CurrentGameTeam