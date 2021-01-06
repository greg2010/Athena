#!/usr/bin/env bash

# Start sbt watch with home in workdir
(cd /workspace/bindpoint && sbt -Duser.home=. -Dsbt.global.base=.sbt -Dsbt.boot.directory=.sbt -Dsbt.ivy.home=.ivy2 ~frontend/fastOptJS &)
# Start webpack watch
(cd /workspace/bindpoint/frontend && npm run build:dev &)

# Watch .env.local, run on file change
cd /workspace/bindpoint/frontend &&
inotifywait -q -m -e close_write .env.local .env |
while read -r filename event; do
  ./env.sh         # or "./$filename"
done