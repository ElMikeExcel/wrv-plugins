#!/bin/bash
# ExcelSkillsPanel 0.2.2 deterministic build: JUnit first, then a
# timestamp-pinned jar, built twice and compared byte-for-byte.
# Excel — Wink Reyes Valley (Iris / Mike).
# VERSION + BUILD_ID below are the release knobs: bump both for
# the next version (the fingerprint gate fails the build if they
# disagree with BuildInfo.java's fragments).
set -euo pipefail
PROJ="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJ"
JDK="${JDK:-$HOME/workspace/.jdk25}"
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT

VERSION='0.2.2'
BUILD_DATE='2026-11-13'
BUILD_ID='EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9'
BUILD_TAG='c7d2e9'

python3 -c "import yaml; yaml.safe_load(open('$PROJ/plugin.yml'))" \
  || { echo 'YAML parse failure — refusing to build'; exit 1; }

PAPER_API="$HOME/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar"
MAGIC_API="$HOME/workspace/magic-scratch/downloads/Magic-11.2.5.jar"
MCMMO_API="$HOME/workspace/browser_downloads/sess-1860165046/browser-download-20261030T211431.488188443Z-60-mcMMO-2.3.003-WRV-XP-2.jar"
# House recipe (mirrors ExcelSummons' build): the shared lib set
# first — its legacy paper-api.jar is the compile surface every
# house plugin ships against — then the 26.3 API as fallback for
# newer classes, bungee chat, annotations, and the Magic + mcMMO
# public APIs (provider modules only; no third-party code is
# copied into the jar).
LIBS=$(find "$HOME/workspace/excelshops/build-libs" -name '*.jar' ! -name 'mockito*' ! -name 'byte-buddy*' | sort | tr '\n' ':')
CP="$LIBS$PAPER_API:$HOME/workspace/menujoin/lib/bungeecord-chat.jar:$HOME/workspace/menujoin/lib/annotations.jar:$MAGIC_API:$MCMMO_API"
JUNIT="$HOME/workspace/excelshops/build-libs/junit-platform-console-standalone-1.11.4.jar"

find "$PROJ/src" -name "*.java" | sort > "$OUT/src.txt"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$CP" -d "$OUT/classes" @"$OUT/src.txt"

mkdir -p "$OUT/test-classes"
find "$PROJ/tests" -name "*.java" | sort > "$OUT/testsrc.txt"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$OUT/classes:$CP:$JUNIT" -d "$OUT/test-classes" @"$OUT/testsrc.txt"
# The BedrockForms handler test builds forms against the REAL
# Cumulus classes inside the staged Geyser jar (loaded by the
# test in its own URLClassLoader — the jars are NOT on the
# harness classpath) and drives synthetic responses through the
# attached handler. Guava/Gson are Cumulus's runtime needs on
# Paper; the house lib set carries both.
GEYSER_JAR="$HOME/workspace/geyser-update/Geyser-Spigot-2.12.1-b1292.jar"
GUAVA_JAR="$HOME/workspace/excelshops/build-libs/guava-33.3.1-jre.jar"
GSON_JAR="$HOME/workspace/excelshops/build-libs/gson-2.11.0.jar"
"$JDK/bin/java" -Dskillspanel.test.geyserJar="$GEYSER_JAR" \
  -Dskillspanel.test.guavaJar="$GUAVA_JAR" -Dskillspanel.test.gsonJar="$GSON_JAR" \
  -jar "$JUNIT" execute --class-path "$OUT/test-classes:$OUT/classes:$CP" \
  --select-class com.winkreyes.excelskillspanel.PanelTest --details summary --disable-banner

cp "$PROJ/plugin.yml" "$OUT/classes/"
# Forensic fingerprint (FINGERPRINT-STANDARD Layer B): per-build
# marker, injected at build time so it survives source edits;
# verified post-build. The build ID is PINNED for a version
# (keeps the double-build byte-identical); release tooling
# rotates seq/hex per distributed build.
printf '%s\n' "$BUILD_ID" > "$OUT/classes/excel-build.txt"
STAGE="$OUT/stage"; mkdir -p "$STAGE"
cp -r "$OUT/classes/." "$STAGE/"
find "$STAGE" -exec touch -d "$BUILD_DATE 00:00:00 UTC" {} +
(cd "$STAGE" && find . -type f | sed 's|^\./||' | LC_ALL=C sort > "$OUT/entries.txt")
for name in "ExcelSkillsPanel-$VERSION.jar" "ExcelSkillsPanel-$VERSION-repro.jar"; do
  JAR_PATH="$PROJ/$name"; rm -f "$JAR_PATH"
  (cd "$STAGE" && "$JDK/bin/jar" --create --file "$JAR_PATH" --no-manifest --date="$BUILD_DATE"T00:00:00Z @"$OUT/entries.txt")
  sha256sum "$JAR_PATH"
done
cmp "$PROJ/ExcelSkillsPanel-$VERSION.jar" "$PROJ/ExcelSkillsPanel-$VERSION-repro.jar" \
  && echo 'double-build: byte-identical'
# Fingerprint gate: the shipped jar MUST carry its Layer B build
# marker in BOTH homes (resource file + BuildInfo constants).
unzip -p "$PROJ/ExcelSkillsPanel-$VERSION.jar" excel-build.txt | grep -q "^${BUILD_ID}$" \
  || { echo 'FINGERPRINT GATE FAIL: excel-build.txt missing/incorrect in jar'; exit 1; }
unzip -p "$PROJ/ExcelSkillsPanel-$VERSION.jar" com/winkreyes/excelskillspanel/BuildInfo.class \
  | strings | grep -q 'EXC-SKILLSPANEL-' \
  || { echo 'FINGERPRINT GATE FAIL: BuildInfo fragments missing in jar'; exit 1; }
unzip -p "$PROJ/ExcelSkillsPanel-$VERSION.jar" com/winkreyes/excelskillspanel/BuildInfo.class \
  | strings | grep -q "$BUILD_TAG" \
  || { echo 'FINGERPRINT GATE FAIL: BuildInfo tag fragment missing in jar'; exit 1; }
echo "fingerprint gate: PASS (excel-build.txt + BuildInfo fragments, build $BUILD_ID)"
