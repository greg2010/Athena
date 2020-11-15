import {Theme, Tooltip, withStyles} from "@material-ui/core";


export const IconTooltip = withStyles((theme: Theme) => ({
    tooltip: {
        backgroundColor: theme.palette.common.black,
        fontSize: theme.typography.pxToRem(12)
    },
    arrow: {
        color: theme.palette.common.black,
    }
}))(Tooltip);