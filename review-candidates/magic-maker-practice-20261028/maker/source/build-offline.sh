#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")" && pwd)
: "${PAPER_LIBS_DIR:?Set PAPER_LIBS_DIR to exact Paper26.2build123 libraries directory}"
: "${PAPI_JAR:?Set PAPI_JAR to verified PlaceholderAPI2.11.6 jar}"
: "${JUNIT_CP:?Set JUNIT_CP to local JUnit5.11.4/platform1.11.4 dependencies}"
JAVAC=${JAVA_HOME:+$JAVA_HOME/bin/}javac
JAVA=${JAVA_HOME:+$JAVA_HOME/bin/}java
JAR=${JAVA_HOME:+$JAVA_HOME/bin/}jar
CP=$(find "$PAPER_LIBS_DIR" -name '*.jar' -print | sort | paste -sd:):$PAPI_JAR${EXTRA_CP:+:$EXTRA_CP}
OUT="$ROOT/build-out"
mkdir -p "$OUT/classes" "$OUT/tests"
find "$ROOT/src" -name '*.java' -print | sort > "$OUT/sources.txt"
"$JAVAC" --release 21 -proc:none -cp "$CP" -d "$OUT/classes" @"$OUT/sources.txt"
find "$ROOT/tests" -name '*.java' -print | sort > "$OUT/tests.txt"
"$JAVAC" --release 21 -proc:none -cp "$OUT/classes:$CP:$JUNIT_CP" -d "$OUT/tests" @"$OUT/tests.txt" "$ROOT/TestRunner.java"
"$JAVA" -cp "$OUT/classes:$OUT/tests:$CP:$JUNIT_CP" TestRunner
if [[ ${1:-} == --jar ]]; then
  cp "$ROOT/"{plugin.yml,config.yml,LICENSE,CHANGELOG.md} "$OUT/classes/"
  (cd "$OUT/classes"; find . -type f | sort > ../entries.txt; "$JAR" --create --file "$OUT/ExcelMakersMark-EXPERIMENTAL-NOT-FOR-INSTALL.jar" --date=2026-10-28T00:00:00Z --no-manifest @../entries.txt)
  sha256sum "$OUT/ExcelMakersMark-EXPERIMENTAL-NOT-FOR-INSTALL.jar"
fi
