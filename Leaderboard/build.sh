#!/usr/bin/env bash
# Build and test Leaderboard using the pinned repository build.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
exec "$ROOT/mvnw" -f "$ROOT/pom.xml" -pl Leaderboard -am verify "$@"
