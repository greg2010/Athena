import { makeStyles } from '@material-ui/core/styles';
import Paper from "@material-ui/core/Paper";
import ChampionIcon from "./ChampionIcon";
import React from "react";
import {ChampionAPI, championDataById, fetchChampion} from "../../api/riot";
import {TeamBan} from "../../api/backend";

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
    bans: TeamBan[]
    championData: ChampionAPI
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

    return (
        <Paper elevation={5} className={classes.bannedMain}>
            {props.bans
                .map(b => {
                    return {...championDataById(props.championData, b.championId), ...b}
                })
                .map(p =>
                    <ChampionIcon
                        champion={p}
                        size='64px'
                        beforeStyles={{...beforeStyle}}
                        styles={{...crossedLineStyle, marginRight:'4px', marginLeft:'4px'}}
                        key={p.pickTurn}/>)
            }
        </Paper>
    );
}

export default Bans