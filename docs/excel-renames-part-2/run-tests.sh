#!/usr/bin/env bash
# Test harness: compiles each project's tests against its built classes and
# runs them with the JUnit platform console (Mockito as javaagent, matching
# the wrv-plugins Maven setup).
set -uo pipefail
export LANG=C.UTF-8 LC_ALL=C.UTF-8
O=/home/hatch/workspace/renames-2026-10-16
LIBS=/home/hatch/workspace/excelshops/build-libs
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
BK=/home/hatch/workspace/backups/pre-guild-20261015/root/plugins
M2=/home/hatch/workspace/m2-repo
export JAVA_HOME=/home/hatch/workspace/.jdk25
JAVAC=$JAVA_HOME/bin/javac; JAVA=$JAVA_HOME/bin/java

CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 adventure-text-logger-slf4j-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2 VaultAPI-1.7; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"
TESTCP="$LIBS/junit-platform-console-standalone-1.11.4.jar:$LIBS/mockito-core-5.15.2.jar:$LIBS/byte-buddy-1.18.3.jar:$LIBS/byte-buddy-agent-1.18.3.jar:$M2/org/objenesis/objenesis/3.3/objenesis-3.3.jar"

fail=0
run_project() {
  local proj="$1" extra_cp="$2"
  local out="$O/build/$proj/test-classes"
  rm -rf "$out"; mkdir -p "$out"
  find "$O/src/$proj/tests" -name '*.java' > "$O/build/$proj/test-sources.txt"
  echo "== $proj: compiling $(wc -l < "$O/build/$proj/test-sources.txt") test sources"
  $JAVAC --release 21 -proc:none -nowarn -cp "$O/build/$proj/classes:$CP$extra_cp:$TESTCP" -d "$out" @"$O/build/$proj/test-sources.txt" || { echo "TEST COMPILE FAIL $proj"; fail=1; return; }
  # ConsoleLauncher on the regular classpath (not -jar): Mockito's plugin
  # loader cannot see Byte Buddy through the standalone jar's classloader.
  $JAVA -javaagent:"$LIBS/mockito-core-5.15.2.jar" \
    -cp "$LIBS/junit-platform-console-standalone-1.11.4.jar:$out:$O/build/$proj/classes:$CP$extra_cp:$TESTCP" \
    org.junit.platform.console.ConsoleLauncher execute \
    --scan-class-path "$out" --details=summary --disable-banner || fail=1
}

run_project ExcelWealthBoard ":$BK/Jobs.jar"
run_project ExcelWeekendXP ""
run_project ExcelNoPhantoms ""
run_project ExcelRestartCountdown ""
exit $fail
