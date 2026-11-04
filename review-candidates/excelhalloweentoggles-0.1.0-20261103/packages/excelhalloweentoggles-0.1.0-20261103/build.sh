#!/usr/bin/env bash
# ExcelHalloweenToggles — offline, reproducible build (Maven Central is
# unreachable from this sandbox; every dependency is a pinned local jar).
#
#   ./build.sh           compile + test + jar
#   ./build.sh jar-only  compile + jar (tests skipped — not a release path)
#
# Classpath sources (all pre-existing workspace artifacts):
#   Paper API 26.3.build.231-stable — the EliteMobs rig's extracted
#     Paper 26.3-231 library set (~/workspace/elitemobs-10.9.12/rig-boot)
#   Halloween_Softici 3.4.0 — the real event jar (compile-time only,
#     scope provided; softdepend at runtime)
#   PlaceholderAPI 2.12.3 — the live server's copy (provided)
#   JUnit 4.13.2 + hamcrest 1.3 — ~/workspace/m2-repo
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
JDK="$HOME/workspace/.jdk25/bin"
RIGLIBS="$HOME/workspace/elitemobs-10.9.12/rig-boot/libraries"
PAPER_API="$RIGLIBS/io/papermc/paper/paper-api/26.3.build.231-stable/paper-api-26.3.build.231-stable.jar"
HALLOWEEN_JAR="$HOME/workspace/halloween-event/halloween-plugin-3.4.0.jar"
PAPI_JAR="$HOME/workspace/minecraft-server/plugins/PlaceholderAPI-2.12.3.jar"
JUNIT="$HOME/workspace/m2-repo/junit/junit/4.13.2/junit-4.13.2.jar"
HAMCREST="$HOME/workspace/m2-repo/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
HALLOWEEN_CONFIG="$HOME/workspace/halloween-event/inspect/config.yml"
VERSION="0.1.0"
JAR_OUT="$ROOT/ExcelHalloweenToggles-$VERSION.jar"

for f in "$PAPER_API" "$HALLOWEEN_JAR" "$PAPI_JAR" "$JUNIT" "$HAMCREST" "$HALLOWEEN_CONFIG"; do
  [ -f "$f" ] || { echo "MISSING dependency: $f" >&2; exit 1; }
done

# Full Paper library set (paper-api's own transitive deps) + the two
# provided plugins. Used for compile and test classpaths alike.
LIBCP="$(find "$RIGLIBS" -name '*.jar' | sort | tr '\n' ':')"
CP="${LIBCP}${PAPER_API}:${HALLOWEEN_JAR}:${PAPI_JAR}"

BUILD="$ROOT/build"
rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/test-classes"

echo "== compile main =="
find "$ROOT/src/main/java" -name '*.java' > "$BUILD/sources.txt"
"$JDK/javac" -encoding UTF-8 -cp "$CP" -d "$BUILD/classes" @"$BUILD/sources.txt"
cp "$ROOT/src/main/resources/plugin.yml" "$ROOT/src/main/resources/config.yml" "$BUILD/classes/"

echo "== jar =="
printf 'Implementation-Title: ExcelHalloweenToggles\nImplementation-Version: %s\n' "$VERSION" > "$BUILD/manifest.mf"
"$JDK/jar" --create --file "$JAR_OUT" --manifest "$BUILD/manifest.mf" --date=2026-10-31T00:00:00Z -C "$BUILD/classes" .

if [ "${1:-}" != "jar-only" ]; then
  echo "== compile tests =="
  find "$ROOT/src/test/java" -name '*.java' > "$BUILD/test-sources.txt"
  "$JDK/javac" -encoding UTF-8 -cp "$CP:$JUNIT:$HAMCREST:$BUILD/classes" \
    -d "$BUILD/test-classes" @"$BUILD/test-sources.txt"

  echo "== run tests =="
  "$JDK/java" -Dhalloween.config="$HALLOWEEN_CONFIG" \
    -cp "$BUILD/test-classes:$BUILD/classes:$ROOT/src/main/resources:$CP:$JUNIT:$HAMCREST" \
    org.junit.runner.JUnitCore \
    com.winkreyesvalley.excelhalloweentoggles.ScareGateTest \
    com.winkreyesvalley.excelhalloweentoggles.EventWindowTest \
    com.winkreyesvalley.excelhalloweentoggles.ToggleStoreTest \
    com.winkreyesvalley.excelhalloweentoggles.FeatureRegistryTest
fi

echo "== built =="
sha256sum "$JAR_OUT"
ls -la "$JAR_OUT"
