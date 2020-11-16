import {makeStyles, Theme} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import {TooltipProps} from '@material-ui/core/Tooltip';
import {IconTooltip} from "./IconTooltip";
import React from "react";
import {ChampionEntry} from "../../api/riot";


interface Props {
    beforeStyles?: any,
    styles?: any,
    size: string,
    ttPlacement?: TooltipProps['placement'],
    champion?: ChampionEntry
}


const ChampionIcon: React.FC<Props> = (props: Props) => {
    let srcUrl = 'placeholder_champion.png'
    let championName = 'Unknown'
    if (props.champion) {
        // @ts-ignore
        srcUrl = window._env_.DDRAGON_BASE_URL + window._env_.DDRAGON_VERSION + '/img/champion/' + props.champion.id + '.png'
        championName = props.champion.name
    }

    const styles = (theme: Theme) => ({
        champIcon: {
            '&::before': {
                ...props.beforeStyles,
                content: '""',
                position: 'absolute',
                width: '100%',
                height: '100%',
                backgroundImage: `url(${srcUrl})`,
                backgroundSize: '100% 100%',
                backgroundPosition: 'center center',
                backgroundRepeat: 'no-repeat',
                borderRadius: '10%',
            },

            ...props.styles,
            width: props.size,
            height: props.size,
            position: 'relative',
            borderRadius: '10%',
            flexShrink: 0
        }
    })

    const classes = makeStyles(styles)();
    return (
        <IconTooltip arrow title={championName} placement={props.ttPlacement}>
            <Box className={classes.champIcon}/>
        </IconTooltip>
    );
}

export default ChampionIcon