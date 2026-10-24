#!/bin/bash
# Compile and run the ExcelDeathRecord plain-JVM suites.
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
rm -rf build/test-classes && mkdir -p build/test-classes
# Only the pure core is unit-tested on the plain JVM; the Bukkit
# glue is exercised on the scratch rig instead.
"$JDK/bin/javac" --release 21 -proc:none -nowarn -d build/test-classes \
  $(find src/com/winkreyes/exceldeathrecord/core -name '*.java') \
  $(find tests -name '*.java')
"$JDK/bin/java" -cp build/test-classes TestMain
