import {makeStyles, StyleRules} from '@material-ui/core/styles';
import React from "react";
import {IconTooltip} from "./IconTooltip";
import {TooltipProps} from "@material-ui/core/Tooltip";
import {SummonerSpellAPI, summonerSpellDataById, SummonerSpellEntry} from "../../api/riot";
import {Box} from "@material-ui/core";
import {Skeleton} from "@material-ui/lab";

interface Props {
    styles?: StyleRules
    size: string
    ssData?: SummonerSpellAPI
    summonerSpellId?: number
    ttPlacement?: TooltipProps['placement']
}

const SummonerSpellIcon: React.FC<Props> = (props: Props) => {

    const styles = {
        sumSpellIcon: {
            ...props.styles,
            width: props.size,
            height: props.size,
            borderRadius: '10%',
        },
    }

    const classes = makeStyles(() => styles)();

    if (!props.summonerSpellId || !props.ssData) {
        return (<Skeleton variant='rect' className={classes.sumSpellIcon}/>)
    } else {
        const ss = summonerSpellDataById(props.ssData, props.summonerSpellId)
        if (ss) {
            // @ts-ignore
            const url = window._env_.DDRAGON_BASE_URL + window._env_.DDRAGON_VERSION + '/img/spell/' + ss.id + '.png'
            return (
                <IconTooltip arrow title={ss.name} placement={props.ttPlacement}>
                    <img className={classes.sumSpellIcon}
                         src={url}
                         alt={ss.name}/>
                </IconTooltip>
            );
        } else {
            return (<Box className={classes.sumSpellIcon} style={{backgroundColor: '#bbb'}}/>)
        }
    }
}

export default SummonerSpellIcon