#!/usr/bin/env bash

# One-off to make sure webpack doesn't crash on start
(cd ../ && sbt frontend/fastOptJS)

# Prepare traps to kill the processes when parent exits
trap 'kill %1; kill %2' SIGINT
# Start sbt watch with home in workdir
(cd ../ && sbt ~frontend/fastOptJS --color=always) | sed -e 's/^/[sbt] /' &
# Start webpack watch
yarn install && FORCE_COLOR=true yarn run start | sed -e 's/^/[webpack] /' &
# Watch .env.local, run on file change
./env.sh &&
  inotifywait -q -m -e close_write .env.local .env |
  while read -r filename event; do
    ./env.sh
  done
