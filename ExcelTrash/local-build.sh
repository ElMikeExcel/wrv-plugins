#!/usr/bin/env bash
# ExcelTrash local build: JDK 25, --release 21, javac against the Paper 26.3 API (build 214, from the test rig libraries).
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd); PROJ=$HERE/project
JDK=${JDK:-/workspace/wrv019/jdk25}; L=${LIBS:-/workspace/ship1025-rig/libraries}
JUNIT=${JUNIT:-/workspace/excelshops13/libs/junit-platform-console-standalone-1.11.4.jar}
CP=$(find "$L" -name "*.jar" | tr '\n' ':')
OUT=$HERE/build-out; rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/test-classes"
find "$PROJ/src" -name "*.java" | sort > "$OUT/src.txt"
"$JDK/bin/javac" --release 21 -proc:none -Xlint:-options -cp "$CP" -d "$OUT/classes" @"$OUT/src.txt"
cp "$PROJ/plugin.yml" "$PROJ/config.yml" "$PROJ/LICENSE" "$OUT/classes/"
find "$PROJ/tests" -name "*.java" | sort > "$OUT/test.txt"
"$JDK/bin/javac" --release 21 -proc:none -cp "$OUT/classes:$CP:$JUNIT" -d "$OUT/test-classes" @"$OUT/test.txt"
"$JDK/bin/java" -jar "$JUNIT" execute -cp "$OUT/classes:$OUT/test-classes:$CP" --select-package com.winkreyes.trash --details summary --fail-if-no-tests
# deterministic jar: fixed timestamps, sorted entries
cd "$OUT/classes"; find . -exec touch -h -d '2026-10-26 00:00:00' {} +
rm -f "$HERE/ExcelTrash-0.3.0.jar"; find . -type f | sort | sed 's#^\./##' > ../entries.txt
"$JDK/bin/jar" --create --file "$HERE/ExcelTrash-0.3.0.jar" --date=2026-10-26T00:00:00Z --no-manifest @../entries.txt
sha256sum "$HERE/ExcelTrash-0.3.0.jar"
