#!/usr/bin/env bash
set -euo pipefail
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
export VILLAGERS_RUN_DIR="$project_root/runs/play"
export VILLAGERS_ROLE=play
export VILLAGERS_TPS=20
export VILLAGERS_TRIAL=village
exec "$project_root/tools/run-client.sh" "$@"
