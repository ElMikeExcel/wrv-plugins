#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd -- "$(dirname -- "$0")" && pwd)
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
: "${TELEMETRY_CP:?Set TELEMETRY_CP to Paper26.2 runtime dependencies plus verified mcMMO XP2 JAR}"
OUT=${1:-$ROOT/ExcelTelemetry-0.1.1.jar}
[[ "$OUT" = /* ]] || OUT="$PWD/$OUT"
WORK=$(mktemp -d); trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classes"
find "$ROOT/src" -name '*.java' | LC_ALL=C sort > "$WORK/sources"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$TELEMETRY_CP" -d "$WORK/classes" @"$WORK/sources"
cp "$ROOT/plugin.yml" "$ROOT/config.yml" "$WORK/classes/"
(cd "$WORK/classes"; find . -type f | LC_ALL=C sort | sed 's|^./||' | xargs "$JAVA_HOME/bin/jar" --create --file "$OUT" --date=2026-10-27T00:00:00Z)
sha256sum "$OUT"
