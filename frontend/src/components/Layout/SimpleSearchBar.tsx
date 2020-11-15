import React from 'react';
import IconButton from '@material-ui/core/IconButton';
import InputBase from '@material-ui/core/InputBase';
import { fade, makeStyles } from '@material-ui/core/styles';
import SearchIcon from '@material-ui/icons/Search';
import Paper from "@material-ui/core/Paper";
import NativeSelect from "@material-ui/core/NativeSelect";
import {RouteComponentProps, withRouter} from "react-router";




const searchBarLanding = makeStyles((theme) => ({
    search: {
        position: 'relative',
        borderRadius: theme.shape.borderRadius,
        backgroundColor: fade(theme.palette.common.white, 0.15),
        '&:hover': {
            backgroundColor: fade(theme.palette.common.white, 0.25),
        },
        marginLeft: 0,
        width: '100%',
        [theme.breakpoints.up('sm')]: {
            marginLeft: theme.spacing(1),
            width: 'auto',
        },
    },
    searchIcon: {
        height: '100%',
        position: 'absolute',
        pointerEvents: 'none',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
    searchDropdown: {
        margin: theme.spacing(1),
        height: '100%',
        minWidth: '20%',
    },
    inputRoot: {
        padding: theme.spacing(0, 2),
        color: 'inherit',
        width: '70%'
    },
}));

const searchBarAppBar = makeStyles((theme) => ({
    search: {
        position: 'relative',
        borderRadius: theme.shape.borderRadius,
        backgroundColor: fade(theme.palette.common.white, 0.15),
        '&:hover': {
            backgroundColor: fade(theme.palette.common.white, 0.25),
        },
        marginLeft: 0,
        width: '100%',
        [theme.breakpoints.up('sm')]: {
            marginLeft: theme.spacing(1),
            width: 'auto',
        },
    },
    searchIcon: {
        padding: theme.spacing(0, 2),
        height: '100%',
        position: 'absolute',
        pointerEvents: 'none',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
    inputRoot: {
        color: 'inherit',
    },
    inputInput: {
        padding: theme.spacing(1, 1, 1, 0),
        // vertical padding + font size from searchIcon
        paddingLeft: `calc(1em + ${theme.spacing(4)}px)`,
        transition: theme.transitions.create('width'),
        width: '100%',
        [theme.breakpoints.up('sm')]: {
            width: '12ch',
            '&:focus': {
                width: '20ch',
            },
        },
    },
}));

interface Props {
    isLanding: boolean
}

const SimpleSearchBar: React.FC<Props & RouteComponentProps> = (props: Props & RouteComponentProps) => {
    let classes: any = {};
    if (props.isLanding) {
        classes = searchBarLanding();
    } else {
        classes = searchBarAppBar();
    }

    const [state, setState] = React.useState({
        realm: 'NA1',
        summonerName: ''
    });

    const handleDropdownChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
        setState({
            ...state,
            'realm': event.target.value,
        });
    };

    const handleSubmit = (event: React.FormEvent) => {
        event.preventDefault()
        if (state.summonerName) {
            const path = '/' + state.realm + '/' + state.summonerName;
            props.history.push(path);
        }
    }
    const handleTextChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setState({
            ...state,
            summonerName: event.target.value,
        });
    }

    const realms = [
        ['NA1', 'NA'],
        ['EUW1', 'EUW'],
        ['EUN1', 'EUNE'],
        ['RU', 'RU'],
        ['OC1', 'OC'],
        ['BR1', 'BR'],
        ['KR1', 'KR'],
        ['LA1', 'LA1'],
        ['LA2', 'LA2'],
        ['TR1', 'TR'],
        ['JP1', 'JP'],
    ];

    return (
        <Paper>
            <form onSubmit={handleSubmit}>
                <InputBase
                    placeholder="Enter a summoner name..."
                    classes={{
                        root: classes.inputRoot,
                        input: classes.inputInput,
                    }}
                    onChange={handleTextChange}
                    inputProps={{ 'aria-label': 'search' }}/>
                    <NativeSelect
                        className={
                            classes.searchDropdown}
                        id="dropdown-id"
                        value={state.realm}
                        onChange={handleDropdownChange}>
                        {realms.map(realm => <option value={realm[0]} key={realm[0]}>{realm[1]}</option>)}
                    </NativeSelect>
                <IconButton type="submit" aria-label="search">
                    <SearchIcon />
                </IconButton>
            </form>
        </Paper>
    );
}

export default withRouter(SimpleSearchBar);