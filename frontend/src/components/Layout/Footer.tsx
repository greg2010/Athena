import { makeStyles } from '@material-ui/core/styles';
import React from "react";
import {Typography} from "@material-ui/core";

const useStyles = makeStyles(theme => ({
    footerMain: {
        marginTop: 'auto',
        marginBottom: '8px',
        display: 'flex',
        width: '100%',
        alignItems: 'center',
        justifyContent: 'center'
    },
    footerText : {
        width: '50%',
        fontSize: '.625rem',
        textAlign: 'center'
    }
}));

const Footer: React.FC = () => {

    const classes = useStyles();

    return (
        <footer className={classes.footerMain}>
            <Typography variant='subtitle1' className={classes.footerText}>
                Athena isn&apos;t endorsed by Riot Games and doesn&apos;t reflect the views or opinions of Riot Games or anyone officially involved in producing or managing Riot Games properties. Riot Games, and all associated properties are trademarks or registered trademarks of Riot Games, Inc.
            </Typography>
        </footer>
    );
}

export default Footer