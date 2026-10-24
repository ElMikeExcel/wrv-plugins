#!/bin/bash
# Unit checks on the plain JVM (TipBook rules, chat ok words, tips.yml rules, a real Cumulus form build).
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
LIBS=${LIBS:-/workspace/build-libs-wrv/paper1.21.4}
FG=${FG:-/workspace/tips-rig/plugins/floodgate.jar}
CP="build/classes:$LIBS/paper-api.jar:$LIBS/snakeyaml-2.2.jar:$LIBS/adventure-api-4.20.0.jar:$LIBS/adventure-key-4.20.0.jar:$LIBS/examination-api-1.3.0.jar:$LIBS/guava-33.3.1-jre.jar"
rm -rf build/tests && mkdir -p build/tests
"$JDK/bin/javac" --release 21 -nowarn -cp "$CP" -d build/tests $(find tests -name '*.java')
"$JDK/bin/java" -cp "build/tests:$CP" com.winkreyes.exceltips.TestMain tips.yml "${BANNED:-}" "$FG"
