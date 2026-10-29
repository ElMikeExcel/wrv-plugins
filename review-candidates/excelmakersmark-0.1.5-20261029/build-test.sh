#!/usr/bin/env bash
# Excel WRV portable source-only build and test. No baseline binary reuse.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")" && pwd)
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
: "${PAPER_LIBS_DIR:?Set PAPER_LIBS_DIR to Paper API and transitive libraries}"
: "${PAPI_JAR:?Set PAPI_JAR to PlaceholderAPI 2.11.6}"
: "${MAGIC_JAR:?Set MAGIC_JAR to matching Magic API dependency}"
: "${JUNIT_JAR:?Set JUNIT_JAR to JUnit console standalone 1.11.4}"
: "${BUILD_DIR:?Set BUILD_DIR to a new empty external output directory}"
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/tests"
CP=$(find "$PAPER_LIBS_DIR" -name '*.jar' | sort | paste -sd:):$PAPI_JAR:$MAGIC_JAR
find "$ROOT/src" -name '*.java' | sort > "$BUILD_DIR/sources.txt"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP" -d "$BUILD_DIR/classes" @"$BUILD_DIR/sources.txt"
find "$ROOT/tests" -name '*.java' | sort > "$BUILD_DIR/tests.txt"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$BUILD_DIR/classes:$CP:$JUNIT_JAR" -d "$BUILD_DIR/tests" @"$BUILD_DIR/tests.txt" "$ROOT/TestRunner.java"
"$JAVA_HOME/bin/java" -cp "$BUILD_DIR/classes:$BUILD_DIR/tests:$CP:$JUNIT_JAR" TestRunner
