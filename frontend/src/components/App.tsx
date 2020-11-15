import React from 'react';
import AppNavbar from "./Layout/AppNavBar";
import Landing from "./Landing/Landing";
import CssBaseline from "@material-ui/core/CssBaseline";
import {Route, Switch} from "react-router";
import {HashRouter} from "react-router-dom";
import CurrentGame from "./CurrentGame/CurrentGame";
import Box from "@material-ui/core/Box";
import {makeStyles} from "@material-ui/core/styles";
import Footer from "./Layout/Footer";

const useStyles = makeStyles(theme => ({
    outerWrapper: {
        '&::before': {
            content: '""',
            backgroundImage: 'linear-gradient(white, #999)',
            //backgroundImage: 'url(jhin-bg.jpg)',
            backgroundSize: 'cover',
            backgroundPosition: 'center center',
            backgroundRepeat: 'no-repeat',
            //opacity: 0.5,
            height: '100vh',
            width: '100vw',
            position: 'absolute',
            zIndex: '-1',
        },
        display: 'flex',
        flexDirection: 'column',
        height: '100vh',
        width: '100vw',
        flex: 1
    }
}));

const App: React.FC = () => {
    const classes = useStyles();

      return (
          <React.Fragment>
              <CssBaseline />
              <HashRouter>
                  <Switch>
                      <Route exact path="/" >
                          <Box className={classes.outerWrapper}>
                          <AppNavbar showSearch={false}/>
                              <Landing/>
                              <Footer/>
                          </Box>
                      </Route>
                      <Route>
                          <Box className={classes.outerWrapper}>
                          <AppNavbar showSearch={true}/>
                              <CurrentGame/>
                              <Footer/>
                          </Box>
                      </Route>
                  </Switch>
              </HashRouter>
          </React.Fragment>
      );
}

export default App;
