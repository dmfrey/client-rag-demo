# shellcheck shell=bash
# Shared helpers for preflight.sh / prepare-frontend.sh / deploy.sh. Sourced, not run.
# Needs only bash, sed, grep and awk (plus `cf` for the foundation checks).

VARS="${VARS:-vars.yml}"
MANIFEST="${MANIFEST:-manifest.yml}"

# get_var <name> - the value of a top-level `name: value` line in $VARS, without quotes or a
# trailing comment. Empty if absent.
get_var() {
  sed -n "s/^$1:[[:space:]]*//p" "$VARS" | head -1 \
    | sed -e 's/[[:space:]]*#.*$//' -e 's/^"\(.*\)"$/\1/' -e "s/^'\(.*\)'$/\1/" -e 's/[[:space:]]*$//'
}

# manifest_vars - every ((variable)) the manifest actually uses (comment lines ignored).
manifest_vars() {
  grep -v '^[[:space:]]*#' "$MANIFEST" | grep -o '((\([A-Za-z0-9_.-]*\)))' | sed 's/[()]//g' | sort -u
}
