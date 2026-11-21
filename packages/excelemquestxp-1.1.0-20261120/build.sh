#!/usr/bin/env bash
# Builds ExcelEMQuestXP-1.1.0.jar from source (D159 target choice + D160 rename).
# Manual javac route (same as the other Excel plugins): JDK 25 javac,
# --release 21, classpath = house build-libs Paper API set.
# EliteMobs-10.9.10.jar (the PINNED live file, W6 fresh pull Nov 19 2026; plugin.yml reports 10.9.14)
# is a COMPILE-ONLY dependency; no EliteMobs classes are packaged — the
# plugin soft-depends on EliteMobs at runtime and disables itself cleanly
# if the pinned API classes are absent (see ExcelQuestXP.onEnable).
# Packaging pins zip entry timestamps and uses a sorted entry list so
# two clean builds reproduce byte-identically.
set -euo pipefail
export LANG=C.UTF-8 LC_ALL=C.UTF-8
PROJ="$(cd "$(dirname "$0")" && pwd)"
LIBS=${EXCEL_LIBS:-/home/hatch/workspace/excelshops/build-libs}
RT=${EXCEL_RT:-/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries}
EM=${EM_JAR:-/home/hatch/workspace/quest-xp-bridge-20261119/live-pull/w6-quest-pull/EliteMobs-10.9.10.jar}
export JAVA_HOME=${JAVA_HOME:-/home/hatch/workspace/.jdk25}
JAVAC=$JAVA_HOME/bin/javac; JAR=$JAVA_HOME/bin/jar
OUT=${1:-$PROJ/../../ExcelEMQuestXP-1.1.0.jar}

# Pin check: refuse to build against anything but the pinned live jar.
EM_SHA=$(sha256sum "$EM" | awk '{print $1}')
if [ "$EM_SHA" != "4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd" ]; then
  echo "REFUSING TO BUILD: EliteMobs jar SHA-256 $EM_SHA != pinned 4a6bc4a7…44bd (live file, reports 10.9.14)" >&2
  exit 1
fi

CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"
CP="$CP:$EM"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classes"
find "$PROJ/src" -name '*.java' > "$WORK/sources.txt"
echo "== ExcelQuestXP: $(wc -l < "$WORK/sources.txt") sources"
$JAVAC --release 21 -proc:none -nowarn -cp "$CP" -d "$WORK/classes" @"$WORK/sources.txt"
cp "$PROJ/plugin.yml" "$WORK/classes/"
cp "$PROJ/quest-xp.yml" "$WORK/classes/"
cp "$PROJ/backpay.yml" "$WORK/classes/"
mkdir -p "$(dirname "$OUT")"
(cd "$WORK/classes" && find . -type f | LC_ALL=C sort | sed 's|^\./||' | xargs $JAR --create --file "$OUT" --date=2026-11-20T00:00:00-06:00)
echo "built $OUT"
sha256sum "$OUT"
ls -l "$OUT"
