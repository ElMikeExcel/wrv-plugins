#!/usr/bin/env bash
# Excel signature - WRV maintained source; existing authorship and license notices preserved.
# Explicit caller-owned dependencies only. No fetching, server control or deployment.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${JAVA_HOME:?Set JAVA_HOME to reviewed JDK 25}"
: "${CP_MAIN:?Set full reviewed main dependency classpath}"
: "${CP_TEST_EXTRA:?Set reviewed JUnit/Mockito/ByteBuddy/Objenesis/logging classpath}"
: "${MOCKITO_AGENT:?Set reviewed mockito-core agent JAR}"
: "${OUTPUT_DIR:?Set a new empty build output directory outside the source tree}"
if [ -e "$OUTPUT_DIR" ]; then echo 'Refusing existing output directory' >&2; exit 2; fi
mkdir -p "$OUTPUT_DIR/classes" "$OUTPUT_DIR/test-classes"
find "$ROOT/src" -name '*.java' | sort > "$OUTPUT_DIR/main-sources.txt"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP_MAIN" -d "$OUTPUT_DIR/classes" @"$OUTPUT_DIR/main-sources.txt"
cp "$ROOT/"{plugin.yml,config.yml,spawn-catalog.yml,LICENSE} "$OUTPUT_DIR/classes/"
cp -R "$ROOT/mobile-menus" "$OUTPUT_DIR/classes/"
CP_TEST="$OUTPUT_DIR/classes:$CP_MAIN:$CP_TEST_EXTRA"
find "$ROOT/tests" -name '*.java' | sort > "$OUTPUT_DIR/test-sources.txt"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP_TEST" -d "$OUTPUT_DIR/test-classes" @"$OUTPUT_DIR/test-sources.txt"
cd "$ROOT"
"$JAVA_HOME/bin/java" -Dnet.bytebuddy.experimental=true -javaagent:"$MOCKITO_AGENT" -cp "$OUTPUT_DIR/test-classes:$CP_TEST" org.junit.platform.console.ConsoleLauncher execute --scan-classpath "$OUTPUT_DIR/test-classes" --details summary --fail-if-no-tests
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP_TEST" -d "$OUTPUT_DIR/test-classes" "$ROOT/verification/"*.java
"$JAVA_HOME/bin/java" -cp "$OUTPUT_DIR/test-classes:$CP_TEST" GuardChecks
"$JAVA_HOME/bin/java" -cp "$OUTPUT_DIR/test-classes:$CP_TEST" LifecycleCrashChecks

"$JAVA_HOME/bin/java" -cp "$OUTPUT_DIR/test-classes:$CP_TEST" StatsPipeProbe
