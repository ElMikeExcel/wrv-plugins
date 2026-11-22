#!/bin/bash
# Build ExcelEMLeash-0.3.0.jar (deterministic: two builds byte-identical).
# Manual javac route, same as the other Excel plugins: house build-libs
# paper-api + the pinned EliteMobs jar (libs/, actually EM 10.9.14
# content) as compile-only dependencies.
set -euo pipefail
cd "$(dirname "$0")"
JDK="${JDK:-$HOME/workspace/.jdk25}"
PAPER_API="${PAPER_API:-$HOME/workspace/excelshops/build-libs/paper-api.jar}"
EM_JAR="${EM_JAR:-libs/EliteMobs-10.9.10.jar}"
OUT=ExcelEMLeash-0.3.0.jar
export TZ=America/Chicago
rm -rf classes jarstage
mkdir -p classes jarstage
BL="$HOME/workspace/excelshops/build-libs"
"$JDK/bin/javac" -encoding UTF-8 -cp "$PAPER_API:$EM_JAR:libs/jetbrains-annotations-stub.jar:$BL/adventure-api-4.20.0.jar:$BL/adventure-key-4.20.0.jar:$BL/gson-2.11.0.jar:$BL/guava-33.3.1-jre.jar:$BL/snakeyaml-2.2.jar:$BL/examination-api-1.3.0.jar:$BL/examination-string-1.3.0.jar:$BL/option-1.0.0.jar" -d classes \
  src/src/com/winkreyes/excelemleash/*.java
find classes -exec touch -d "2026-01-01 00:00:00" {} +
cp -r classes/* jarstage/
cp src/plugin.yml src/config.yml jarstage/
(cd jarstage && find . -type f -printf '%P\n' | LC_ALL=C sort | tr '\n' '\0' | \
  xargs -0 "$JDK/bin/jar" --create --file "../$OUT" --date=2026-01-01T00:00:00-06:00 --no-manifest -C . 2>/dev/null || true)
if [ ! -s "$OUT" ]; then
  (cd jarstage && "$JDK/bin/jar" --create --file "../$OUT" --date=2026-01-01T00:00:00-06:00 --no-manifest .)
fi
sha256sum "$OUT"
ls -la "$OUT"
