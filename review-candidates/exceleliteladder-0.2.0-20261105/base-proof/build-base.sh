#!/bin/bash
# Base proof: rebuild UNMODIFIED 0.1.2 sources with this VM's toolchain and
# compare against the jar of record (7ea61276..., 31,403 B).
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_HOME=/home/hatch/workspace/.jdk25
PATH="$JAVA_HOME/bin:$PATH"
PAPER=/home/hatch/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar
L=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
CP="$PAPER"
for j in $(find $L/net/kyori $L/org/jspecify $L/com/google $L/net/md-5 $L/org/yaml $L/org/slf4j $L/org/joml $L/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
WORK="$HERE/base-proof/work"
rm -rf "$WORK"; mkdir -p "$WORK/classes"
find "$HERE/src" -name '*.java' > "$WORK/sources.txt"
javac -encoding UTF-8 -cp "$CP" -d "$WORK/classes" @"$WORK/sources.txt"
cp "$HERE/plugin.yml" "$HERE/config.yml" "$WORK/classes/"
(cd "$WORK/classes" && "$JAVA_HOME/bin/jar" --create --file "$HERE/base-proof/ExcelEliteLadder-0.1.2-rebuilt.jar" --date=2026-11-01T00:00:00-05:00 .)
stat -c '%s bytes' "$HERE/base-proof/ExcelEliteLadder-0.1.2-rebuilt.jar"
sha256sum "$HERE/base-proof/ExcelEliteLadder-0.1.2-rebuilt.jar"
echo "jar of record: 31403 bytes, sha256 7ea61276a543820351cae016c8ece4f927b8d430ae4cf71a4987b6d4cecbd7fb"
