#!/bin/bash
# Build ExcelTips-0.1.0.jar (deterministic: fixed jar date, sorted inputs).
# Manual javac route like the other Excel plugins. No Floodgate, Geyser or
# Magic classes are needed at compile time (all reflection).
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
LIBS=${LIBS:-/workspace/build-libs-wrv/paper1.21.4}
CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
RT=${RT:-/workspace/build-libs-wrv/runtime-libraries}
CP="$CP:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar"
rm -rf build/classes && mkdir -p build/classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java' | sort)
cp plugin.yml config.yml tips.yml build/classes/
OUT=${OUT:-../ExcelTips-0.1.0.jar}
rm -f "$OUT"
"$JDK/bin/jar" --create --file "$OUT" --date=2026-10-24T00:00:00-05:00 -C build/classes .
sha256sum "$OUT"
