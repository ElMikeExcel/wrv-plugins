#!/bin/bash
# Compile and run the ExcelEMLeash plain-JVM suites.
set -euo pipefail
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
rm -rf build/test-classes && mkdir -p build/test-classes
# The two policy cores are dependency-free (no Bukkit, no EliteMobs),
# so the suites run on the bare JVM; the Bukkit/EM glue is exercised
# on the scratch rig instead.
"$JDK/bin/javac" --release 21 -proc:none -nowarn -d build/test-classes \
  src/src/com/winkreyes/excelemleash/RadiusPolicy.java \
  src/src/com/winkreyes/excelemleash/AggroClearPolicy.java \
  $(find src/tests -name '*.java')
"$JDK/bin/java" -cp "build/test-classes" TestMain
