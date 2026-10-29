#!/usr/bin/env bash
# Excel signature - WRV maintained source; authorship notices preserved.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${JAVA_HOME:?Set reviewed JDK 25}"
: "${CP_MAIN:?Set complete reviewed dependencies}"
: "${OUTPUT_DIR:?Set new output directory outside this packet}"
if [ -e "$OUTPUT_DIR" ]; then echo 'Refusing existing output directory' >&2; exit 2; fi
mkdir -p "$OUTPUT_DIR/classes" "$OUTPUT_DIR/tests" "$OUTPUT_DIR/timer"
find "$ROOT/src" -name '*.java' | sort > "$OUTPUT_DIR/sources.txt"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP_MAIN" -d "$OUTPUT_DIR/classes" @"$OUTPUT_DIR/sources.txt"
cp "$ROOT/"{plugin.yml,config.yml} "$OUTPUT_DIR/classes/"
cp -R "$ROOT/menus" "$OUTPUT_DIR/classes/menus"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$OUTPUT_DIR/classes:$CP_MAIN" -d "$OUTPUT_DIR/tests" "$ROOT/tests/"*.java
cd "$ROOT"
"$JAVA_HOME/bin/java" -cp "$OUTPUT_DIR/tests:$OUTPUT_DIR/classes:$CP_MAIN" TestMain
find "$ROOT/timer-harness" -name '*.java' | sort > "$OUTPUT_DIR/timer-sources.txt"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -d "$OUTPUT_DIR/timer" @"$OUTPUT_DIR/timer-sources.txt"
"$JAVA_HOME/bin/java" -cp "$OUTPUT_DIR/timer" com.winkreyes.excelmenu.bukkit.RefreshHarness true
