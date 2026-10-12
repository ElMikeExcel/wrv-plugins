#!/usr/bin/env bash
# Build and test WealthBoard using the pinned repository build.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
exec "$ROOT/mvnw" -f "$ROOT/pom.xml" -pl WealthBoard -am verify "$@"
