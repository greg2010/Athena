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
import {ChampionAPI, championDataById, RunesReforgedAPI, SummonerSpellAPI} from "../../api/riot";
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
    playerData?: Summoner
    championData?: ChampionAPI,
    ssData?: SummonerSpellAPI,
    runesData?: RunesReforgedAPI,
    playedWith?: number[]
}

const CurrentGame: React.FC<Props> = (props: Props) => {
    const classes = useStyles();


    let championName
    if (props.championData && props.playerData) {
        championName = championDataById(props.championData, props.playerData.championId)?.name
    }
    let rankedData
    if (props.playerData) {
        rankedData = rankedUtils.rankedDataUtils(props.playerData.rankedLeagues)
    }

    return (
        <Paper elevation={4} className={classes.cardMain}>
            <ChampionIcon championData={props.championData}
                          championId={props.playerData?.championId}
                          size='80px'
                          styles={{'margin-left': '4px', 'margin-right': '4px'}} ttPlacement='left'/>
            <Box display='flex' flexDirection='column' height='90%' justifyContent='space-around'>
                <SummonerSpellIcon size='32px'
                                   ssData={props.ssData}
                                   summonerSpellId={props.playerData?.summonerSpells.spell1Id}/>
                <SummonerSpellIcon size='32px'
                                   ssData={props.ssData}
                                   summonerSpellId={props.playerData?.summonerSpells.spell2Id}/>
            </Box>
            <Box display='flex' flexDirection='column' height='90%' justifyContent='space-around'
                 alignItems='center' marginLeft='4px'>
                <RuneIcon boxSize='32px'
                          iconSize='32px'
                          rrData={props.runesData}
                          treeId={props.playerData?.runes.primaryPathId}
                          keystoneId={props.playerData?.runes.keystone}/>
                <RuneIcon boxSize='32px'
                          iconSize='26px'
                          rrData={props.runesData}
                          treeId={props.playerData?.runes.secondaryPathId}
                          runeStyle={{borderRadius: '50%'}}/>
            </Box>
            <Box className={classes.nameBlock}>
                {props.playerData?.name ?
                    <Typography className={classes.nameText} variant='h6'>{props.playerData.name}</Typography>
                    :
                    <Skeleton><Typography className={classes.nameText} variant='h6'>Loading</Typography></Skeleton>}
                {championName ?
                    <Typography className={classes.championText} variant='subtitle1'>{championName}</Typography>
                    : <Skeleton><Typography className={classes.championText}
                                            variant='subtitle1'>Loading</Typography></Skeleton>}
                {props.playerData ? <RankedWinrate rankedData={rankedData}/> : <RankedWinrate isLoading={true}/>}
            </Box>
            {props.playerData ? <RankedIconBlock rankedData={rankedData}/> : <RankedIconBlock isLoading={true}/>}
            <PlaysWithBlock playsWithChampions={props.playedWith}
                            championData={props.championData}/>
        </Paper>
    );


}

export default CurrentGame