#!/bin/bash
# Compile and run the ExcelTestMode plain-JVM suite (the TestModeTest
# checks ported unchanged from the ExcelEMFixes v31 tree — same
# contract, same numbers; only the package moved).
set -euo pipefail
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
LIBS=/home/hatch/workspace/excelshops/build-libs
CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:/home/hatch/workspace/m2-repo/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar"
rm -rf build/test-classes && mkdir -p build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/test-classes \
  src/src/exceltestmode/TestModeMath.java \
  $(find src/tests -name '*.java')
"$JDK/bin/java" -cp "build/test-classes:$CP" TestMain
