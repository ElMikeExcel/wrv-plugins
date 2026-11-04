#!/usr/bin/env bash
# ExcelMagicLevel — offline, reproducible build (Maven Central is unreachable
# from this sandbox; every dependency is a pinned local jar).
#
#   ./build.sh           compile + test + generate templates + jar
#   ./build.sh jar-only  compile + generate + jar (tests skipped — not a release path)
#
# Classpath sources (all pre-existing workspace artifacts):
#   Paper API 26.3.build.231-stable — the EliteMobs rig's extracted
#     Paper 26.3-231 library set (~/workspace/elitemobs-10.9.12/rig-boot)
#   Magic 11.2.5 — the real event jar (compile-time only, scope provided;
#     the plugin `depend`s on Magic at runtime)
#   PlaceholderAPI 2.12.3 — the menu-2.0 rig's jar (compile-time only;
#     PAPI stays an optional runtime neighbour, softdepend)
#   JUnit 4.13.2 + hamcrest 1.3 — ~/workspace/m2-repo
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
JDK="$HOME/workspace/.jdk25/bin"
RIGLIBS="$HOME/workspace/elitemobs-10.9.12/rig-boot/libraries"
PAPER_API="$RIGLIBS/io/papermc/paper/paper-api/26.3.build.231-stable/paper-api-26.3.build.231-stable.jar"
MAGIC_JAR="$HOME/workspace/magic-scratch/downloads/Magic-11.2.5.jar"
PAPI_JAR="$HOME/workspace/menu-2.0/rig/plugins/PlaceholderAPI-2.12.3.jar"
JUNIT="$HOME/workspace/m2-repo/junit/junit/4.13.2/junit-4.13.2.jar"
HAMCREST="$HOME/workspace/m2-repo/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
VERSION="0.2.0"
JAR_OUT="$ROOT/ExcelMagicLevel-$VERSION.jar"
FRAGMENT_OUT="$ROOT/modifiers-magiclevel-fragment.yml"

for f in "$PAPER_API" "$MAGIC_JAR" "$PAPI_JAR" "$JUNIT" "$HAMCREST"; do
  [ -f "$f" ] || { echo "MISSING dependency: $f" >&2; exit 1; }
done

LIBCP="$(find "$RIGLIBS" -name '*.jar' | sort | tr '\n' ':')"
CP="${LIBCP}${PAPER_API}:${MAGIC_JAR}:${PAPI_JAR}"

BUILD="$ROOT/build-out"
rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/test-classes" "$BUILD/gen-classes"

echo "== compile main =="
find "$ROOT/src/main/java" -name '*.java' > "$BUILD/sources.txt"
"$JDK/javac" -encoding UTF-8 -cp "$CP" -d "$BUILD/classes" @"$BUILD/sources.txt"
cp "$ROOT/src/main/resources/plugin.yml" "$ROOT/src/main/resources/config.yml" "$BUILD/classes/"

echo "== generate modifier templates from config =="
find "$ROOT/src/gen/java" -name '*.java' > "$BUILD/gen-sources.txt"
"$JDK/javac" -encoding UTF-8 -cp "$CP:$BUILD/classes" -d "$BUILD/gen-classes" @"$BUILD/gen-sources.txt"
"$JDK/java" -cp "$BUILD/gen-classes:$BUILD/classes:$CP" \
  com.winkreyesvalley.excelmagiclevel.gen.TemplateGenMain \
  "$ROOT/src/main/resources/config.yml" "$FRAGMENT_OUT"

echo "== jar =="
printf 'Implementation-Title: ExcelMagicLevel\nImplementation-Version: %s\n' "$VERSION" > "$BUILD/manifest.mf"
"$JDK/jar" --create --file "$JAR_OUT" --manifest "$BUILD/manifest.mf" --date=2026-10-31T00:00:00Z -C "$BUILD/classes" .

if [ "${1:-}" != "jar-only" ]; then
  echo "== compile tests =="
  find "$ROOT/src/test/java" -name '*.java' > "$BUILD/test-sources.txt"
  "$JDK/javac" -encoding UTF-8 -cp "$CP:$JUNIT:$HAMCREST:$BUILD/classes" \
    -d "$BUILD/test-classes" @"$BUILD/test-sources.txt"

  echo "== run tests =="
  "$JDK/java" -Dmagiclevel.srcdir="$ROOT/src/main/java" \
    -cp "$BUILD/test-classes:$BUILD/classes:$ROOT/src/main/resources:$CP:$JUNIT:$HAMCREST" \
    org.junit.runner.JUnitCore \
    com.winkreyesvalley.excelmagiclevel.core.CurveTest \
    com.winkreyesvalley.excelmagiclevel.core.XpMathTest \
    com.winkreyesvalley.excelmagiclevel.core.AntiAbuseWindowsTest \
    com.winkreyesvalley.excelmagiclevel.core.TablesTest \
    com.winkreyesvalley.excelmagiclevel.core.ShareDistributionTest \
    com.winkreyesvalley.excelmagiclevel.core.ModifierTemplatesTest \
    com.winkreyesvalley.excelmagiclevel.core.ExportRowsTest \
    com.winkreyesvalley.excelmagiclevel.store.PlayerStoreTest \
    com.winkreyesvalley.excelmagiclevel.adapter.magic.ResultPolicyTest \
    com.winkreyesvalley.excelmagiclevel.PackageDependencyTest
fi

echo "== built =="
sha256sum "$JAR_OUT" "$FRAGMENT_OUT"
ls -la "$JAR_OUT" "$FRAGMENT_OUT"
