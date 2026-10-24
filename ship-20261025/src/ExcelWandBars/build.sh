#!/bin/bash
# Build ExcelWandBars-0.2.0.jar (deterministic: fixed jar date, sorted inputs).
# Manual javac route, same as the other Excel plugins. Magic 11.2.5's jar is
# COMPILE-ONLY (soft-depend at runtime); nothing from Magic or Geyser is packaged.
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
LIBS=${LIBS:-/workspace/build-libs-wrv/paper1.21.4}
MAGIC=${MAGIC:-/workspace/wandbars/rig/plugins/Magic-11.2.5.jar}
CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
RT=${RT:-/workspace/build-libs-wrv/runtime-libraries}
CP="$CP:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar"
CP="$CP:$MAGIC"
rm -rf build/classes && mkdir -p build/classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java' | sort)
cp plugin.yml config.yml build/classes/
OUT=${OUT:-../../ExcelWandBars-0.2.0.jar}
rm -f "$OUT"
"$JDK/bin/jar" --create --file "$OUT" --date=2026-10-24T00:00:00-05:00 -C build/classes .
sha256sum "$OUT"
