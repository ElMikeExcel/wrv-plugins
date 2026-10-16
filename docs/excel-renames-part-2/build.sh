#!/usr/bin/env bash
# Build harness for the 2026-10-16 plugin renames part 2 (Argus).
# Manual javac route (same as the 2026-10-15 renames): JDK 25 javac, --release 21.
set -euo pipefail
export LANG=C.UTF-8 LC_ALL=C.UTF-8
O=/home/hatch/workspace/renames-2026-10-16
LIBS=/home/hatch/workspace/excelshops/build-libs
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
BK=/home/hatch/workspace/backups/pre-guild-20261015/root/plugins
export JAVA_HOME=/home/hatch/workspace/.jdk25
JAVAC=$JAVA_HOME/bin/javac; JAR=$JAVA_HOME/bin/jar

CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2 VaultAPI-1.7; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"

build_one() {
  local proj="$1" extra_cp="$2" jarname="$3"
  local out="$O/build/$proj"
  rm -rf "$out"; mkdir -p "$out/classes"
  find "$O/src/$proj/src" -name '*.java' > "$out/sources.txt"
  echo "== $proj: $(wc -l < "$out/sources.txt") sources"
  $JAVAC --release 21 -proc:none -nowarn -cp "$CP$extra_cp" -d "$out/classes" @"$out/sources.txt"
  cp "$O/src/$proj/plugin.yml" "$out/classes/"
  # Reproducible jar (the JDK jar tool stamps entries with build time):
  # sorted entries + fixed zip timestamps via make-jar.py, so two
  # consecutive builds are byte-identical.
  python3 "$O/make-jar.py" "$out/classes" "$O/$jarname"
  echo "built $O/$jarname"
}

build_one ExcelWealthBoard ":$BK/Jobs.jar" "ExcelWealthBoard-1.4.0.jar"
build_one ExcelWeekendXP "" "ExcelWeekendXP-1.1.0.jar"
build_one ExcelNoPhantoms "" "ExcelNoPhantoms-1.1.0.jar"
build_one ExcelRestartCountdown "" "ExcelRestartCountdown-1.2.0.jar"
echo "ALL BUILDS OK"
