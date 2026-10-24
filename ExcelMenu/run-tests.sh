#!/bin/bash
# Compile and run the ExcelMenu plain-JVM suites.
set -euo pipefail
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
LIBS=/home/hatch/workspace/excelshops/build-libs
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
PAPI=/home/hatch/workspace/minecraft-server/plugins/PlaceholderAPI-2.12.3.jar
# Same library set as build.sh: the core is unit-tested on the plain
# JVM; the Paper API jar supplies Material names for the validator
# fixtures. The Bukkit glue is exercised on the rig.
CP="$LIBS/paper-api.jar:$LIBS/VaultAPI-1.7.jar:$LIBS/snakeyaml-2.2.jar:$PAPI:/home/hatch/workspace/m2-repo/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"
rm -rf build/test-classes && mkdir -p build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/test-classes \
  $(find src/com/winkreyes/excelmenu/core -name '*.java') \
  $(find tests -name '*.java')
"$JDK/bin/java" -cp "build/test-classes:$CP" TestMain

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
