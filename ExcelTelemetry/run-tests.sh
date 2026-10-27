#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd -- "$(dirname -- "$0")" && pwd)
: "${JAVA_HOME:?Set JAVA_HOME to JDK25}"
: "${TELEMETRY_CP:?Set TELEMETRY_CP to Paper26.2 runtime dependencies plus mcMMO XP2}"
WORK=$(mktemp -d); trap 'rm -rf "$WORK"' EXIT
find "$ROOT/src" "$ROOT/tests" -name '*.java' | LC_ALL=C sort > "$WORK/sources"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$TELEMETRY_CP" -d "$WORK/classes" @"$WORK/sources"
for T in DamageLedgerTest EncounterTest IntegrityFlagsTest JsonlReaderTest DeathCauseTest McMmoShapingTest PerfGuardTest JsonTest TrialCompatibilityTest; do
 "$JAVA_HOME/bin/java" -cp "$WORK/classes:$TELEMETRY_CP" "com.winkreyes.exceltelemetry.$T"
done
