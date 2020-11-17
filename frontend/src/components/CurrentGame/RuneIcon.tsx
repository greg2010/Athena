import {createStyles, makeStyles, StyleRules} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import React from "react";
import {IconTooltip} from "./IconTooltip";
import {TooltipProps} from "@material-ui/core/Tooltip";
import {RunesReforgedAPI, runesReforgedDataById, RunesReforgedEntry} from "../../api/riot";
import {LoadableProp} from "../LoadableProp";
import {Skeleton} from "@material-ui/lab";

interface Props {
    boxSize: string,
    iconSize: string,
    runeStyle?: any,
    rrData?: RunesReforgedAPI
    treeId?: number
    keystoneId?: number
    ttPlacement?: TooltipProps['placement']
}

const RuneIcon: React.FC<Props> = (props: Props) => {

    const styles = {
        runeBox: {
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: props.boxSize,
            height: props.boxSize,
            borderRadius: '30%',
            border: '1px solid #bbb'
            //'background-color': '#bbb'
        },
        runeIcon: {
            ...props.runeStyle,
            width: props.iconSize,
            height: props.iconSize,
        },
    }
    const classes = makeStyles(() => createStyles(styles))();

    if (!props.rrData || !props.treeId) {
        return (<Skeleton variant='circle' className={classes.runeBox}/>)
    } else {
        const rune = runesReforgedDataById(props.rrData, props.treeId, props.keystoneId)
        if (rune) {
            // @ts-ignore
            const url = window._env_.DDRAGON_BASE_URL + 'img/' + rune.icon
            return (
                <Box className={classes.runeBox}>
                    <IconTooltip arrow title={rune.name} placement={props.ttPlacement}>
                        <img className={classes.runeIcon}
                             src={url}
                             alt={rune.name}/>
                    </IconTooltip>
                </Box>
            );
        } else {
            return (
                <Box className={classes.runeBox}>
                    <Box className={classes.runeIcon} style={{backgroundColor: '#bbb'}}/>
                </Box>)
        }
    }
}

export default RuneIcon