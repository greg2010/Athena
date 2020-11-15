import React from 'react';
import {makeStyles} from '@material-ui/core/styles';
import Container from "@material-ui/core/Container";
import Typography from "@material-ui/core/Typography";
import Paper from "@material-ui/core/Paper";
import Box from "@material-ui/core/Box";
import {RouteComponentProps, withRouter} from "react-router";
import sampledata from './triggers0.json'
import Divider from "@material-ui/core/Divider";
import Grid from "@material-ui/core/Grid";
import CurrentGameTeam from "./CurrentGameTeam";
import {currentGameKey, CurrentGameResponse, fetchCurrentGame} from "../../api/backend";
import {
    ddChampionKey,
    ddRuneKey,
    ddSummonerKey,
    fetchChampion,
    fetchRunesReforged,
    fetchSummonerSpell
} from "../../api/riot";
import {useQuery} from "react-query";
import {FetchError} from "../../util/request";

const useStyles = makeStyles(theme => ({
    container: {
        marginTop: theme.spacing(8),
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        padding: `${theme.spacing(2)}px ${theme.spacing(3)}px ${theme.spacing(3)}px`,
    },
    separatorLine: {
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
    },
    mainPaper: {
        border: '1px solid #000000',
        'border-radius': '10px',
        padding: `${theme.spacing(2)}px ${theme.spacing(3)}px ${theme.spacing(3)}px`,
        backgroundColor: 'rgba(255,255,255,0.5)'
    },
    headerPaper: {
        border: '1px solid #000000',
        'border-radius': '10px',
        marginBottom: '16px',
        padding: `${theme.spacing(2)}px ${theme.spacing(3)}px ${theme.spacing(3)}px`,
    }
}));

interface Props {
    dummy?: any
}

const CurrentGame: React.FC<Props & RouteComponentProps> = (props: Props  & RouteComponentProps) => {

    const [_, realm, summonerName] = props.location.pathname.split('/')
    const classes = useStyles();


    const ddQueryOpts = {
        refetchOnMount: false,
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
        cacheTime: 1000 * 60 * 60 * 24 * 7
    }

    const championQuery = useQuery(ddChampionKey, fetchChampion(), ddQueryOpts)
    const ssQuery = useQuery(ddSummonerKey, fetchSummonerSpell(), ddQueryOpts)
    const rrQuery = useQuery(ddRuneKey, fetchRunesReforged(), ddQueryOpts)

    const summonerQuery = useQuery([currentGameKey, realm, summonerName], fetchCurrentGame(realm, summonerName), {
        retry: false,
        refetchOnWindowFocus: false,
        refetchOnMount: false,
    })


    if (championQuery.isLoading ||
        ssQuery.isLoading ||
        rrQuery.isLoading ||
        championQuery.isIdle ||
        ssQuery.isIdle ||
        rrQuery.isIdle) {
        return (<span>Loading DD</span>)
    }

    if (championQuery.isError || ssQuery.isError || rrQuery.isError) {
        return (<div>DD Bad response</div>)
    }

    if (summonerQuery.isLoading || summonerQuery.isIdle) {
        return <span>Loading...wut</span>
    }

    if (summonerQuery.isError) {
        const fe = summonerQuery.error as FetchError
        if (fe.res && fe.res.status == 404) {
            return <span>Not in game</span>
        }
        return <span>Alarm!!!</span>
    }

    const useData = summonerQuery.data!
    return (
        <Container className={classes.container}>
            <Paper className={classes.mainPaper} style={{width: '100%', marginTop: '20px'}}>
                <Box display='flex' flexDirection='column'>
                    <Box display='flex' justifyContent='center' alignItems='center'  >
                        <Paper className={classes.headerPaper}>
                            <Typography variant='h2'>
                                Live game of {summonerName}
                            </Typography>
                        </Paper>
                    </Box>
                    <Grid container>
                        <Grid item xs={12} sm={12} md={6}>
                            <CurrentGameTeam teamName='Blue' teamSummoners={useData.blueTeamSummoners}
                                             teamGroups={useData.blueTeamGroups}
                                             teamPositions={useData.blueTeamPositions}
                                             teamBans={useData.blueTeamBans}
                                             championData={championQuery.data!}
                                             runesReforgedData={rrQuery.data!}
                                             summonerSpellData={ssQuery.data!}/>
                        </Grid>
                        <Divider orientation='vertical' absolute={true}/>
                        <Grid item xs={12} sm={12} md={6}>
                            <CurrentGameTeam teamName='Red' teamSummoners={useData.redTeamSummoners}
                                             teamGroups={useData.redTeamGroups}
                                             teamPositions={useData.redTeamPositions}
                                             teamBans={useData.redTeamBans}
                                             championData={championQuery.data!}
                                             runesReforgedData={rrQuery.data!}
                                             summonerSpellData={ssQuery.data!}/>
                        </Grid>
                    </Grid>
                </Box>
            </Paper>
        </Container>
    );
}

export default withRouter(CurrentGame);
