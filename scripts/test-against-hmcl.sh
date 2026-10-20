#!/usr/bin/env bash
#
# HMCL Download Agent
#
# Verifies the agent against a real HMCL build. The probe forces a China-like
# environment (Asia/Shanghai time zone and a failing HTTP proxy) and asserts
# that the unmodified build reports a mainland-China machine/network while the
# agent-patched build keeps the machine check true and disables only the
# network download gate.
#
# Usage:
#   ./scripts/test-against-hmcl.sh                 # latest upstream release
#   ./scripts/test-against-hmcl.sh /path/to/HMCL.jar

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ $# -gt 0 ]]; then
    HMCL_JAR="$1"
else
    DOWNLOAD_DIR="$ROOT/build/hmcl-latest"
    mkdir -p "$DOWNLOAD_DIR"
    read -r TAG URL < <(
        curl -fsSL "https://api.github.com/repos/HMCL-dev/HMCL/releases?per_page=1" |
            python3 -c '
import json
import sys

release = json.load(sys.stdin)[0]
asset = next(a for a in release["assets"] if a["name"].endswith(".jar"))
print(release["tag_name"], asset["browser_download_url"])
'
    )
    HMCL_JAR="$DOWNLOAD_DIR/HMCL-$TAG.jar"
    if [[ ! -f "$HMCL_JAR" ]]; then
        echo "Downloading HMCL $TAG..."
        curl -fL --progress-bar -o "$HMCL_JAR" "$URL"
    fi
fi

if [[ ! -f "$HMCL_JAR" ]]; then
    echo "HMCL jar not found: $HMCL_JAR" >&2
    exit 2
fi

HMCL_JAR="$(cd "$(dirname "$HMCL_JAR")" && pwd)/$(basename "$HMCL_JAR")"

control="$(./gradlew --quiet --console=plain probeControl -PhmclJar="$HMCL_JAR")"
echo "Unpatched: $control"
if ! grep -q 'machine=true network=true' <<<"$control"; then
    echo "FAIL: expected machine=true network=true without the agent" >&2
    exit 1
fi

patched="$(./gradlew --quiet --console=plain probePatched -PhmclJar="$HMCL_JAR")"
echo "Patched:   $patched"
if ! grep -q 'machine=true network=false' <<<"$patched"; then
    echo "FAIL: expected machine=true network=false with the agent" >&2
    exit 1
fi

echo "PASS: agent disables the network download gate in $(basename "$HMCL_JAR")"
