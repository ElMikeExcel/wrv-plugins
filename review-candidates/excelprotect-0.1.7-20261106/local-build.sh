#!/bin/bash
# ExcelProtect deterministic build: JUnit first, then a timestamp-pinned jar.
# Excel — Wink Reyes Valley
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
# WorldEdit API (0.1.6 hook compile deps; softdepend — never bundled, runtime fails soft without WE)
for WEJAR in "$HOME/workspace/.m2/repository/com/sk89q/worldedit/worldedit-core/7.2.7/worldedit-core-7.2.7.jar" "$HOME/workspace/.m2/repository/com/sk89q/worldedit/worldedit-bukkit/7.2.0-SNAPSHOT/worldedit-bukkit-7.2.0-SNAPSHOT.jar"; do
  if [ -f "$WEJAR" ]; then CP="$CP:$WEJAR"; fi
done
JUNIT="$HOME/workspace/excelshops/build-libs/junit-platform-console-standalone-1.11.4.jar"

find "$PROJ/src" -name "*.java" | sort > "$OUT/src.txt"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$CP" -d "$OUT/classes" @"$OUT/src.txt"

mkdir -p "$OUT/test-classes"
find "$PROJ/tests" -name "*.java" | sort > "$OUT/testsrc.txt"
"$JDK/bin/javac" --release 21 -proc:none -encoding UTF-8 -cp "$OUT/classes:$CP:$JUNIT" -d "$OUT/test-classes" @"$OUT/testsrc.txt"
"$JDK/bin/java" -jar "$JUNIT" execute --class-path "$OUT/test-classes:$OUT/classes:$CP" \
  --select-class com.winkreyes.excelprotect.ProtectTest --details summary --disable-banner

cp "$PROJ/plugin.yml" "$PROJ/config.yml" "$OUT/classes/"
# Forensic fingerprint (FINGERPRINT-STANDARD Layer B): per-build marker,
# injected at build time so it survives source edits; verified post-build.
# The build ID is PINNED for a version (keeps the double-build
# byte-identical); release tooling rotates seq/hex per distributed build.
printf 'EXC-PROTECT-0.1.7-WRV-20261106-01-c017f1\n' > "$OUT/classes/excel-build.txt"
STAGE="$OUT/stage"; mkdir -p "$STAGE"
cp -r "$OUT/classes/." "$STAGE/"
find "$STAGE" -exec touch -d '2026-11-06 00:00:00 UTC' {} +
JAR_PATH="$PROJ/ExcelProtect-0.1.7.jar"; rm -f "$JAR_PATH"
# Deterministic packaging: sorted entry list, pinned entry dates, no manifest.
(cd "$STAGE" && find . -type f | sed 's|^\./||' | LC_ALL=C sort > "$OUT/entries.txt")
(cd "$STAGE" && "$JDK/bin/jar" --create --file "$JAR_PATH" --no-manifest --date=2026-11-06T00:00:00Z @"$OUT/entries.txt")
sha256sum "$JAR_PATH"
# Fingerprint gate: the shipped jar MUST carry its Layer B build marker.
unzip -p "$JAR_PATH" excel-build.txt | grep -q '^EXC-PROTECT-0.1.7-WRV-20261106-01-c017f1$' \
  || { echo 'FINGERPRINT GATE FAIL: excel-build.txt missing/incorrect in jar'; exit 1; }
echo 'fingerprint gate: PASS (excel-build.txt present)'
