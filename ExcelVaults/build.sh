#!/bin/bash
# Build ExcelVaults-0.1.1.jar (deterministic: fixed jar date, sorted inputs) and run unit tests.
# Compile-time only deps: Paper API 26.3 (from the rig's bundler libraries), VaultAPI, Floodgate (bundles Cumulus).
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
LIBS=${LIBS:-/workspace/build-libs-wrv}
PAPER263=${PAPER263:-/workspace/claimfly-rig/libraries/io/papermc/paper/paper-api/26.3.build.214-stable/paper-api-26.3.build.214-stable.jar}
CP="$PAPER263:$(ls $LIBS/paper26.2/*.jar | grep -v paper-api | tr '\n' ':')$LIBS/VaultAPI-1.7.jar:$LIBS/floodgate.jar"
rm -rf build && mkdir -p build/classes build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java' | sort)
cp plugin.yml config.yml build/classes/
"$JDK/bin/javac" --release 21 -proc:none -cp "build/classes:$CP" -d build/test-classes $(find tests -name '*.java' | sort)
"$JDK/bin/java" -cp "build/test-classes:build/classes:$CP" com.winkreyes.excelvaults.TestMain
OUT=${OUT:-../ExcelVaults-0.1.1.jar}
rm -f "$OUT"
"$JDK/bin/jar" --create --file "$OUT" --date=2026-10-26T00:00:00-05:00 -C build/classes .
sha256sum "$OUT"
