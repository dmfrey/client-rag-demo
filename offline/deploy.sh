#!/usr/bin/env bash
# One command to deploy: point the frontend at your backend, check everything, then cf push.
#
#   ./deploy.sh frontend-dist.tar.gz    first time (extracts the frontend bundle)
#   ./deploy.sh                         later pushes (frontend already extracted and prepared)
#
# The apps are pushed in manifest order - backend first. Let the backend start before anything that
# depends on its database migrations: sharepoint-batch must never run against a database the
# backend has not migrated at least once.
set -eu
cd "$(dirname "$0")" || exit 1

./prepare-frontend.sh "$@"
echo
./preflight.sh
echo
echo "Pushing (cf push -f manifest.yml --vars-file vars.yml)..."
cf push -f manifest.yml --vars-file vars.yml
