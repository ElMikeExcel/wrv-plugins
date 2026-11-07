#!/bin/bash
# ExcelAutoSort deterministic build: JUnit first, then a timestamp-pinned jar,
# built twice and compared byte-for-byte. Excel — Wink Reyes Valley (Iris / Mike).
set -euo pipefail
PROJ="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJ"
JDK="${JDK:-$HOME/workspace/.jdk25}"
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT

python3 -c "import yaml; yaml.safe_load(open('$PROJ/plugin.yml')); yaml.safe_load(open('$PROJ/config.yml'))" \
  || { echo 'YAML parse failure — refusing to build'; exit 1; }

LIBS=$(find "$HOME/workspace/excelshops/build-libs" -name '*.jar' ! -name 'mockito*' ! -name 'byte-buddy*' | sort | tr '\n' ':')
CP="$LIBS$HOME/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar:$HOME/workspace/menujoin/lib/bungeecord-chat.jar:$HOME/workspace/menujoin/lib/annotations.jar"
for dep in "$PROJ"/libs/*.jar; do CP="$CP:$dep"; done
JUNIT="$HOME/workspace/excelshops/build-libs/junit-platform-console-standalone-1.11.4.jar"

find "$PROJ/src" -name "*.java" | sort > "$OUT/src.txt"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$CP" -d "$OUT/classes" @"$OUT/src.txt"

mkdir -p "$OUT/test-classes"
find "$PROJ/tests" -name "*.java" | sort > "$OUT/testsrc.txt"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$OUT/classes:$CP:$JUNIT" -d "$OUT/test-classes" @"$OUT/testsrc.txt"
"$JDK/bin/java" -jar "$JUNIT" execute --class-path "$OUT/test-classes:$OUT/classes:$CP" \
  --select-class com.winkreyes.excelautosort.AutoSortTest --details summary --disable-banner

cp "$PROJ/plugin.yml" "$PROJ/config.yml" "$OUT/classes/"
# Forensic fingerprint (FINGERPRINT-STANDARD Layer B): per-build marker,
# injected at build time so it survives source edits; verified post-build.
# The build ID is PINNED for a version (keeps the double-build
# byte-identical); release tooling rotates seq/hex per distributed build.
BUILD_ID='EXC-AUTOSORT-0.1.0-WRV-20261106-04-f96f93'
printf '%s\n' "$BUILD_ID" > "$OUT/classes/excel-build.txt"
STAGE="$OUT/stage"; mkdir -p "$STAGE"
cp -r "$OUT/classes/." "$STAGE/"
find "$STAGE" -exec touch -d '2026-11-06 00:00:00 UTC' {} +
(cd "$STAGE" && find . -type f | sed 's|^\./||' | LC_ALL=C sort > "$OUT/entries.txt")
for name in ExcelAutoSort-0.1.0.jar ExcelAutoSort-0.1.0-repro.jar; do
  JAR_PATH="$PROJ/$name"; rm -f "$JAR_PATH"
  (cd "$STAGE" && "$JDK/bin/jar" --create --file "$JAR_PATH" --no-manifest --date=2026-11-06T00:00:00Z @"$OUT/entries.txt")
  sha256sum "$JAR_PATH"
done
cmp "$PROJ/ExcelAutoSort-0.1.0.jar" "$PROJ/ExcelAutoSort-0.1.0-repro.jar" \
  && echo 'double-build: byte-identical'
# Fingerprint gate: the shipped jar MUST carry its Layer B build marker
# in BOTH homes (resource file + BuildInfo constant fragments).
unzip -p "$PROJ/ExcelAutoSort-0.1.0.jar" excel-build.txt | grep -q "^${BUILD_ID}$" \
  || { echo 'FINGERPRINT GATE FAIL: excel-build.txt missing/incorrect in jar'; exit 1; }
unzip -p "$PROJ/ExcelAutoSort-0.1.0.jar" com/winkreyes/excelautosort/BuildInfo.class \
  | strings | grep -q 'EXC-AUTOSORT-' \
  || { echo 'FINGERPRINT GATE FAIL: BuildInfo fragments missing in jar'; exit 1; }
unzip -p "$PROJ/ExcelAutoSort-0.1.0.jar" com/winkreyes/excelautosort/BuildInfo.class \
  | strings | grep -q 'f96f93' \
  || { echo 'FINGERPRINT GATE FAIL: BuildInfo tag fragment missing in jar'; exit 1; }
echo "fingerprint gate: PASS (excel-build.txt + BuildInfo fragments, build $BUILD_ID)"
