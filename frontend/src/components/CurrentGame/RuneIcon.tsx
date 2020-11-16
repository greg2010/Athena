import {createStyles, makeStyles, StyleRules} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import React from "react";
import {IconTooltip} from "./IconTooltip";
import {TooltipProps} from "@material-ui/core/Tooltip";
import {RunesReforgedEntry} from "../../api/riot";

interface Props {
    boxSize: string,
    iconSize: string,
    runeStyle?: any,
    rune?: RunesReforgedEntry,
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

    if (props.rune) {
        // @ts-ignore
        const url = window._env_.DDRAGON_BASE_URL + 'img/' + props.rune.icon
        return (
            <Box className={classes.runeBox}>
                <IconTooltip arrow title={props.rune.name} placement={props.ttPlacement}>
                    <img className={classes.runeIcon}
                         src={url}
                         alt={props.rune.name}/>
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

export default RuneIcon