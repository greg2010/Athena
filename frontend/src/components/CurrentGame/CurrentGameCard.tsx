import React from "react";
import { makeStyles } from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import Paper from "@material-ui/core/Paper";
import {Typography} from "@material-ui/core";
import ChampionIcon from "./ChampionIcon";
import SummonerSpellIcon from "./SummonerSpellIcon";
import RuneIcon from "./RuneIcon";
import RankedWinrate from "./RankedWinrate";
import RankedIconBlock from "./RankedIconBlock";
import * as rankedUtils from "../../util/rankedDataUtils";
import {SummonerWithGroups} from "./CurrentGameTeam";
import {
    ChampionAPI,
    championDataById,
    ChampionEntry, RunesReforgedAPI,
    runesReforgedDataById, SummonerSpellAPI,
    summonerSpellDataById
} from "../../api/riot";
import {Skeleton} from "@material-ui/lab";
import PlaysWithBlock from "./PlaysWithBlock";
import {Summoner} from "../../api/backend";


const useStyles = makeStyles(theme => ({
    cardMain: {
        display: 'inline-flex',
        border: '1px solid #000000',
        'border-radius': '10px',
        padding: '4px',
        height: '108px',
        maxWidth: '540px',
        minWidth: '432px',
        width: '100%',
        marginBottom: '4px',
        'align-items': 'center',
        'justify-content': 'space-around',
    },
    nameBlock: {
        display: 'inline-flex',
        'flex-direction': 'column',
        'align-items': 'center',
        width: '180px',
    },
    nameText: {
        textOverflow: 'ellipsis',
        overflow: 'hidden',
        whiteSpace: 'nowrap',
        lineHeight: '1.2',
        maxWidth: '100%'
    },
    championText: {
        lineHeight: '1.5',
    }
}));

interface Props {
    playerData?: Summoner | SummonerWithGroups
    championData?: ChampionAPI,
    ssData?: SummonerSpellAPI,
    runesData?: RunesReforgedAPI
}

const CurrentGame: React.FC<Props> = (props: Props) => {
    const classes = useStyles();
    const resolveIds = (playerData: SummonerWithGroups, championData: ChampionAPI, ssData: SummonerSpellAPI, rrData: RunesReforgedAPI) => {
        const championObj = championDataById(championData, playerData.championId)

        const sums = Object.values(playerData.summonerSpells).map(ss => summonerSpellDataById(ssData, ss))

        let championsPlayedWith: (ChampionEntry | undefined)[] | undefined = undefined
        if (playerData.playedWithChampions) {
            championsPlayedWith = playerData.playedWithChampions.map(c => championDataById(championData, c))
        }

        return {
            champion: championObj,
            summonerName: playerData.name,
            summonerSpells: sums,
            runes: {
                keystone: runesReforgedDataById(rrData, playerData.runes.primaryPathId, playerData.runes.keystone),
                secondary: runesReforgedDataById(rrData, playerData.runes.secondaryPathId)
            },
            championsPlayedWith: championsPlayedWith
        }
    }


    if (props.championData && props.ssData && props.runesData && props.playerData) {
        const resolvedPlayerData = resolveIds(props.playerData, props.championData, props.ssData, props.runesData)
        const processedRankedData = rankedUtils.rankedDataUtils(props.playerData.rankedLeagues)
        return (
            <Paper elevation={4} className={classes.cardMain}>
                <ChampionIcon champion={resolvedPlayerData.champion} size='80px'
                              styles={{'margin-left': '4px', 'margin-right': '4px'}} ttPlacement='left'/>
                <Box display='flex' flexDirection='column' height='90%' justifyContent='space-around'>
                    {resolvedPlayerData.summonerSpells.map((s, index) => <SummonerSpellIcon summonerSpell={s}
                                                                                            size='32px'
                                                                                            key={index}/>)}
                </Box>
                <Box display='flex' flexDirection='column' height='90%' justifyContent='space-around'
                     alignItems='center' marginLeft='4px'>
                    <RuneIcon boxSize='32px' iconSize='32px' rune={resolvedPlayerData.runes.keystone}/>
                    <RuneIcon boxSize='32px' iconSize='26px' rune={resolvedPlayerData.runes.secondary}
                              runeStyle={{borderRadius: '50%'}}/>
                </Box>
                <Box className={classes.nameBlock}>
                    <Typography className={classes.nameText} variant='h6'>{resolvedPlayerData.summonerName}</Typography>
                    <Typography className={classes.championText}
                                variant='subtitle1'>{resolvedPlayerData.champion!.name}</Typography>
                    <RankedWinrate rankedData={processedRankedData}/>
                </Box>
                <RankedIconBlock rankedData={processedRankedData}/>
                <PlaysWithBlock playsWithChampions={resolvedPlayerData.championsPlayedWith}/>
            </Paper>
        );
    } else {
        return (
            <Paper elevation={4} className={classes.cardMain}>
                <Skeleton variant='rect' width='80px' height='80px'/>
                <Box display='flex' flexDirection='column' height='90%' justifyContent='space-around'>
                    <Skeleton variant='rect' width='32px' height='32px'/>
                    <Skeleton variant='rect' width='32px' height='32px'/>
                </Box>
                <Box display='flex' flexDirection='column' height='90%' justifyContent='space-around'
                     alignItems='center' marginLeft='4px'>
                    <Skeleton variant='circle' width='32px' height='32px'/>
                    <Skeleton variant='circle' width='32px' height='32px'/>
                </Box>
                <Box className={classes.nameBlock}>
                    <Skeleton>
                        <Typography className={classes.nameText} variant='h6'>Summoner Name</Typography>
                    </Skeleton>
                    <Skeleton>
                        <Typography className={classes.championText}
                                    variant='subtitle1'>Champ Name</Typography>
                    </Skeleton>
                    <Skeleton><RankedWinrate rankedData={undefined}/></Skeleton>
                </Box>
                <Skeleton><RankedIconBlock rankedData={undefined}/></Skeleton>
                <PlaysWithBlock/>
            </Paper>
        )
    }


}

export default CurrentGame