#!/usr/bin/env bash
# Excel WRV portable source-only build and test. No dependency downloads.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")" && pwd)
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
: "${PAPER_LIBS_DIR:?Set PAPER_LIBS_DIR to Paper API and transitive libraries}"
: "${JUNIT_JAR:?Set JUNIT_JAR to JUnit console standalone 1.11.4}"
: "${BUILD_DIR:?Set BUILD_DIR to a new empty external output directory}"
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/tests"
CP=$(find "$PAPER_LIBS_DIR" -name '*.jar' | sort | paste -sd:)
find "$ROOT/src" -name '*.java' | sort > "$BUILD_DIR/sources.txt"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP" -d "$BUILD_DIR/classes" @"$BUILD_DIR/sources.txt"
find "$ROOT/tests" -name '*.java' | sort > "$BUILD_DIR/tests.txt"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$BUILD_DIR/classes:$CP:$JUNIT_JAR" -d "$BUILD_DIR/tests" @"$BUILD_DIR/tests.txt"
"$JAVA_HOME/bin/java" -Dfixtures="$ROOT/tests/fixtures" -jar "$JUNIT_JAR" execute -cp "$BUILD_DIR/classes:$BUILD_DIR/tests:$CP" --select-package com.winkreyes.sealalerts --details summary --fail-if-no-tests --disable-banner
