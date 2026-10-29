#!/usr/bin/env bash
# Excel WRV actual preference-save failure/retry test with synthetic identity.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
: "${JAVA_HOME:?}" "${PAPER_LIBS_DIR:?}" "${BUILD_DIR:?}"
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/fixtures"
CP=$(find "$PAPER_LIBS_DIR" -name '*.jar' | sort | paste -sd:)
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP" -d "$BUILD_DIR/classes" "$ROOT/src/com/winkreyes/makersmark/Prefs.java" "$ROOT"/regression/prefs/com/winkreyes/makersmark/*.java "$ROOT/regression/prefs/org/bukkit/Bukkit.java"
"$JAVA_HOME/bin/java" -cp "$BUILD_DIR/classes:$CP" com.winkreyes.makersmark.PrefsFailureTest "$BUILD_DIR/fixtures"
