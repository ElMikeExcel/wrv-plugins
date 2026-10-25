#!/bin/bash
# Build ExcelClaimFly-0.1.0.jar (deterministic: fixed jar date, sorted inputs) and run unit tests.
# Compile-time only deps: Paper API 26.2, VaultAPI, LuckPerms API, GriefPrevention, Floodgate (bundles Cumulus).
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
LIBS=${LIBS:-/workspace/build-libs-wrv}
RIGP=${RIGP:-/workspace/ship1025-rig/plugins}
CP="$(ls $LIBS/paper26.2/*.jar | tr '\n' ':')$LIBS/VaultAPI-1.7.jar:$LIBS/floodgate.jar:$RIGP/LuckPerms-Bukkit-5.5.87.jar:$RIGP/GriefPrevention-16.18.7.jar"
rm -rf build && mkdir -p build/classes build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java' | sort)
cp plugin.yml config.yml build/classes/
"$JDK/bin/javac" --release 21 -proc:none -cp "build/classes:$CP" -d build/test-classes $(find tests -name '*.java' | sort)
"$JDK/bin/java" -cp "build/test-classes:build/classes:$CP" com.winkreyes.excelclaimfly.TestMain
OUT=${OUT:-../ExcelClaimFly-0.1.0.jar}
rm -f "$OUT"
"$JDK/bin/jar" --create --file "$OUT" --date=2026-10-25T00:00:00-05:00 -C build/classes .
sha256sum "$OUT"
