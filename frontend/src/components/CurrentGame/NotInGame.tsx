import {makeStyles} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import React from "react";
import {Button, Divider, Typography} from "@material-ui/core";
import {RouteComponentProps, withRouter} from "react-router";

interface Props {
    realm: string,
    name: string
    reloadHook: () => void
}

const NotInGame: React.FC<Props & RouteComponentProps> = (props: Props & RouteComponentProps) => {

    const styles = makeStyles(theme => ({}))


    const classes = styles()
    return (
        <Box display='flex' flexDirection='column' justifyContent='center' alignItems='center'>
            <img src='blitzcrank_logo.png' alt='api-error' width='256px' height='256px'
                 style={{marginBottom: '12px'}}/>
            <Divider flexItem={true} variant='fullWidth'/>
            <Typography variant='h4' style={{marginBottom: '12px'}}>
                Summoner {props.name} is not currently in game.</Typography>
            <Button onClick={props.reloadHook}>Reload</Button>
        </Box>
    )
}

export default withRouter(NotInGame)