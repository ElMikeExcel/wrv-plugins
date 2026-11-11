#!/bin/bash
# Build ExcelMenuJoin-1.4.4.jar (deterministic: two builds byte-identical).
# Manual javac route, same house classpath as 1.4.1 (excelshops build-libs
# Paper API + kyori 4.20 set + PlaceholderAPI 2.12.3, compile-time only).
# The 1.4.3 base packet shipped with Moon's python builder
# (build-and-test.py in that packet); this packet uses the house javac
# recipe, as the Quartermaster/AimAssist packets do.
# Packaging pins zip entry timestamps and uses a sorted entry list so two
# clean builds reproduce byte-identically (the Quartermaster house recipe).
set -euo pipefail
export LANG=C.UTF-8 LC_ALL=C.UTF-8
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
LIBS=/home/hatch/workspace/excelshops/build-libs
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
PAPI=/home/hatch/workspace/minecraft-server/plugins/PlaceholderAPI-2.12.3.jar
OUT=${1:-/home/hatch/workspace/menujoin-142-build/ExcelMenuJoin-1.4.4.jar}
CP="$LIBS/paper-api.jar:$PAPI"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classes"
find src -name '*.java' > "$WORK/sources.txt"
echo "== ExcelMenuJoin: $(wc -l < "$WORK/sources.txt") sources"
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d "$WORK/classes" @"$WORK/sources.txt"
cp plugin.yml "$WORK/classes/"
mkdir -p "$(dirname "$OUT")"
(cd "$WORK/classes" && find . -type f | LC_ALL=C sort | sed 's|^\./||' | xargs "$JDK/bin/jar" --create --file "$OUT" --date=2026-11-10T00:00:00-06:00)
echo "built $OUT"
sha256sum "$OUT"
ls -l "$OUT"
