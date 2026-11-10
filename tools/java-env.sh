# Sourced by the launcher scripts. Selects a JDK 25 and exports JAVA_HOME and PATH.
# Order: explicit overrides, PATH, the local companion toolchain, then the original local install.
# Every selection must really be Java 25.

villagers_java_feature() {
    "$1/bin/java" -XshowSettings:properties -version 2>&1 \
        | sed -n 's/^ *java\.specification\.version = //p' | head -n 1
}

villagers_select_java() {
    local candidate
    if [[ -n "${VILLAGERS_JAVA_HOME:-}" ]]; then
        if [[ ! -x "$VILLAGERS_JAVA_HOME/bin/java" ]]; then
            echo "VILLAGERS_JAVA_HOME has no bin/java: $VILLAGERS_JAVA_HOME" >&2
            return 1
        fi
        if [[ "$(villagers_java_feature "$VILLAGERS_JAVA_HOME")" != "25" ]]; then
            echo "VILLAGERS_JAVA_HOME must point to JDK 25: $VILLAGERS_JAVA_HOME" >&2
            return 1
        fi
        export JAVA_HOME="$VILLAGERS_JAVA_HOME"
    else
        local path_java=""
        if command -v java > /dev/null 2>&1; then
            path_java=$(dirname -- "$(dirname -- "$(readlink -f -- "$(command -v java)")")")
        fi
        unset -v selected
        for candidate in "${JAVA_HOME:-}" "$path_java" "$HOME"/Games/VanillaPerfectedCompanion/ai/toolchain/* "/home/niko/.internal/minecraft-tools/jdk-25.0.5+8"; do
            if [[ -n "$candidate" && -x "$candidate/bin/java" && "$(villagers_java_feature "$candidate")" == "25" ]]; then
                selected="$candidate"
                break
            fi
        done
        if [[ -z "${selected:-}" ]]; then
            echo "JDK 25 not found: set VILLAGERS_JAVA_HOME (or JAVA_HOME) to a JDK 25 directory." >&2
            return 1
        fi
        export JAVA_HOME="$selected"
    fi
    export PATH="$JAVA_HOME/bin:$PATH"
}
