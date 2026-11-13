#!/usr/bin/env bash
# ExcelMenu 2.0 (Phase 2) — offline, reproducible build (Maven Central is
# unreachable from this sandbox; every dependency is a pinned local jar).
#
#   ./build.sh           compile + test + jar
#   ./build.sh jar-only  compile + jar (tests skipped — not a release path)
#
# Classpath sources (all pre-existing workspace artifacts):
#   Paper API 26.3.build.231-stable — the EliteMobs rig's extracted
#     Paper 26.3-231 library set (~/workspace/elitemobs-10.9.12/rig-boot)
#   PlaceholderAPI 2.12.3 — the live server's copy (provided)
#   JUnit 4.13.2 + hamcrest 1.3 — ~/workspace/m2-repo
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
JDK="$HOME/workspace/.jdk25/bin"
RIGLIBS="$HOME/workspace/elitemobs-10.9.12/rig-boot/libraries"
PAPER_API="$RIGLIBS/io/papermc/paper/paper-api/26.3.build.231-stable/paper-api-26.3.build.231-stable.jar"
PAPI_JAR="$HOME/workspace/minecraft-server/plugins/PlaceholderAPI-2.12.3.jar"
# Phase 2 additions (compile = provided scope; both are present at
# runtime on the live server — Vault is a live plugin, JetBrains
# annotations ship with Paper): the ported 0.1.8 BukkitContext reads
# the Vault Economy service, and Paper API signatures reference the
# annotations.
VAULT_JAR="$HOME/workspace/vault-build/Vault-2.0.0.jar"
ANNOT_JAR="$HOME/workspace/m2-repo/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar"
JUNIT="$HOME/workspace/m2-repo/junit/junit/4.13.2/junit-4.13.2.jar"
HAMCREST="$HOME/workspace/m2-repo/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
VERSION="3.0.0"
JAR_OUT="$ROOT/ExcelMenu-$VERSION.jar"

for f in "$PAPER_API" "$PAPI_JAR" "$VAULT_JAR" "$ANNOT_JAR" "$JUNIT" "$HAMCREST"; do
  [ -f "$f" ] || { echo "MISSING dependency: $f" >&2; exit 1; }
done

LIBCP="$(find "$RIGLIBS" -name '*.jar' | sort | tr '\n' ':')"
CP="${LIBCP}${PAPER_API}:${PAPI_JAR}:${VAULT_JAR}:${ANNOT_JAR}"

BUILD="$ROOT/build"
rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/test-classes"

echo "== compile main =="
find "$ROOT/src/main/java" -name '*.java' > "$BUILD/sources.txt"
"$JDK/javac" --release 21 -encoding UTF-8 -cp "$CP" -d "$BUILD/classes" @"$BUILD/sources.txt"
cp -r "$ROOT/src/main/resources/." "$BUILD/classes/"

echo "== jar =="
printf 'Implementation-Title: ExcelMenu\nImplementation-Version: %s\n' "$VERSION" > "$BUILD/manifest.mf"
"$JDK/jar" --create --file "$JAR_OUT" --manifest "$BUILD/manifest.mf" --date=2026-10-31T00:00:00Z -C "$BUILD/classes" .

if [ "${1:-}" != "jar-only" ]; then
  echo "== compile tests =="
  find "$ROOT/src/test/java" -name '*.java' > "$BUILD/test-sources.txt"
  "$JDK/javac" --release 21 -encoding UTF-8 -cp "$CP:$JUNIT:$HAMCREST:$BUILD/classes" \
    -d "$BUILD/test-classes" @"$BUILD/test-sources.txt"

  echo "== run tests =="
  "$JDK/java" -Dharness.fixtures="$ROOT/fixtures" -Dharness.entries="$ROOT/src/main/resources/entries" \
    -Dharness.layouts="$ROOT/src/main/resources/layouts" \
    -Dharness.menus="$ROOT/src/main/resources/menus" \
    -cp "$BUILD/test-classes:$BUILD/classes:$ROOT/src/main/resources:$CP:$JUNIT:$HAMCREST" \
    org.junit.runner.JUnitCore \
    com.winkreyes.excelmenu.core.registry.EntryLoaderTest \
    com.winkreyes.excelmenu.core.registry.GuardResolutionTest \
    com.winkreyes.excelmenu.core.registry.DateWindowTest \
    com.winkreyes.excelmenu.core.registry.EntryRegistryTest \
    com.winkreyes.excelmenu.core.platform.PlatformDetectionTest \
    com.winkreyes.excelmenu.core.render.LayoutLoaderTest \
    com.winkreyes.excelmenu.core.render.ChestModelTest \
    com.winkreyes.excelmenu.core.render.FormModelTest \
    com.winkreyes.excelmenu.core.render.FormSessionTest \
    com.winkreyes.excelmenu.core.screens.TeleportConfirmationGateTest \
    com.winkreyes.excelmenu.core.screens.TeleportCoreTest \
    com.winkreyes.excelmenu.core.screens.ChildproofCoreTest \
    com.winkreyes.excelmenu.core.screens.AskCoreTest \
    com.winkreyes.excelmenu.core.screens.SkillsPerksCoreTest \
    com.winkreyes.excelmenu.core.screens.ConfirmTargetsTest \
    com.winkreyes.excelmenu.core.screens.CommandsPanelTest \
    com.winkreyes.excelmenu.bukkit.BundledEntriesTest \
    com.winkreyes.excelmenu.core.render.PinsModelTest \
    com.winkreyes.excelmenu.core.render.LayoutLoaderPinsTest \
    com.winkreyes.excelmenu.core.registry.OrderMergeTest \
    com.winkreyes.excelmenu.core.registry.PrefsCodecTest \
    com.winkreyes.excelmenu.core.MenuEditorCoreTest \
    com.winkreyes.excelmenu.harness.GoldenHarnessTest \
    com.winkreyes.excelmenu.harness.CatalogueConversionTest \
    com.winkreyes.excelmenu.harness.CatalogueHarnessTest
fi

echo "== built =="
sha256sum "$JAR_OUT"
ls -la "$JAR_OUT"
