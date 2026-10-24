#!/bin/bash
# Build ExcelDeathRecord-<version>.jar deterministically.
# Usage: ./build.sh [output.jar]
# Env: JDK (JDK 25 home), LIBS (folder of Paper 26.x API jars + deps),
#      VAULT (VaultAPI jar, compile only; 0.3.0 Lost & Found).
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
LIBS=${LIBS:-/workspace/build-libs-wrv/paper26.2}
VER=0.3.0
OUT="$(realpath -m "${1:-build/ExcelDeathRecord-$VER.jar}")"
VAULT=${VAULT:-/workspace/build-libs-wrv/VaultAPI-1.7.jar}
CP=$(find "$LIBS" -name '*.jar' | sort | tr '\n' ':')$VAULT
rm -rf build/classes && mkdir -p build/classes "$(dirname "$OUT")"
"$JDK/bin/javac" --release 21 -proc:none -nowarn -Xlint:-options -cp "$CP" -d build/classes \
  $(find src/com/winkreyes/exceldeathrecord -name '*.java' | sort)
cp src/plugin.yml src/config.yml build/classes/
rm -f "$OUT"
(cd build/classes && find . -type f | sort | sed 's#^\./##' > ../entries.txt \
  && "$JDK/bin/jar" --create --file "$OUT" --date=2026-10-24T00:00:00Z @../entries.txt)
echo "built: $OUT"
sha256sum "$OUT"
