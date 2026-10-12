#!/usr/bin/env bash
# Build and test NoPhantoms using the pinned repository build.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
exec "$ROOT/mvnw" -f "$ROOT/pom.xml" -pl NoPhantoms -am verify "$@"
