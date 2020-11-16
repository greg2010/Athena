import { makeStyles } from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import Typography from "@material-ui/core/Typography";
import {renderWRPercentage} from "../../util/rankedDataUtils";
import React from "react";
import Paper from "@material-ui/core/Paper";
import {Skeleton} from "@material-ui/lab";


interface Props {
    winrateAverage?: number,
    winrateRange?: number,
    teamName: string,
    isLoading: boolean
}

const TeamHeader: React.FC<Props> = (props: Props) => {

    const styles = {
        headerBox: {
            display: 'inline-flex',
            maxWidth: '540px',
            minWidth: '432px',
            width: '100%',
            justifyContent: 'space-around',
            alignItems: 'center',
            padding: '12px',
            marginBottom: '8px',
            border: '1px solid #000000',
            borderRadius: '10px',
        }
    }

    const classes = makeStyles(() => styles)();
    if (props.isLoading) {
        return (
            <Paper className={classes.headerBox}>
                <Box display='flex' alignItems='center' flexDirection='column'>
                    <Skeleton>
                        <Typography variant='subtitle1'>
                            Average team WR
                        </Typography>
                    </Skeleton>
                    <Skeleton>
                        <Typography variant='subtitle2'>
                            {props.winrateAverage ? renderWRPercentage(props.winrateAverage) + "%" : "N/A"}
                        </Typography>
                    </Skeleton>
                </Box>
                <Skeleton>
                    <Typography variant='h4' style={{fontWeight: 'bold'}}>
                        {props.teamName} Team
                    </Typography>
                </Skeleton>
                <Box display='flex' alignItems='center' flexDirection='column'>
                    <Skeleton>
                        <Typography variant='subtitle1'>
                            Team WR range
                        </Typography>
                    </Skeleton>
                    <Skeleton>
                        <Typography variant='subtitle2'>
                            {props.winrateRange ? renderWRPercentage(props.winrateRange) + "%" : "N/A"}
                        </Typography>
                    </Skeleton>
                </Box>
            </Paper>
        );

    } else {
        return (
            <Paper className={classes.headerBox}>
                <Box display='flex' alignItems='center' flexDirection='column'>
                    <Typography variant='subtitle1'>
                        Average team WR
                    </Typography>
                    <Typography variant='subtitle2'>
                        {props.winrateAverage ? renderWRPercentage(props.winrateAverage) + "%" : "N/A"}
                    </Typography>
                </Box>
                <Typography variant='h4' style={{fontWeight: 'bold'}}>
                    {props.teamName} Team
                </Typography>
                <Box display='flex' alignItems='center' flexDirection='column'>
                    <Typography variant='subtitle1'>
                        Team WR range
                    </Typography>
                    <Typography variant='subtitle2'>
                        {props.winrateRange ? renderWRPercentage(props.winrateRange) + "%" : "N/A"}
                    </Typography>
                </Box>
            </Paper>
        );
    }
}

export default TeamHeader