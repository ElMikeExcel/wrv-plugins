#!/bin/bash
# Build ExcelEliteLadder-0.2.1.jar (deterministic: the script builds twice and
# refuses to finish unless the two jars are byte-identical).
# Manual javac route like the other Excel plugins. Compile-only deps are read
# from this VM's pinned trees (Paper API 26.3 build 231 + the Paper runtime
# library set), the same classpath family the other 0.2.0-era builds use.
# 0.2.1 adds the EliteMobs 10.9.12 jar as a COMPILE-ONLY dep (the elite-speed
# listener compiles against its API event classes; softdepend at runtime —
# no EM classes are packaged into this jar). The copy inside this tree's rig
# is the exact jar the live server runs.
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/hatch/workspace/.jdk25}
API=${API:-/home/hatch/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar}
EM=${EM:-$PWD/rig/server/plugins/EliteMobs-10.9.12.jar}
L=${L:-/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries}
CP="$API:$EM"
for j in $(find $L/net/kyori $L/org/jspecify $L/com/google $L/net/md-5 $L/org/yaml $L/org/slf4j $L/org/joml $L/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
rm -rf build/classes && mkdir -p build/classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp plugin.yml config.yml ladder.yml build/classes/
DATE=2026-11-05T00:00:00-06:00
OUT=${OUT:-ExcelEliteLadder-0.2.1.jar}
rm -f "$OUT" build/repro.jar
"$JDK/bin/jar" --create --file "$OUT" --date=$DATE -C build/classes .
"$JDK/bin/jar" --create --file build/repro.jar --date=$DATE -C build/classes .
cmp "$OUT" build/repro.jar && echo "deterministic: two builds byte-identical"
sha256sum "$OUT"
