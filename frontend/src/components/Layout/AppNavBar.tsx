import React from 'react';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import { fade, makeStyles } from '@material-ui/core/styles';
import SimpleSearchBar from "./SimpleSearchBar";
import {NavLink} from "react-router-dom";
import {Box} from "@material-ui/core";

const useStyles = makeStyles((theme) => ({
    root: {
    },
    menuButton: {
        marginRight: theme.spacing(2),
    },
    title: {
        flexGrow: 1,
        display: 'none',
        [theme.breakpoints.up('sm')]: {
            display: 'block',
        },
    },
}));

interface Props {
    showSearch: boolean
}

const AppNavBar: React.FC<Props> = (props: Props) => {
    const classes = useStyles();

    return (
        <div className={classes.root}>
            <AppBar position="static">
                <Toolbar>
                    <Typography className={classes.title} variant="h6" noWrap>
                        <NavLink style={{color: 'White', textDecoration: 'none'}} to='/'>
                            Athena
                        </NavLink>
                    </Typography>
                    <Box  hidden={!props.showSearch}>
                        <SimpleSearchBar isLanding={false}/>
                    </Box>
                </Toolbar>
            </AppBar>
        </div>
    );
}

export default AppNavBar
