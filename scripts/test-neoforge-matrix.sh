#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLEW="$PROJECT_ROOT/gradlew"
LOG_ROOT="${NEOFORGE_MATRIX_LOG_ROOT:-$PROJECT_ROOT/build/neoforge-matrix-logs/$(date +%Y%m%d-%H%M%S)}"

# One current NeoForge build is selected for each Minecraft version published by
# Modrinth in the requested range. Update this table when a newer target build
# should become the compatibility baseline. The 1.21.2, 1.21.6, and 1.21.7
# lines currently use the latest published beta builds.
TARGETS=(
    "1.21:21.0.167"
    "1.21.1:21.1.253"
    "1.21.2:21.2.1-beta"
    "1.21.3:21.3.97"
    "1.21.4:21.4.158"
    "1.21.5:21.5.98"
    "1.21.6:21.6.20-beta"
    "1.21.7:21.7.25-beta"
    "1.21.8:21.8.54"
)

if [[ ! -x "$GRADLEW" ]]; then
    echo "Gradle wrapper is missing or not executable: $GRADLEW" >&2
    exit 2
fi

mkdir -p "$LOG_ROOT"

declare -a failed_targets=()
declare -a passed_targets=()

echo "NeoForge compatibility test matrix"
echo "Project: $PROJECT_ROOT"
echo "Logs:    $LOG_ROOT"
echo

for target in "${TARGETS[@]}"; do
    minecraft_version="${target%%:*}"
    neo_version="${target##*:}"
    safe_version="${minecraft_version//./_}"
    log_file="$LOG_ROOT/minecraft-$safe_version-neoforge-$neo_version.log"

    echo "=== Minecraft $minecraft_version / NeoForge $neo_version ==="

    set +e
    (
        cd "$PROJECT_ROOT"
        "$GRADLEW" test \
            --no-daemon \
            --no-configuration-cache \
            -Pminecraft_version="$minecraft_version" \
            -Pneo_version="$neo_version"
    ) 2>&1 | tee "$log_file"
    status="${PIPESTATUS[0]}"
    set -e

    if [[ "$status" -eq 0 ]]; then
        passed_targets+=("$minecraft_version / $neo_version")
        echo "PASS: Minecraft $minecraft_version / NeoForge $neo_version"
    else
        failed_targets+=("$minecraft_version / $neo_version")
        echo "FAIL: Minecraft $minecraft_version / NeoForge $neo_version"
        echo "      See $log_file"
    fi
    echo
done

echo "=== Matrix summary ==="
echo "Passed: ${#passed_targets[@]}"
for target in "${passed_targets[@]}"; do
    echo "  PASS $target"
done

echo "Failed: ${#failed_targets[@]}"
for target in "${failed_targets[@]}"; do
    echo "  FAIL $target"
done

if ((${#failed_targets[@]} > 0)); then
    exit 1
fi

echo "All NeoForge matrix targets passed."
