#!/usr/bin/env bash
set -euo pipefail
release_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
exec "$release_dir/../../tools/play.sh" "$@"
