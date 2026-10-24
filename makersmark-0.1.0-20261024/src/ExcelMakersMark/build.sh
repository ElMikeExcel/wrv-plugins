#!/usr/bin/env bash
# ExcelMakersMark local build: same route as ExcelTrash (JDK 25, javac --release 21, Paper API scratch libs,
# JUnit console). PlaceholderAPI is compile-only (soft-depend). Deterministic jar: fixed dates, sorted entries.
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd); PROJ=$HERE; V=0.1.0
JDK=${JDK:-/workspace/wrv019/jdk25}; L=${LIBS:-/workspace/acro/scratch/libraries}
JUNIT=${JUNIT:-/workspace/excelshops13/libs/junit-platform-console-standalone-1.11.4.jar}
PAPI=${PAPI:-/workspace/build-libs-wrv/PlaceholderAPI.jar}
CP=$(find "$L" -name "*.jar" | tr '\n' ':')$PAPI
OUT=$HERE/build-out; rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/test-classes"
find "$PROJ/src" -name "*.java" | sort > "$OUT/src.txt"
"$JDK/bin/javac" --release 21 -proc:none -Xlint:-options -Xlint:unchecked -cp "$CP" -d "$OUT/classes" @"$OUT/src.txt"
cp "$PROJ/plugin.yml" "$PROJ/config.yml" "$PROJ/LICENSE" "$PROJ/CHANGELOG.md" "$OUT/classes/"
find "$PROJ/tests" -name "*.java" | sort > "$OUT/test.txt"
"$JDK/bin/javac" --release 21 -proc:none -cp "$OUT/classes:$CP:$JUNIT" -d "$OUT/test-classes" @"$OUT/test.txt"
"$JDK/bin/java" -jar "$JUNIT" execute -cp "$OUT/classes:$OUT/test-classes:$CP" --select-package com.winkreyes.makersmark --details summary --fail-if-no-tests
cd "$OUT/classes"; find . -exec touch -h -d '2026-10-24 00:00:00' {} +
rm -f "$HERE/ExcelMakersMark-$V.jar"; find . -type f | sort | sed 's#^\./##' > ../entries.txt
"$JDK/bin/jar" --create --file "$HERE/ExcelMakersMark-$V.jar" --date=2026-10-24T00:00:00Z --no-manifest @../entries.txt
sha256sum "$HERE/ExcelMakersMark-$V.jar"
