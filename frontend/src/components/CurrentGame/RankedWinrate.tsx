import {createStyles, makeStyles} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import {Typography} from "@material-ui/core";
import WinrateBar from "./WinrateBar";
import React from "react";
import * as rankedUtils from "../../util/rankedDataUtils";
import {ParsedRankedData} from "../../util/rankedDataUtils";
import {QueueType} from "../../api/backend";



interface Props {
    rankedData?: ParsedRankedData
}

const RankedWinrate: React.FC<Props> = (props: Props) => {

    const styles = createStyles({
        winrateBlock: {
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center'
        },
    })

    const classes = makeStyles(() => styles)();

    if (!props.rankedData) return (
        <Box className={classes.winrateBlock}>
            <Typography variant='subtitle1'>No ranked data</Typography>
        </Box>
    )
    else {
        let queueName = ''
        if (props.rankedData.queueType == QueueType.RANKED_SOLO_5x5) {
            queueName = 'Solo/Duo'
        } else {
            queueName = 'Flex'
        }
        return (
            <Box className={classes.winrateBlock}>
                <WinrateBar winrate={props.rankedData.winrate}/>
                <Typography variant='subtitle2' style={{flex: 1, textAlign: 'right', fontSize: 12}}>
                    {queueName} WR: {rankedUtils.renderWRPercentage(props.rankedData.winrate)}%
                </Typography>
                <Typography variant='subtitle2' style={{flex: 1, textAlign: 'right', fontSize: 12}}>
                    ({props.rankedData.totalGames} Played)
                </Typography>
            </Box>
        );
    }
}

export default RankedWinrate