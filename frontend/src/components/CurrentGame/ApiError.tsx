import {makeStyles} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import React from "react";
import {Button, Divider, Typography} from "@material-ui/core";
import Paper from "@material-ui/core/Paper";

interface Props {
    reloadHook: () => void
}

const ApiError: React.FC<Props> = (props: Props) => {

    const styles = makeStyles(theme => ({}))

    const classes = styles()
    return (
                <Box display='flex' flexDirection='column' justifyContent='center' alignItems='center'>
                    <img src='amumu_error.png' alt='api-error' width='271px' height='239px' style={{marginBottom: '12px'}}/>
                    <Divider flexItem={true} variant='fullWidth' />
                    <Typography variant='h4' style={{marginBottom: '12px'}}>
                        We&apos;re experiencing server issues :&apos;(</Typography>
                    <Button onClick={props.reloadHook}>Reload</Button>
                </Box>
    )
}

export default ApiError