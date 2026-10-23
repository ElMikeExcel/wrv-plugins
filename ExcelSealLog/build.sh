#!/usr/bin/env bash
# ExcelSealLog local build: JDK 25, --release 21, javac against Paper API (26.2.133 scratch libs; API-compatible with 26.3).
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd); PROJ=$HERE/project
JDK=${JDK:-/workspace/wrv019/jdk25}; L=${LIBS:-/workspace/acro/scratch/libraries}
JUNIT=${JUNIT:-/workspace/excelshops13/libs/junit-platform-console-standalone-1.11.4.jar}
CP=$(find "$L" -name "*.jar" | tr '\n' ':')
OUT=$HERE/build-out; rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/test-classes"
find "$PROJ/src" -name "*.java" | sort > "$OUT/src.txt"
"$JDK/bin/javac" --release 21 -proc:none -Xlint:-options -cp "$CP" -d "$OUT/classes" @"$OUT/src.txt"
cp "$PROJ/plugin.yml" "$PROJ/LICENSE" "$OUT/classes/"
find "$PROJ/tests" -name "*.java" | sort > "$OUT/test.txt"
"$JDK/bin/javac" --release 21 -proc:none -cp "$OUT/classes:$CP:$JUNIT" -d "$OUT/test-classes" @"$OUT/test.txt"
"$JDK/bin/java" -jar "$JUNIT" execute -cp "$OUT/classes:$OUT/test-classes:$CP" --select-package com.winkreyes.seallog --details summary --fail-if-no-tests
# deterministic jar: fixed timestamps, sorted entries
cd "$OUT/classes"; find . -exec touch -h -d '2026-10-23 00:00:00' {} +
rm -f "$HERE/ExcelSealLog-1.0.0.jar"; find . -type f | sort | sed 's#^\./##' > ../entries.txt
"$JDK/bin/jar" --create --file "$HERE/ExcelSealLog-1.0.0.jar" --date=2026-10-23T00:00:00Z --no-manifest @../entries.txt
sha256sum "$HERE/ExcelSealLog-1.0.0.jar"
