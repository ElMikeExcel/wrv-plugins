#!/bin/bash
# Plain-JVM suites for the pure core (house pattern, no framework), plus a
# Cumulus form build check against the real floodgate jar when present.
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
FG=${FG:-/workspace/build-libs-wrv/floodgate.jar}
rm -rf build/test-classes && mkdir -p build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -d build/test-classes \
  $(find src/com/winkreyes/excelwandbars/core -name '*.java') $(find tests -name '*.java')
"$JDK/bin/java" -Dfloodgate="$FG" -cp build/test-classes TestMain
