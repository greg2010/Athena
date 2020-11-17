import {makeStyles, Theme} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import {TooltipProps} from '@material-ui/core/Tooltip';
import {IconTooltip} from "./IconTooltip";
import React from "react";
import {ChampionAPI, championDataById, ChampionEntry} from "../../api/riot";
import {LoadableProp} from "../LoadableProp";
import {Skeleton} from "@material-ui/lab";


interface Props {
    beforeStyles?: any,
    styles?: any,
    size: string,
    ttPlacement?: TooltipProps['placement'],
    championData?: ChampionAPI,
    championId?: number
}


const ChampionIcon: React.FC<Props> = (props: Props) => {
    if (!props.championData || !props.championId) {
        return (<Skeleton variant='rect' height={props.size} width={props.size}
                          style={{borderRadius: '10%', flexShrink: 0}}/>)
    } else {

        let srcUrl = 'placeholder_champion.png'
        let championName = 'Unknown'
        const champion = championDataById(props.championData, props.championId)
        if (champion) {
            // @ts-ignore
            srcUrl = window._env_.DDRAGON_BASE_URL + window._env_.DDRAGON_VERSION + '/img/champion/' + champion.id + '.png'
            championName = champion.name
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
}

export default ChampionIcon