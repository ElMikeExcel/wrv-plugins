#!/usr/bin/env bash
# ExcelSilkWatch - Excel signature - MikeExcel
# Sanitized for the records package (board-manager run 494): machine-specific
# default paths from the private build packet were replaced with required
# environment variables. Set JDK_HOME, WRV_LIB_DIR and PAPI_JAR before running.
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK_HOME:?set JDK_HOME to a JDK 25 home}
LIBS=${WRV_LIB_DIR:?set WRV_LIB_DIR to the server libraries directory}
CP=$(find "$LIBS" -name '*.jar' | sort | paste -sd:)
CP="$CP:${PAPI_JAR:?set PAPI_JAR to the PlaceholderAPI API jar}"
mkdir -p build/classes
find src -name '*.java' | sort > build/sources.txt
"$JDK/bin/javac" --release 21 -proc:none -Xlint:all,-classfile -cp "$CP" -d build/classes @build/sources.txt
cp resources/* build/classes/
"$JDK/bin/jar" --create --file build/ExcelSilkWatch-0.1.3-OFFLINE.jar --date=2026-10-29T00:00:00Z -C build/classes .
sha256sum build/ExcelSilkWatch-0.1.3-OFFLINE.jar
