#!/usr/bin/env bash
# Build and test WRVPlayerMap using the pinned repository build.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
exec "$ROOT/mvnw" -f "$ROOT/pom.xml" -pl WRVPlayerMap -am verify "$@"
