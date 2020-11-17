import {makeStyles} from '@material-ui/core/styles';
import Box from "@material-ui/core/Box";
import React from "react";
import {Typography} from "@material-ui/core";
import SeriesBar from "./SeriesBar";
import {ParsedRankedData} from "../../util/rankedDataUtils";
import {LoadableProp} from "../LoadableProp";
import {Skeleton} from "@material-ui/lab";

interface Props {
    rankedData?: ParsedRankedData
}

const RankedIconBlock: React.FC<Props & LoadableProp> = (props: Props & LoadableProp) => {

    if (props.isLoading) {
        return (
            <Box display='flex' flexDirection='column' alignItems='center' justifyContent='center' width='92px'>
                <Skeleton variant='rect' width='40px' height='45px'/>
                <Skeleton>
                    <Typography variant='subtitle2' style={{lineHeight: '1.5'}}>N/A League</Typography>
                </Skeleton>
                <Skeleton><Typography variant='subtitle2' style={{lineHeight: '1.5'}}>N/A LP</Typography></Skeleton>
            </Box>)
    } else {
        const styles = {}

        const classes = makeStyles(() => styles)();

        if (!props.rankedData) {
            return (
                <Box display='flex' flexDirection='column' alignItems='center' justifyContent='center' width='92px'>
                    <img width='46px' src={'Emblem_Unranked.png'}/>
                    <Typography variant='subtitle2'>Unranked</Typography>
                </Box>
            )
        } else {
            return (
                <Box display='flex' flexDirection='column' alignItems='center' justifyContent='center' width='92px'>
                    <img width='40px' src={'Emblem_' + props.rankedData.league + '.png'}/>
                    <Typography variant='subtitle2'
                                style={{lineHeight: '1.5'}}>{props.rankedData.league} {props.rankedData.division}</Typography>
                    <Typography variant='subtitle2' style={{lineHeight: '1.5'}}>{props.rankedData.lp} LP</Typography>
                    <SeriesBar series={props.rankedData.series}/>
                </Box>
            );
        }
    }
}

export default RankedIconBlock