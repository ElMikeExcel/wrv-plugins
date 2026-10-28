#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
: "${BUILD_CP:?Set BUILD_CP to Paper 26.2 API/runtime libraries and required compile-only dependencies}"
mkdir -p build/classes
find src -name '*.java' | sort > build/sources.txt
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$BUILD_CP" -d build/classes @build/sources.txt
cp plugin.yml config.yml build/classes/
(cd build/classes; find . -type f | sort | sed 's|^./||' > ../entries.txt; "$JAVA_HOME/bin/jar" --create --file ../ExcelGuildGate-0.3.4.jar --date=2026-10-27T00:00:00Z @../entries.txt)
sha256sum build/ExcelGuildGate-0.3.4.jar
