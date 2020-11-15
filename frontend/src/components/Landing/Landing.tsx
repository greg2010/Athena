import React, {useEffect} from 'react';
import { fade, makeStyles } from '@material-ui/core/styles';
import Container from "@material-ui/core/Container";
import Typography from "@material-ui/core/Typography";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";
import Box from "@material-ui/core/Box";
import SimpleSearchBar from "../Layout/SimpleSearchBar";
import AppNavbar from "../Layout/AppNavBar";
import {queryCache} from "react-query";
import {
    ddChampionKey,
    ddRuneKey,
    ddSummonerKey,
    fetchChampion,
    fetchRunesReforged,
    fetchSummonerSpell
} from "../../api/riot";

const useStyles = makeStyles(theme => ({
    container: {
        marginTop: theme.spacing(8),
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        padding: `${theme.spacing(2)}px ${theme.spacing(3)}px ${theme.spacing(3)}px`,
    },
    mainPaper: {
        height: '400px',
        border: '1px solid #000000',
        'border-radius': '10px',
        padding: `${theme.spacing(2)}px ${theme.spacing(3)}px ${theme.spacing(3)}px`,
    }
}));



const Landing: React.FC = () => {
    const prefetchDD = async () => {
        await queryCache.prefetchQuery(ddChampionKey, fetchChampion)
        await queryCache.prefetchQuery(ddSummonerKey, fetchSummonerSpell)
        await queryCache.prefetchQuery(ddRuneKey, fetchRunesReforged)

        // The results of this query will be cached like a normal query

    }

    useEffect(() => {
        prefetchDD()
    })

    const classes = useStyles();

    return (
        <Container className={classes.container}>
                <Box display='flex' flexDirection='column' alignItems='center' justifyContent='space-around'>
                    <Box>
                        <img src="blitzcrank_logo.png" alt="Logo"/>
                    </Box>
                    <Box width='700px'>
                        <SimpleSearchBar isLanding={true} />
                    </Box>
                </Box>
        </Container>
    );
}

export default Landing