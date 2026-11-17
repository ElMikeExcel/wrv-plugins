#!/usr/bin/env bash
# ExcelSilkWatch - Excel signature - MikeExcel
# Sanitized for the records package (board-manager run 494): machine-specific
# default paths from the private build packet were replaced with required
# environment variables. Set JDK_HOME and WRV_LIB_DIR before running.
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK_HOME:?set JDK_HOME to a JDK 25 home}
LIBS=${WRV_LIB_DIR:?set WRV_LIB_DIR to the server libraries directory}
CP=$(find "$LIBS" -name '*.jar' | sort | paste -sd:)
mkdir -p build/preference-test-classes evidence
"$JDK/bin/javac" --release 21 -proc:none -cp "$CP" -d build/preference-test-classes src/com/winkreyes/silkstorage/PreferenceStore.java preference-tests/com/winkreyes/silkstorage/PreferenceStoreTest.java
"$JDK/bin/java" -cp "build/preference-test-classes:$CP" com.winkreyes.silkstorage.PreferenceStoreTest | tee evidence/preference-results.txt
