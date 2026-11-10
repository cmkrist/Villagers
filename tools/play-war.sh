#!/usr/bin/env bash
set -euo pipefail
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
python3 -B "$project_root/tools/prepare-war-client.py"
export VILLAGERS_RUN_DIR="$project_root/runs/four-biomes-play"
export VILLAGERS_ROLE=play
export VILLAGERS_TPS=20
export VILLAGERS_TRIAL=war
exec "$project_root/tools/run-client.sh" "$@"
