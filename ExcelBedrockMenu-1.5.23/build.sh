#!/bin/bash
# Build ExcelBedrockMenu-1.5.23.jar from this tree (1.5.22 source + Server Commands parity: /cosmetics in Shops & Trading, new Your Items & Help group with /lostfound, /trash, /mark, /ask).
# Same recipe that reproduces the live 1.5.18 jar ee3d00f0 byte for byte from the 1.5.18 source:
# javac --release 21 with JDK 25.0.5, then `jar --date=2026-10-26T00:00:00Z` with an explicit sorted file list.
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
LIBS=${LIBS:-/workspace/build-libs-wrv/paper1.21.4}
CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
RT=${RT:-/workspace/build-libs-wrv/runtime-libraries}
CP="$CP:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar"
rm -rf build && mkdir -p build/classes build/tests
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java' | sort) 2>&1 | grep -v '^Note:' || true
cp plugin.yml config.yml build/classes/
OUT=${OUT:-../staged/ExcelBedrockMenu-1.5.23.jar}
OUT=$(realpath -m "$OUT"); rm -f "$OUT"
(cd build/classes && TZ=UTC "$JDK/bin/jar" --create --file "$OUT" --date=2026-10-26T00:00:00Z $(find com -name '*.class' | sort) config.yml plugin.yml)
sha256sum "$OUT"
# Unit checks (MenuLogicTest reads the source tree, so run from here)
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP:build/classes" -d build/tests $(find test -name '*.java')
"$JDK/bin/java" -cp "$CP:build/classes:build/tests" com.winkreyes.excelbedrockmenu.MenuLogicTest > ../tests/MenuLogicTest-1.5.23.out || { tail -5 ../tests/MenuLogicTest-1.5.23.out; exit 1; }
grep -c '^ok' ../tests/MenuLogicTest-1.5.23.out; tail -1 ../tests/MenuLogicTest-1.5.23.out
