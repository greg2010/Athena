import {makeStyles, StyleRules} from '@material-ui/core/styles';
import React from "react";
import {IconTooltip} from "./IconTooltip";
import {TooltipProps} from "@material-ui/core/Tooltip";
import {SummonerSpellEntry} from "../../api/riot";
import {Box} from "@material-ui/core";

interface Props {
    styles?: StyleRules
    size: string
    summonerSpell: SummonerSpellEntry | undefined
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

    if (props.summonerSpell) {
        // @ts-ignore
        const url = window._env_.DDRAGON_BASE_URL + window._env_.DDRAGON_VERSION + '/img/spell/' + props.summonerSpell.id + '.png'
        return (
            <IconTooltip arrow title={props.summonerSpell.name} placement={props.ttPlacement}>
                <img className={classes.sumSpellIcon}
                     src={url}
                     alt={props.summonerSpell.name}/>
            </IconTooltip>
        );
    } else {
        return (<Box className={classes.sumSpellIcon} style={{backgroundColor: '#bbb'}}/>)
    }
}

export default SummonerSpellIcon