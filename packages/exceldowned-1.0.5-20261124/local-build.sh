#!/bin/bash
# ExcelDowned deterministic build: JUnit first, then a timestamp-pinned jar,
# built twice and compared byte-for-byte. Excel — Wink Reyes Valley (Iris / Mike).
# VERSION + BUILD_ID below are the release knobs: bump both for the next
# version (the fingerprint gate fails the build if they disagree with
# BuildInfo.java's fragments).
set -euo pipefail
PROJ="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJ"
JDK="${JDK:-$HOME/workspace/.jdk25}"
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT

VERSION='1.0.5'
BUILD_DATE='2026-11-24'
BUILD_ID='EXC-DOWNED-1.0.5-WRV-20261124-03-deathdefer'
BUILD_TAG='deathdefer'

python3 -c "import yaml; yaml.safe_load(open('$PROJ/plugin.yml')); yaml.safe_load(open('$PROJ/config.yml'))" \
  || { echo 'YAML parse failure — refusing to build'; exit 1; }

PAPER_API="$HOME/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar"
MAGIC_API="$HOME/workspace/magic-scratch/downloads/Magic-11.2.5.jar"
# EliteMobs pin (Raise Ally seam, D107): EmClericGate compiles against
# the live-pin 10.9.14 jar ONLY — the bridge's pinned-jar discipline.
# Any other hash refuses the build (a moved ACS internal is a
# re-scope, never a silent recompile).
EM_JAR="$HOME/workspace/abilitybridge-rename-build-20261114/EliteMobs-10.9.14.jar"
EM_SHA256='4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd'
echo "$EM_SHA256  $EM_JAR" | sha256sum -c - >/dev/null \
  || { echo 'EliteMobs pin mismatch — refusing to build (see D107 spike)'; exit 1; }
# House recipe (mirrors ExcelAutoSort's build): the shared lib set
# first — its legacy paper-api.jar is the compile surface every house
# plugin ships against — then the 26.3 API as fallback for newer
# classes, bungee chat, annotations, and the Magic API. Consequence:
# 26.3-only constants (EntityType.HAPPY_GHAST, Material.RED_HARNESS)
# are NOT visible at compile time; the source resolves those two by
# name at runtime instead (see SummonsListener.applyHarness).
LIBS=$(find "$HOME/workspace/excelshops/build-libs" -name '*.jar' ! -name 'mockito*' ! -name 'byte-buddy*' | sort | tr '\n' ':')
EXCELITEMS_JAR="$HOME/workspace/excelitems-build-20261118/ExcelItems-0.1.0.jar"
EXCELITEMS_SHA256='85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d'
echo "$EXCELITEMS_SHA256  $EXCELITEMS_JAR" | sha256sum -c - >/dev/null \
  || { echo 'ExcelItems pin mismatch — refusing to build (M1 hard depend)'; exit 1; }
CP="$LIBS$PAPER_API:$HOME/workspace/menujoin/lib/bungeecord-chat.jar:$HOME/workspace/menujoin/lib/annotations.jar:$MAGIC_API:$EM_JAR:$EXCELITEMS_JAR"
JUNIT="$HOME/workspace/excelshops/build-libs/junit-platform-console-standalone-1.11.4.jar"

find "$PROJ/src" -name "*.java" | sort > "$OUT/src.txt"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$CP" -d "$OUT/classes" @"$OUT/src.txt"

mkdir -p "$OUT/test-classes"
find "$PROJ/tests" -name "*.java" | sort > "$OUT/testsrc.txt"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$OUT/classes:$CP:$JUNIT" -d "$OUT/test-classes" @"$OUT/testsrc.txt"
"$JDK/bin/java" -jar "$JUNIT" execute --class-path "$OUT/test-classes:$OUT/classes:$CP" \
  --select-class com.winkreyes.exceldowned.DownedTest --details summary --disable-banner

cp "$PROJ/plugin.yml" "$PROJ/config.yml" "$OUT/classes/"
# Forensic fingerprint (FINGERPRINT-STANDARD Layer B): per-build marker,
# injected at build time so it survives source edits; verified post-build.
# The build ID is PINNED for a version (keeps the double-build
# byte-identical); release tooling rotates seq/hex per distributed build.
printf '%s\n' "$BUILD_ID" > "$OUT/classes/excel-build.txt"
STAGE="$OUT/stage"; mkdir -p "$STAGE"
cp -r "$OUT/classes/." "$STAGE/"
find "$STAGE" -exec touch -d "$BUILD_DATE 00:00:00 UTC" {} +
(cd "$STAGE" && find . -type f | sed 's|^\./||' | LC_ALL=C sort > "$OUT/entries.txt")
for name in "ExcelDowned-$VERSION.jar" "ExcelDowned-$VERSION-repro.jar"; do
  JAR_PATH="$PROJ/$name"; rm -f "$JAR_PATH"
  (cd "$STAGE" && "$JDK/bin/jar" --create --file "$JAR_PATH" --no-manifest --date="$BUILD_DATE"T00:00:00Z @"$OUT/entries.txt")
  sha256sum "$JAR_PATH"
done
cmp "$PROJ/ExcelDowned-$VERSION.jar" "$PROJ/ExcelDowned-$VERSION-repro.jar" \
  && echo 'double-build: byte-identical'
# Fingerprint gate: the shipped jar MUST carry its Layer B build marker
# in BOTH homes (resource file + BuildInfo constant fragments).
unzip -p "$PROJ/ExcelDowned-$VERSION.jar" excel-build.txt | grep -q "^${BUILD_ID}$" \
  || { echo 'FINGERPRINT GATE FAIL: excel-build.txt missing/incorrect in jar'; exit 1; }
unzip -p "$PROJ/ExcelDowned-$VERSION.jar" com/winkreyes/exceldowned/BuildInfo.class \
  | strings | grep -q 'EXC-DOWNED-' \
  || { echo 'FINGERPRINT GATE FAIL: BuildInfo fragments missing in jar'; exit 1; }
unzip -p "$PROJ/ExcelDowned-$VERSION.jar" com/winkreyes/exceldowned/BuildInfo.class \
  | strings | grep -q "$BUILD_TAG" \
  || { echo 'FINGERPRINT GATE FAIL: BuildInfo tag fragment missing in jar'; exit 1; }
echo "fingerprint gate: PASS (excel-build.txt + BuildInfo fragments, build $BUILD_ID)"
