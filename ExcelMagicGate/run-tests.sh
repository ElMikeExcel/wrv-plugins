#!/usr/bin/env bash
# ExcelMagicGate automated tests: compiles the plugin sources plus the
# plain-JVM test main and runs it against the REAL bundled stage
# packets (grant counts 0/13/33/59/67/100 asserted). Prints one
# PASS/FAIL line per check and exits non-zero on any failure.
#
# Usage: ./run-tests.sh
set -euo pipefail
export LANG=C.UTF-8 LC_ALL=C.UTF-8
PROJ="$(cd "$(dirname "$0")" && pwd)"
LIBS=/home/hatch/workspace/excelshops/build-libs
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
export JAVA_HOME=/home/hatch/workspace/.jdk25
JAVAC=$JAVA_HOME/bin/javac; JAVA=$JAVA_HOME/bin/java

CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"

TDIR="$PROJ/build/test-classes"
rm -rf "$TDIR"
mkdir -p "$TDIR"
find "$PROJ/src" "$PROJ/tests" -name '*.java' > "$PROJ/build/test-sources.txt"
echo "== ExcelMagicGate tests: $(wc -l < "$PROJ/build/test-sources.txt") sources"
$JAVAC --release 21 -proc:none -nowarn -cp "$CP" -d "$TDIR" @"$PROJ/build/test-sources.txt"
$JAVA -cp "$TDIR" com.winkreyes.excelmagicgate.ExcelMagicGateTest "$PROJ/stages"
