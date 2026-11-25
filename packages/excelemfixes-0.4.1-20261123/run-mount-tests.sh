#!/bin/bash
# Plain-JVM suites for the summon-mount module, ported from the
# module lane's run-tests.sh at the 0.4.0 merge: the module's pure
# core and the item definition are Bukkit-free, and the registry
# checks run against the REAL ExcelItems core classes compiled
# from the ExcelItems 0.1.0 sources — no server needed. (The
# pack's main suites run via run-tests.sh; this script covers
# only the summon-mount additions, whose tests live in
# mount-tests/ because they use their own TestMain.)
set -euo pipefail
cd "$(dirname "$0")"

JDK=~/workspace/.jdk25
EI_SRC=/home/hatch/workspace/excelitems-build-20261118/src/ExcelItems/src

rm -rf build/mount-test-classes build/support-classes
mkdir -p build/mount-test-classes build/support-classes

# ExcelItems core (Bukkit-free) from its own sources.
"$JDK/bin/javac" --release 21 -proc:none -nowarn \
  -d build/support-classes \
  "$EI_SRC/com/winkreyes/excelitems/core/ItemDefinition.java" \
  "$EI_SRC/com/winkreyes/excelitems/core/ItemRegistry.java" \
  "$EI_SRC/com/winkreyes/excelitems/core/BuiltinItems.java"

# The module's pure core + the item definition (Bukkit-free).
"$JDK/bin/javac" --release 21 -proc:none -nowarn \
  -cp "build/support-classes" \
  -d build/mount-test-classes \
  src/src/excelemfixes/mount/core/*.java \
  src/src/excelemfixes/mount/MountItem.java \
  mount-tests/*.java

"$JDK/bin/java" -cp "build/support-classes:build/mount-test-classes" MountTestMain
