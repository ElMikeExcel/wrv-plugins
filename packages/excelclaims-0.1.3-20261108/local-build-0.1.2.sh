#!/bin/bash
# ExcelClaims 0.1.2 deterministic build: YAML gate, JUnit suite, consumer
# link-check (the four compile-time GP adapters, compiled UNCHANGED
# against the facade classes), then a timestamp-pinned jar built twice
# and compared byte-for-byte, plus the source zip.
# Excel — Wink Reyes Valley (Iris / Mike).
set -euo pipefail
PROJ="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJ"
JDK="${JDK:-$HOME/workspace/.jdk25}"
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT

python3 -c "import yaml; yaml.safe_load(open('$PROJ/plugin.yml')); yaml.safe_load(open('$PROJ/config.yml'))" \
  || { echo 'YAML parse failure — refusing to build'; exit 1; }

PAPER="$HOME/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar"
CP="$PAPER"
for dep in "$PROJ"/libs/*.jar; do CP="$CP:$dep"; done
JUNIT="$HOME/workspace/excelshops/build-libs/junit-platform-console-standalone-1.11.4.jar"

# ---- main sources
find "$PROJ/src" -name "*.java" | sort > "$OUT/src.txt"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$CP" -d "$OUT/classes" @"$OUT/src.txt"

# ---- unit tests
mkdir -p "$OUT/test-classes"
find "$PROJ/tests" -name "*.java" | sort > "$OUT/testsrc.txt"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$OUT/classes:$CP:$JUNIT" -d "$OUT/test-classes" @"$OUT/testsrc.txt"
"$JDK/bin/java" -jar "$JUNIT" execute --class-path "$OUT/test-classes:$OUT/classes:$CP" \
  --select-package com.winkreyes.excelclaims --details summary --disable-banner

# ---- link-check: consumer GP adapters, compiled UNCHANGED against the facade.
# Landmarks + AutoSort adapter pairs are self-contained; Protect and
# AuditLog adapters get compile-only stubs for their own plugins'
# internal classes (linkcheck/stubs) — the GP-facing surface under
# test is the consumers' real code, byte for byte.
mkdir -p "$OUT/linkcheck"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$OUT/classes:$CP" -d "$OUT/linkcheck" \
  "$HOME/workspace/landmarks-build/src/com/winkreyes/excellandmarks/hook/GpAdapter.java" \
  "$HOME/workspace/landmarks-build/src/com/winkreyes/excellandmarks/hook/GpHook.java"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$OUT/classes:$CP" -d "$OUT/linkcheck" \
  "$HOME/workspace/autosort-plugin/src/com/winkreyes/excelautosort/hook/GpRights.java" \
  "$HOME/workspace/autosort-plugin/src/com/winkreyes/excelautosort/hook/GpRightsHook.java"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$OUT/classes:$CP" -d "$OUT/linkcheck" \
  "$PROJ"/linkcheck/stubs/com/winkreyes/excelprotect/*.java \
  "$HOME/workspace/excelprotect/src/ExcelProtect/src/com/winkreyes/excelprotect/GpAdapter.java"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$OUT/classes:$CP" -d "$OUT/linkcheck" \
  "$PROJ"/linkcheck/stubs/com/winkreyes/auditlog/*.java \
  "$HOME/workspace/excelauditlog/src/ExcelAuditLog/src/com/winkreyes/auditlog/GpBridge.java"
echo 'link-check: PASS (Landmarks GpHook, AutoSort GpRightsHook, Protect GpAdapter, AuditLog GpBridge — all unchanged)'

# ---- deterministic jar
cp "$PROJ/plugin.yml" "$PROJ/config.yml" "$OUT/classes/"
# Forensic fingerprint (FINGERPRINT-STANDARD Layer B): per-build marker,
# injected at build time; verified post-build. The build ID is PINNED
# for a version (keeps the double-build byte-identical); release
# tooling rotates seq/hex per distributed build.
BUILD_ID='EXC-CLAIMS-0.1.2-WRV-20261107-03-3b8f2d'
printf '%s\n' "$BUILD_ID" > "$OUT/classes/excel-build.txt"
STAGE="$OUT/stage"; mkdir -p "$STAGE"
cp -r "$OUT/classes/." "$STAGE/"
find "$STAGE" -exec touch -d '2026-11-07 00:00:00 UTC' {} +
(cd "$STAGE" && find . -type f | sed 's|^\./||' | LC_ALL=C sort > "$OUT/entries.txt")
for name in ExcelClaims-0.1.2.jar ExcelClaims-0.1.2-repro.jar; do
  JAR_PATH="$PROJ/$name"; rm -f "$JAR_PATH"
  (cd "$STAGE" && "$JDK/bin/jar" --create --file "$JAR_PATH" --no-manifest --date=2026-11-07T00:00:00Z @"$OUT/entries.txt")
  sha256sum "$JAR_PATH"
done
cmp "$PROJ/ExcelClaims-0.1.2.jar" "$PROJ/ExcelClaims-0.1.2-repro.jar" \
  && echo 'double-build: byte-identical'
# Fingerprint gate: the shipped jar MUST carry its Layer B build marker
# in BOTH homes (resource file + BuildInfo constant fragments).
unzip -p "$PROJ/ExcelClaims-0.1.2.jar" excel-build.txt | grep -q "^${BUILD_ID}$" \
  || { echo 'FINGERPRINT GATE FAIL: excel-build.txt missing/incorrect in jar'; exit 1; }
unzip -p "$PROJ/ExcelClaims-0.1.2.jar" com/winkreyes/excelclaims/BuildInfo.class \
  | strings | grep -q 'EXC-CLAIMS-' \
  || { echo 'FINGERPRINT GATE FAIL: BuildInfo fragments missing in jar'; exit 1; }
unzip -p "$PROJ/ExcelClaims-0.1.2.jar" com/winkreyes/excelclaims/BuildInfo.class \
  | strings | grep -q '3b8f2d' \
  || { echo 'FINGERPRINT GATE FAIL: BuildInfo tag fragment missing in jar'; exit 1; }
echo "fingerprint gate: PASS (excel-build.txt + BuildInfo fragments, build $BUILD_ID)"
# Plugin identity gate: the jar must register AS GriefPrevention (the
# facade contract) while branding ExcelClaims.
unzip -p "$PROJ/ExcelClaims-0.1.2.jar" plugin.yml | grep -q '^name: GriefPrevention$' \
  || { echo 'IDENTITY GATE FAIL: plugin.yml name is not GriefPrevention'; exit 1; }
unzip -p "$PROJ/ExcelClaims-0.1.2.jar" plugin.yml | grep -q '^main: me.ryanhamshire.GriefPrevention.GriefPrevention$' \
  || { echo 'IDENTITY GATE FAIL: plugin.yml main is not the facade class'; exit 1; }
echo 'identity gate: PASS (registers as GriefPrevention, facade main class)'

# ---- source zip (deterministic)
SRCSTAGE="$OUT/srcstage/ExcelClaims-0.1.2-src"; mkdir -p "$SRCSTAGE"
cp -r "$PROJ/src" "$PROJ/tests" "$PROJ/linkcheck" "$SRCSTAGE/"
cp "$PROJ/plugin.yml" "$PROJ/config.yml" "$PROJ/local-build.sh" "$PROJ/local-build-0.1.1.sh" "$PROJ/local-build-0.1.2.sh" "$SRCSTAGE/"
[ -f "$PROJ/DESIGN-0.1.2-per-player-expiration.md" ] && cp "$PROJ/DESIGN-0.1.2-per-player-expiration.md" "$SRCSTAGE/"
[ -f "$PROJ/INVENTORY.md" ] && cp "$PROJ/INVENTORY.md" "$SRCSTAGE/"
[ -f "$PROJ/SOURCE.md" ] && cp "$PROJ/SOURCE.md" "$SRCSTAGE/"
[ -f "$PROJ/BUILD-RECORD.md" ] && cp "$PROJ/BUILD-RECORD.md" "$SRCSTAGE/"
find "$SRCSTAGE" -exec touch -d '2026-11-07 00:00:00 UTC' {} +
(cd "$OUT/srcstage" && find . -type f | sed 's|^\./||' | LC_ALL=C sort > "$OUT/srcentries.txt")
rm -f "$PROJ/ExcelClaims-0.1.2-src.zip"
(cd "$OUT/srcstage" && "$JDK/bin/jar" --create --file "$PROJ/ExcelClaims-0.1.2-src.zip" --no-manifest --date=2026-11-07T00:00:00Z @"$OUT/srcentries.txt")
sha256sum "$PROJ/ExcelClaims-0.1.2-src.zip"
ls -l "$PROJ/ExcelClaims-0.1.2.jar" "$PROJ/ExcelClaims-0.1.2-src.zip"
