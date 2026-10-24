#!/bin/bash
# Compile and run the ExcelAttunement plain-JVM suites.
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-~/workspace/.jdk25}
STAGES=${STAGES:-/home/hatch/workspace/excelmagicgate-0.1.0/src/ExcelMagicGate/stages}
rm -rf build/test-classes && mkdir -p build/test-classes
# Only the pure core is unit-tested on the plain JVM; the Bukkit/Magic
# glue is exercised on the scratch rig instead.
"$JDK/bin/javac" --release 21 -proc:none -nowarn -d build/test-classes \
  $(find src/com/winkreyes/excelattunement/core -name '*.java') \
  $(find tests -name '*.java')
"$JDK/bin/java" -Dstages="$STAGES" -Dtable=spell-classes.txt -Dsignals=tests/spell-signals.txt \
  -cp build/test-classes TestMain
