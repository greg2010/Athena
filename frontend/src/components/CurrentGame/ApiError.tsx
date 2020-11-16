import {makeStyles} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import React from "react";
import {Button, Divider, Typography} from "@material-ui/core";
import SeriesBar from "./SeriesBar";
import {ParsedRankedData} from "../../util/rankedDataUtils";
import ChampionIcon from "./ChampionIcon";
import Paper from "@material-ui/core/Paper";
import {ChampionEntry} from "../../api/riot";
import {Skeleton} from "@material-ui/lab";

interface Props {
    reloadHook: () => void
}

const ApiError: React.FC<Props> = (props: Props) => {

    const styles = makeStyles(theme => ({
        container: {
            marginTop: theme.spacing(8),
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            padding: `${theme.spacing(2)}px ${theme.spacing(3)}px ${theme.spacing(3)}px`,
        },
        mainPaper: {
            border: '1px solid #000000',
            'border-radius': '10px',
            padding: `${theme.spacing(2)}px ${theme.spacing(3)}px ${theme.spacing(3)}px`,
            backgroundColor: 'rgba(255,255,255,0.5)'
        },
    }))

    const classes = styles()
    return (
        <Box className={classes.container}>
            <Paper elevation={5} className={classes.mainPaper}>
                <Box display='flex' flexDirection='column' justifyContent='center' alignItems='center'>
                    <img src='amumu_error.png' alt='api-error' width='271px' height='239px' style={{marginBottom: '12px'}}/>
                    <Divider flexItem={true} variant='fullWidth' />
                    <Typography variant='h4' style={{marginBottom: '12px'}}>
                        We&apos;re experiencing server issues :&apos;(</Typography>
                    <Button onClick={props.reloadHook}>Reload</Button>
                </Box>
            </Paper>
        </Box>
    )
}

export default ApiError