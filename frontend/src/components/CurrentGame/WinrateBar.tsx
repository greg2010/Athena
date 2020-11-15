import { makeStyles } from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import React from 'react';

interface Props {
    winrate: number,
}

const WinrateBar: React.FC<Props> = (props: Props) => {

    const colorLow = '#761616', colorHigh = '#094523';
    let wrHex = '#000'
    if (props.winrate >= 0.5) {
        wrHex = colorHigh
    } else {
        wrHex = colorLow
    }

    const styles = {
        winrateMain: {
            width: '100px',
            height: '10px',
            border: '1px solid ' + `${wrHex}`,
        },
        winrateFiller: {
            height: '100%',
            width: `${props.winrate * 100}%`,
            backgroundColor:`${wrHex}`,
            borderRadius: 'inherit',
        }
    }

    const classes = makeStyles(() => styles)();

    return (
        <Box className={classes.winrateMain}>
            <Box className={classes.winrateFiller}/>
        </Box>
    );
}

export default WinrateBar