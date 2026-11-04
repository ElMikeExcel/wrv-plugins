#!/usr/bin/env bash
# ExcelHalloweenToggles 0.1.1 — offline, reproducible build.
# Same pinned-dependency discipline as the 0.1.0 build.sh (which
# rebuilds the shipped 0.1.0 jar byte-identically — proven again at
# this build's start); only the source tree, version and jar date move.
#
#   ./build.sh           compile + test + jar
#   ./build.sh jar-only  compile + jar (tests skipped — not a release path)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/src-work"
JDK="$HOME/workspace/.jdk25/bin"
RIGLIBS="$HOME/workspace/elitemobs-10.9.12/rig-boot/libraries"
PAPER_API="$RIGLIBS/io/papermc/paper/paper-api/26.3.build.231-stable/paper-api-26.3.build.231-stable.jar"
HALLOWEEN_JAR="$HOME/workspace/halloween-event/halloween-plugin-3.4.0.jar"
PAPI_JAR="$HOME/workspace/minecraft-server/plugins/PlaceholderAPI-2.12.3.jar"
JUNIT="$HOME/workspace/m2-repo/junit/junit/4.13.2/junit-4.13.2.jar"
HAMCREST="$HOME/workspace/m2-repo/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
HALLOWEEN_CONFIG="$HOME/workspace/halloween-event/inspect/config.yml"
VERSION="0.1.1"
JAR_OUT="$ROOT/ExcelHalloweenToggles-$VERSION.jar"

for f in "$PAPER_API" "$HALLOWEEN_JAR" "$PAPI_JAR" "$JUNIT" "$HAMCREST" "$HALLOWEEN_CONFIG"; do
  [ -f "$f" ] || { echo "MISSING dependency: $f" >&2; exit 1; }
done

LIBCP="$(find "$RIGLIBS" -name '*.jar' | sort | tr '\n' ':')"
CP="${LIBCP}${PAPER_API}:${HALLOWEEN_JAR}:${PAPI_JAR}"

BUILD="$ROOT/build"
rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/test-classes"

echo "== compile main =="
find "$SRC/main/java" -name '*.java' > "$BUILD/sources.txt"
"$JDK/javac" -encoding UTF-8 -cp "$CP" -d "$BUILD/classes" @"$BUILD/sources.txt"
cp "$SRC/main/resources/plugin.yml" "$SRC/main/resources/config.yml" "$BUILD/classes/"

echo "== jar =="
printf 'Implementation-Title: ExcelHalloweenToggles\nImplementation-Version: %s\n' "$VERSION" > "$BUILD/manifest.mf"
"$JDK/jar" --create --file "$JAR_OUT" --manifest "$BUILD/manifest.mf" --date=2026-11-01T00:00:00Z -C "$BUILD/classes" .

if [ "${1:-}" != "jar-only" ]; then
  echo "== compile tests =="
  find "$SRC/test/java" -name '*.java' > "$BUILD/test-sources.txt"
  "$JDK/javac" -encoding UTF-8 -cp "$CP:$JUNIT:$HAMCREST:$BUILD/classes" \
    -d "$BUILD/test-classes" @"$BUILD/test-sources.txt"

  echo "== run tests =="
  "$JDK/java" -Dhalloween.config="$HALLOWEEN_CONFIG" \
    -cp "$BUILD/test-classes:$BUILD/classes:$SRC/main/resources:$CP:$JUNIT:$HAMCREST" \
    org.junit.runner.JUnitCore \
    com.winkreyesvalley.excelhalloweentoggles.ScareGateTest \
    com.winkreyesvalley.excelhalloweentoggles.EventWindowTest \
    com.winkreyesvalley.excelhalloweentoggles.ToggleStoreTest \
    com.winkreyesvalley.excelhalloweentoggles.FeatureRegistryTest \
    com.winkreyesvalley.excelhalloweentoggles.ScareViewTest
fi

echo "== built =="
sha256sum "$JAR_OUT"
ls -la "$JAR_OUT"
