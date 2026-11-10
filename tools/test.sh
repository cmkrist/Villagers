#!/usr/bin/env bash
set -euo pipefail
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
source "$project_root/tools/java-env.sh"
villagers_select_java || exit 1
cd "$project_root"
python3 -B -m unittest discover -s tools/tests
exec ./gradlew build test "$@"
