# shellcheck shell=bash
# Shared helpers for preflight.sh / prepare-frontend.sh / deploy.sh. Sourced, not run.
# Needs only bash, sed, grep and awk (plus `cf` for the foundation checks).

VARS="${VARS:-vars.yml}"
MANIFEST="${MANIFEST:-manifest.yml}"
BATCH_MANIFEST="${BATCH_MANIFEST:-manifest-batch.yml}"

# get_var <name> - the value of a top-level `name: value` line in $VARS, without quotes or a
# trailing comment. Empty if absent.
get_var() {
  tr -d '\r' < "$VARS" | sed -n "s/^$1:[[:space:]]*//p" | head -1 \
    | sed -e 's/[[:space:]]*#.*$//' -e 's/^"\(.*\)"$/\1/' -e "s/^'\(.*\)'$/\1/" -e 's/[[:space:]]*$//'
}

# manifest_vars <manifest>... - every ((variable)) those manifests actually use (comments ignored).
manifest_vars() {
  for m in "$@"; do
    grep -v '^[[:space:]]*#' "$m" | grep -o '((\([A-Za-z0-9_.-]*\)))'
  done | sed 's/[()]//g' | sort -u
}

# want_batch - prints true or false: whether the optional batch job (manifest-batch.yml) is being
# deployed. DEPLOY_BATCH in the environment (set by deploy.sh --with-batch / --without-batch) wins,
# otherwise deploy-batch from vars.yml; off by default.
want_batch() {
  case "${DEPLOY_BATCH:-$(get_var deploy-batch)}" in
    true|True|TRUE|yes|Yes|1) echo true ;;
    *) echo false ;;
  esac
}
