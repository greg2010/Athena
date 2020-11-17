import React, {PropsWithChildren} from 'react';
import {makeStyles} from '@material-ui/core/styles';
import Container from "@material-ui/core/Container";
import Typography from "@material-ui/core/Typography";
import Paper from "@material-ui/core/Paper";
import Box from "@material-ui/core/Box";
import {RouteComponentProps, withRouter} from "react-router";
import Divider from "@material-ui/core/Divider";
import Grid from "@material-ui/core/Grid";
import CurrentGameTeam from "./CurrentGameTeam";
import {currentGameKey, fetchCurrentGame} from "../../api/backend";
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
import ApiError from "./ApiError";
import NotInGame from "./NotInGame";

const useStyles = makeStyles(theme => ({
    container: {
        marginTop: theme.spacing(2),
        marginBottom: theme.spacing(2),
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
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
        padding: `${theme.spacing(1)}px ${theme.spacing(2)}px ${theme.spacing(2)}px`,
    }
}));

interface Props {
    dummy?: number
}

const CurrentGame: React.FC<Props & RouteComponentProps> = (props: Props  & RouteComponentProps) => {

    const [_, realm, summonerName] = props.location.pathname.split('/')
    const classes = useStyles();

    const OuterWrapper = (props: { children?: React.ReactNode }) => {

        return (
            <Container className={classes.container}>
                <Paper className={classes.mainPaper} style={{width: '100%', marginTop: '20px'}}>
                    {props.children}
                </Paper>
            </Container>)
    }


    const ddQueryOpts = {
        refetchOnMount: false,
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
        cacheTime: 1000 * 60 * 60 * 24 * 7
    }

    const backendQueryOpts = {
        retry: false,
        refetchOnWindowFocus: false,
        refetchOnMount: false,
    }


    const championQuery = useQuery(ddChampionKey, fetchChampion(), ddQueryOpts)
    const ssQuery = useQuery(ddSummonerKey, fetchSummonerSpell(), ddQueryOpts)
    const rrQuery = useQuery(ddRuneKey, fetchRunesReforged(), ddQueryOpts)
    const summonerQuery = useQuery([currentGameKey, realm, summonerName], fetchCurrentGame(realm, summonerName), backendQueryOpts)

    const fetchAll = () => {
        championQuery.refetch()
        ssQuery.refetch()
        rrQuery.refetch()
        return summonerQuery.refetch()
    }

    if (championQuery.isError || ssQuery.isError || rrQuery.isError) return (<OuterWrapper><ApiError
        reloadHook={fetchAll}/></OuterWrapper>)


    if (summonerQuery.isError) {
        const fe = summonerQuery.error as FetchError
        if (fe.res && fe.res.status == 404) {
            return (<OuterWrapper><NotInGame realm={realm} name={summonerName}
                                             reloadHook={summonerQuery.refetch}/></OuterWrapper>)
        }
        return (<OuterWrapper><ApiError reloadHook={fetchAll}/></OuterWrapper>)
    }

    return (
        <OuterWrapper>
            <Box display='flex' flexDirection='column'>
                <Box display='flex' justifyContent='center' alignItems='center'>
                    <Paper className={classes.headerPaper}>
                        <Typography variant='h4'>
                            Live game of {summonerName}
                        </Typography>
                    </Paper>
                </Box>
                <Grid container>
                    <Grid item xs={12} sm={12} md={6}>
                        <CurrentGameTeam teamName='Blue'
                                         teamSummoners={summonerQuery.data?.blueTeamSummoners}
                                         teamGroups={summonerQuery.data?.blueTeamGroups}
                                         teamPositions={summonerQuery.data?.blueTeamPositions}
                                         teamBans={summonerQuery.data?.blueTeamBans}
                                         championData={championQuery.data}
                                         runesReforgedData={rrQuery.data}
                                         summonerSpellData={ssQuery.data}/>
                    </Grid>
                    <Divider orientation='vertical' absolute={true}/>
                    <Grid item xs={12} sm={12} md={6}>
                        <CurrentGameTeam teamName='Red'
                                         teamSummoners={summonerQuery.data?.redTeamSummoners}
                                         teamGroups={summonerQuery.data?.redTeamGroups}
                                         teamPositions={summonerQuery.data?.redTeamPositions}
                                         teamBans={summonerQuery.data?.redTeamBans}
                                         championData={championQuery.data}
                                         runesReforgedData={rrQuery.data}
                                         summonerSpellData={ssQuery.data}/>
                    </Grid>
                </Grid>
            </Box>
        </OuterWrapper>
    );
}

export default withRouter(CurrentGame);
