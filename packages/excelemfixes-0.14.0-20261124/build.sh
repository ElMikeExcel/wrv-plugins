#!/bin/bash
# Build ExcelEMFixes-0.14.0.jar (deterministic: two builds byte-identical).
# Manual javac route, same as the other Excel plugins: house build-libs
# Paper API jar + kyori 4.20 set. SnakeYAML + Gson are used directly
# (Paper provides both at runtime). EliteMobs is a compile-time-only
# dependency for the quest-destinations module's EmQuestData reader
# (softdepend at runtime). Jobs is a compile-time-only dependency for
# the jobs-dummy-gate module's JobsDummyGate (softdepend at runtime;
# the class is only loaded when Jobs is present). ExcelItems is a
# compile-time-only dependency for the field-medic module's item
# registration bridge and the summon-mount module's registrar
# (softdepend at runtime; both bridges are only loaded when
# ExcelItems is present, and both modules stand down without
# it). VaultAPI is compile-time-only for the summon-mount buy
# flow (softdepend at runtime; only purchases touch it).
# mcMMO is a compile-time-only dependency for the
# mcmmo-xp-connector module's McMMOBridge (softdepend at
# runtime; the class is only loaded when mcMMO is present).
# The quest-giver-glow module reads EliteMobs quest/NPC data
# through the same compile-time-only EliteMobs dependency and
# sends its per-viewer glow packets by reflection only (the
# GlowPackets bridge), so it adds no new dependency at all.
# Geyser access is reflection-only, per the established
# Bedrock pattern.
set -euo pipefail
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
LIBS=/home/hatch/workspace/excelshops/build-libs
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"
CP="$CP:/home/hatch/workspace/m2-repo/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar"
CP="$CP:libs/EliteMobs-10.9.10.jar"
CP="$CP:libs/Jobs-5.2.6.3.jar"
CP="$CP:libs/ExcelItems-0.1.0.jar"
CP="$CP:libs/VaultAPI-1.7.jar"
CP="$CP:libs/mcMMO-2.3.003-WRV.jar"
rm -rf build/classes && mkdir -p build/classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src/src -name '*.java')
cp src/plugin.yml src/config.yml src/quest-destinations.yml src/drops-index.yml src/tracker-catalog.yml src/seal-restore.yml src/clarity-menus.yml build/classes/
OUT=ExcelEMFixes-0.14.0.jar
rm -f "$OUT"
"$JDK/bin/jar" --create --file "$OUT" --date=2026-11-23T00:00:00-06:00 -C build/classes .
sha256sum "$OUT"
ls -l "$OUT"
