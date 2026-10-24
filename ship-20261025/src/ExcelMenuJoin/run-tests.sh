#!/bin/bash
# Compile and run the ExcelMenuJoin 1.4.1 plain-JVM suites.
set -euo pipefail
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
LIBS=/home/hatch/workspace/excelshops/build-libs
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
CP="$LIBS/paper-api.jar:$LIBS/adventure-api-4.20.0.jar:$LIBS/adventure-key-4.20.0.jar:$LIBS/adventure-text-logger-slf4j-4.20.0.jar:$LIBS/examination-api-1.3.0.jar:$LIBS/option-1.0.0.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"
rm -rf build/test-classes && mkdir -p build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/test-classes \
  src/com/winkreyesvalley/excelmenujoin/PouchCompassCheck.java \
  $(find tests -name '*.java')
"$JDK/bin/java" -cp "build/test-classes:$CP" TestMain
