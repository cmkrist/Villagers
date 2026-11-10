#!/usr/bin/env bash
set -euo pipefail
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
source "$project_root/tools/java-env.sh"
villagers_select_java || exit 1
if [[ -z "${DISPLAY:-}" && -z "${WAYLAND_DISPLAY:-}" ]]; then
    echo "A visible desktop session is required for Minecraft testing." >&2
    exit 1
fi
settings_output=$(python3 -B "$project_root/tools/runtime_config.py" --shell-lines)
mapfile -t runtime_settings <<< "$settings_output"
export VILLAGERS_RUN_DIR="${runtime_settings[0]}"
export VILLAGERS_ROLE="${runtime_settings[1]}"
export VILLAGERS_TPS="${runtime_settings[2]}"
# The descriptor remains open in Gradle for the entire client lifetime.
if [[ -L "$VILLAGERS_RUN_DIR/.villagers-client.lock" ]]; then
    echo "Launcher lock must not be a symlink." >&2
    exit 1
fi
exec 9>"$VILLAGERS_RUN_DIR/.villagers-client.lock"
if ! flock -n 9; then
    echo "A client or preparation already owns this game directory: $VILLAGERS_RUN_DIR" >&2
    exit 1
fi
python3 -B "$project_root/tools/runtime_config.py" --record-launch > /dev/null
cd "$project_root"
if [[ "$VILLAGERS_RUN_DIR" == "$project_root/run" ]]; then
    exec ./gradlew runClient "$@"
fi
exec ./gradlew --project-cache-dir "$VILLAGERS_RUN_DIR/.gradle" runClient "$@"
