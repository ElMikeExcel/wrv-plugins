#!/usr/bin/env bash
set -uo pipefail
export LANG=C.UTF-8 LC_ALL=C.UTF-8
O=/home/hatch/workspace/weekendxp-1.2.0
R=/home/hatch/workspace/renames-2026-10-16
LIBS=/home/hatch/workspace/excelshops/build-libs
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
M2=/home/hatch/workspace/m2-repo
export JAVA_HOME=/home/hatch/workspace/.jdk25
JAVAC=$JAVA_HOME/bin/javac; JAVA=$JAVA_HOME/bin/java
CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 adventure-text-logger-slf4j-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2 VaultAPI-1.7; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"
TESTCP="$LIBS/junit-platform-console-standalone-1.11.4.jar:$LIBS/mockito-core-5.15.2.jar:$LIBS/byte-buddy-1.18.3.jar:$LIBS/byte-buddy-agent-1.18.3.jar:$M2/org/objenesis/objenesis/3.3/objenesis-3.3.jar"
rm -rf "$O/build"; mkdir -p "$O/build/classes" "$O/build/test-classes"
find "$O/src/src" -name '*.java' > "$O/build/sources.txt"
echo "== main: $(wc -l < "$O/build/sources.txt") sources"
$JAVAC --release 21 -proc:none -nowarn -cp "$CP" -d "$O/build/classes" @"$O/build/sources.txt" || { echo MAIN-COMPILE-FAIL; exit 1; }
cp "$O/src/plugin.yml" "$O/build/classes/"
python3 "$R/make-jar.py" "$O/build/classes" "$O/ExcelWeekendXP-1.2.0.jar" || { echo JAR-FAIL; exit 1; }
find "$O/src/tests" -name '*.java' > "$O/build/test-sources.txt"
echo "== tests: $(wc -l < "$O/build/test-sources.txt") sources"
$JAVAC --release 21 -proc:none -nowarn -cp "$O/build/classes:$CP:$TESTCP" -d "$O/build/test-classes" @"$O/build/test-sources.txt" || { echo TEST-COMPILE-FAIL; exit 1; }
$JAVA -javaagent:"$LIBS/mockito-core-5.15.2.jar" -cp "$LIBS/junit-platform-console-standalone-1.11.4.jar:$O/build/test-classes:$O/build/classes:$CP:$TESTCP" org.junit.platform.console.ConsoleLauncher execute --scan-class-path "$O/build/test-classes" --details=summary --disable-banner
