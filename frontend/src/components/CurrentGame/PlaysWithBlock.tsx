import {makeStyles} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import React from "react";
import {Typography} from "@material-ui/core";
import SeriesBar from "./SeriesBar";
import {ParsedRankedData} from "../../util/rankedDataUtils";
import ChampionIcon from "./ChampionIcon";
import Paper from "@material-ui/core/Paper";
import {ChampionEntry} from "../../api/riot";
import {Skeleton} from "@material-ui/lab";

interface Props {
    playsWithChampions?: (ChampionEntry | undefined)[]
}

const PlaysWithBlock: React.FC<Props> = (props: Props) => {

    const styles = {
        playsWith: {
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
        }
    }

    const classes = makeStyles(() => styles)();

    if (props.playsWithChampions && props.playsWithChampions.length > 0) {
        return (
            <Box display='flex' flexDirection='column' alignItems='center' justifyContent='center'>
                <Typography variant='subtitle2'>Plays with</Typography>
                <Box className={classes.playsWith}>
                    {props.playsWithChampions.map((p, index) =>
                        <ChampionIcon champion={p} size='32px' styles={{marginLeft: '2px', marginRight: '2px'}}
                                      key={index}/>)}
                </Box>
            </Box>)

    } else if (props.playsWithChampions) {
        return (
            <Box display='flex'
                 flexDirection='column'
                 alignItems='center'
                 justifyContent='center'
                 style={{border: '1px solid #000000', borderRadius: '10px', width: '72px', height: '88px'}}>
                <Typography variant='subtitle2'>Plays</Typography>
                <Typography variant='subtitle2'>Solo</Typography>
            </Box>)
    } else {
        return (
            <Box display='flex' flexDirection='column' alignItems='center' justifyContent='center'>
                <Skeleton><Typography variant='subtitle2'>Plays with</Typography></Skeleton>
                <Box className={classes.playsWith}>
                    <Skeleton variant='rect' width='32px' height='32px'/>
                    <Skeleton variant='rect' width='32px' height='32px'/>
                    <Skeleton variant='rect' width='32px' height='32px'/>
                    <Skeleton variant='rect' width='32px' height='32px'/>
                </Box>
            </Box>)
    }
}

export default PlaysWithBlock