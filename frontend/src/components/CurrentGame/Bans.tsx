import { makeStyles } from '@material-ui/core/styles';
import Paper from "@material-ui/core/Paper";
import ChampionIcon from "./ChampionIcon";
import React from "react";
import {ChampionAPI, championDataById, ChampionEntry, fetchChampion} from "../../api/riot";
import {TeamBan} from "../../api/backend";
import {Skeleton} from "@material-ui/lab";

const useStyles = makeStyles(theme => ({
    bannedMain: {
        display: 'inline-flex',
        border: '1px solid #800000',
        borderRadius: '10px',
        paddingTop: '4px',
        paddingBottom: '4px',
        marginTop: '8px',
        marginBot: '12px',
        alignItems: 'center',
    },
}));

interface Props {
    bans?: TeamBan[]
    championData?: ChampionAPI,
    isLoading: boolean
}

const Bans: React.FC<Props> = (props: Props) => {

    const classes = useStyles();

    const crossedLineStyle = {
        '&::after': {
            content: '""',
            position: 'absolute',
            width: '100%',
            height: '100%',
            backgroundImage: 'url(slash_red_256.png)',
            backgroundSize: '100% 100%',
            backgroundPosition: 'center center',
            backgroundRepeat: 'no-repeat',
            borderRadius: '10%'
        }
    }

    const beforeStyle = {
        filter: 'grayscale(50%)'
    }
    if (props.isLoading) {
        return (
            <Paper elevation={5} className={classes.bannedMain}>
                {[0, 1, 2, 3, 4].map(i => <Skeleton variant={'rect'}
                                                    width='64px'
                                                    height='64px'
                                                    style={{
                                                        marginRight: '4px',
                                                        marginLeft: '4px',
                                                        borderRadius: '10%'
                                                    }}
                                                    key={i}/>)}
            </Paper>)
    }

    if (props.bans && props.bans.length > 0) {
        return (
            <Paper elevation={5} className={classes.bannedMain}>
                {props.bans
                    .map((p, index) =>
                        <ChampionIcon
                            championId={p.championId}
                            championData={props.championData}
                            size='64px'
                            beforeStyles={{...beforeStyle}}
                            styles={{...crossedLineStyle, marginRight: '4px', marginLeft: '4px'}}
                            key={index}/>)
                }
            </Paper>
        );
    } else {
        return null
    }
}

export default Bans