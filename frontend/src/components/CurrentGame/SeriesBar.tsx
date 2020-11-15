import { makeStyles } from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import React from "react";
import {Series} from "../../api/backend";


interface PropsCircle {
    seriesElem: string,

}

const Circle: React.FC<PropsCircle> = (props: PropsCircle) => {
    let color = ''

    if (props.seriesElem == 'W') {
        color = '#094523'
    }else if (props.seriesElem == 'L') {
        color = '#761616'
    } else {
        color = '#bbb'
    }
    const styles = {
        seriesCircle: {
            width: '12px',
            height: '12px',
            'border-radius': '50%',
            'background-color': color,
            'margin-left': '1px',
            'margin-right': '1px',
        }
    }
    const classes = makeStyles(() => styles)()
    return (
        <Box className={classes.seriesCircle} textAlign='center'/>
    )
}

interface PropsSeries {
    series: Series | null,

}

const SeriesBar: React.FC<PropsSeries> = (props: PropsSeries) => {
    if (!props.series) return null
    else {
        return (
            <Box display='flex' justifyContent='center' width={'92px'} height={'16px'}>
                {Array.prototype.map.call(props.series.progress, (ch, index) => <Circle seriesElem={ch} key={index}/>)}
            </Box>
        );
    }
}

export default SeriesBar