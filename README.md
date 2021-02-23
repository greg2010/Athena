# Athena
[![Build-status](https://drone.greg2010.me/api/badges/greg2010/Athena/status.svg)](https://drone.greg2010.me/greg2010/Athena/)
[![GitHub license](https://img.shields.io/github/license/greg2010/Athena.svg)](https://github.com/greg2010/Athena/blob/master/LICENSE)

Athena is an open-source AGPL-3.0 licensed companion webapp for League of Legends. 
Think op.gg, Mobalytics, or Blitz, but free. It is licensed under AGPL-3.0 and is designed to be self-hosted.
Alternatively, feel free to use the hosted version at [https://lol.krabsin.space](https://lol.krabsin.space).

Athena is written in modern, purely functional Scala and ScalaJS.
### Notable libraries
- We use [ZIO](https://github.com/zio/zio) as our effect library of choice.
- [Laminar](https://github.com/raquo/Laminar), a minimal FRP framework is used on the frontend.
- Frontend makes use of [imgproxy](https://github.com/imgproxy/imgproxy) to resize remote assets, such as Data Dragon images, on the fly.

## Usage
### Obtaining the API Key
To make use of the app, you need a valid Riot API key. To test the service locally you can obtain a temporary
developer API key on the [Riot Developer portal](https://developer.riotgames.com/).
This key needs to be renewed every day, and per Riot Terms of Service cannot be used for any publicly available services.

For more permanent deployments, request the appropriate personal or production API Key through the developer portal.
The approval period is anywhere between a couple of days and a couple of months. 
For more information on the application process refer to the official [documentation](https://developer.riotgames.com/docs/portal#product-registration_application-process).

### Running the service
The primary way of distributing Athena is docker. You need backend to interact with Riot API, and frontend to serve the webapp.
```bash
docker run -e ATHENA_RIOT_API_KEY=your_api_key \
           -e ATHENA_HTTP_PORT=8081 \
           -p 8081:8081 \
           -d \
           ghcr.io/greg2010/athena-backend:latest
docker run -e BACKEND_API_URL=http://localhost:8081 \
           -e FRONTEND_URL=http://localhost:8080 \
           -p 8080:80 \
           -d \
           ghcr.io/greg2010/athena-frontend:latest
```
The application is fully stateless, with in-memory cache for efficient API usage.

### Environment variables
The full reference of all available environment variables, as well as the default values are available in the source code. 
Please refer to the following files:
- Backend: [application.conf](https://github.com/greg2010/Athena/blob/master/backend/src/main/resources/application.conf)
- Frontend: [.env](https://github.com/greg2010/Athena/blob/master/frontend/.env)

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

### Setting up the development environment
You need a reasonably recent (>= 11) `JDK`, `sbt`, `node` and `yarn`. To start backend locally, run
```bash
sbt backend/run
```
be sure to have the environment variable set for the Riot API key. The service will not start without it.

To start a revolving environment for frontend development (akin to what create-react-app provides out of the box) run
```bash
cd frontend; ./up.sh
```
This will start the local Webpack development server on `localhost:8080`.
If a different host/port is desired, pass the respective environment variables to `up.sh`, e.g.
```bash
cd frontend; HOST=0.0.0.0 PORT=8081 ./up.sh
```
Supply any frontend application-specific environment variables in `frontend/.env.local`.
See [.env](https://github.com/greg2010/Athena/blob/master/frontend/.env) for reference.
## Versioning
At this point the development velocity makes tagged releases unfeasible. Instead, images are tagged with the commit hash.
Master branch images can be assumed relatively stable and safe for use.

## License
[AGPL-3.0](https://choosealicense.com/licenses/agpl-3.0/)