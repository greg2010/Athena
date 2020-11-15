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
    },
    playedWith: {
        display: 'inline-flex',
        'flex-wrap': 'wrap',
        'align-items': 'center',
        'justify-content': 'center',
        width: '72px',
        height: '72px',
        marginBottom: '4px'
    },
    playedWithChampion: {
        width: '32px',
        height: '32px',
        marginLeft: '2px',
        marginRight: '2px'
    },
}));

interface Props {
    playerData: SummonerWithGroups
    championData: ChampionAPI,
    ssData: SummonerSpellAPI,
    runesData: RunesReforgedAPI
}

const CurrentGame: React.FC<Props> = (props: Props) => {

    const resolveIds = (playerData: SummonerWithGroups) => {
        const championObj = championDataById(props.championData, playerData.championId)

        const sums = Object.values(playerData.summonerSpells).map(ss => summonerSpellDataById(props.ssData, ss))

        let championsPlayedWith: ChampionEntry[] = []
        if (playerData.playedWithChampions) {
            championsPlayedWith = playerData.playedWithChampions.map(c => championDataById(props.championData, c))
        }

        return {
            champion: championObj,
            summonerName: playerData.name,
            summonerSpells: sums,
            runes: {
                keystone: runesReforgedDataById(props.runesData, playerData.runes.primaryPathId, playerData.runes.keystone),
                secondary: runesReforgedDataById(props.runesData, playerData.runes.secondaryPathId)
            },
            championsPlayedWith: championsPlayedWith
        }
    }

    const resolvedPlayerData = resolveIds(props.playerData)
    const processedRankedData = rankedUtils.rankedDataUtils(props.playerData.rankedLeagues)

    const classes = useStyles();

    return (
            <Paper elevation={4} className={classes.cardMain}>
                <ChampionIcon champion={resolvedPlayerData.champion} size='80px' styles={{'margin-left': '4px', 'margin-right': '4px'}} ttPlacement='left' />
                <Box display='flex' flexDirection='column' height='90%' justifyContent='space-around'>
                    {resolvedPlayerData.summonerSpells.map((s, index) => <SummonerSpellIcon summonerSpell={s} size='32px' key={index}/>)}
                </Box>
                <Box display='flex' flexDirection='column' height='90%' justifyContent='space-around' alignItems='center' marginLeft='4px'>
                    <RuneIcon boxSize='32px' iconSize='32px' rune={resolvedPlayerData.runes.keystone} />
                    <RuneIcon boxSize='32px' iconSize='26px' rune={resolvedPlayerData.runes.secondary} runeStyle={{borderRadius: '50%'}} />
                </Box>
                <Box className={classes.nameBlock}>
                    <Typography className={classes.nameText} variant='h6'>{resolvedPlayerData.summonerName}</Typography>
                    <Typography className={classes.championText} variant='subtitle1'>{resolvedPlayerData.champion.name}</Typography>
                    <RankedWinrate rankedData={processedRankedData}/>
                </Box>
                <RankedIconBlock rankedData={processedRankedData}/>
                <Box display='flex' flexDirection='column' alignItems='center' justifyContent='center' >
                    <Typography variant='subtitle2'>Plays with</Typography>
                    <Box className={classes.playedWith}>
                        {resolvedPlayerData.championsPlayedWith.map(p =>
                            <ChampionIcon champion={p} size='32px' styles={{marginLeft: '2px', marginRight: '2px'}} key={p.key}/>)}
                    </Box>
                </Box>
            </Paper>
    );
}

export default CurrentGame