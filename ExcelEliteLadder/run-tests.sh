#!/bin/bash
# Compile and run the ExcelEliteLadder plain-JVM suite (core only; Bukkit glue is proven on the rig).
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
rm -rf build/test-classes && mkdir -p build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -d build/test-classes \
  $(find src/com/winkreyes/exceleliteladder/core -name '*.java') $(find tests -name '*.java')
"$JDK/bin/java" -cp build/test-classes TestMain
