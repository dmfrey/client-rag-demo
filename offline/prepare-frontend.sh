#!/usr/bin/env bash
# Points the pre-built frontend bundle at your backend.
#
# The bundle on the GitHub Release (frontend-dist.tar.gz) is built with a placeholder instead of a
# backend URL, so one bundle works for any site; this swaps the placeholder for https://<backend-route>
# (from vars.yml) in the bundle's JavaScript. No Node/npm needed.
#
#   ./prepare-frontend.sh frontend-dist.tar.gz   extract the bundle into frontend-dir, then point it
#   ./prepare-frontend.sh                        point the already-extracted frontend-dir
#
# To change the backend route later, run it again WITH the tarball (the placeholder is gone from an
# already-prepared copy, so the tarball has to be extracted afresh).
set -eu
cd "$(dirname "$0")" || exit 1
. ./lib.sh

[ -f "$VARS" ] || { echo "No $VARS - copy vars.yml.example to vars.yml and edit it." >&2; exit 1; }
backend=$(get_var backend-route)
dir=$(get_var frontend-dir)
[ -n "$backend" ] || { echo "backend-route is not set in $VARS" >&2; exit 1; }
[ -n "$dir" ] || { echo "frontend-dir is not set in $VARS" >&2; exit 1; }
PLACEHOLDER='__RUNTIME_API_BASE_URL__'

if [ $# -ge 1 ]; then
  [ -f "$1" ] || { echo "Bundle not found: $1" >&2; exit 1; }
  mkdir -p "$dir"
  tar -xzf "$1" -C "$dir"
  echo "Extracted $1 into $dir"
fi
[ -d "$dir" ] || { echo "$dir does not exist - pass the frontend-dist.tar.gz bundle as an argument." >&2; exit 1; }

files=$(grep -rl "$PLACEHOLDER" "$dir" 2>/dev/null || true)
if [ -z "$files" ]; then
  if grep -rqF "https://$backend" "$dir" 2>/dev/null; then
    echo "$dir already points at https://$backend - nothing to do."
    exit 0
  fi
  echo "No $PLACEHOLDER found in $dir and it does not mention https://$backend either." >&2
  echo "It was prepared for another backend route: re-run with the frontend-dist.tar.gz bundle." >&2
  exit 1
fi

count=0
for f in $files; do
  sed -i.bak "s|$PLACEHOLDER|https://$backend|g" "$f" && rm -f "$f.bak"
  count=$((count + 1))
done
echo "Pointed the frontend at https://$backend ($count file(s) updated)."
