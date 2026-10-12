#!/usr/bin/env bash
# Build and test maintained BedrockMenu source using the pinned repository build.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
exec "$ROOT/mvnw" -f "$ROOT/pom.xml" -pl BedrockMenu -am verify "$@"
