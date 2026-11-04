#!/usr/bin/env bash
# Build ExcelAutoSortBridge-0.1.0.jar (deterministic: two builds byte-identical).
# Manual javac route, same shape as the ExcelAimAssist 0.1.4 build:
# Paper API 26.3 from the EliteMobs rig library set; PlaceholderAPI
# 2.12.3 is COMPILE-TIME ONLY (the expansion class loads solely when
# PlaceholderAPI is present at runtime). Jar date pinned.
set -euo pipefail
cd "$(dirname "$0")"
JDK="$HOME/workspace/.jdk25/bin"
RIGLIBS="$HOME/workspace/elitemobs-10.9.12/rig-boot/libraries"
PAPER_API="$RIGLIBS/io/papermc/paper/paper-api/26.3.build.231-stable/paper-api-26.3.build.231-stable.jar"
PAPI="$HOME/workspace/minecraft-server/plugins/PlaceholderAPI-2.12.3.jar"
for f in "$PAPER_API" "$PAPI"; do [ -f "$f" ] || { echo "MISSING dependency: $f" >&2; exit 1; }; done
LIBCP="$(find "$RIGLIBS" -name '*.jar' | sort | tr '\n' ':')"
CP="${LIBCP}${PAPER_API}:${PAPI}"

rm -rf build && mkdir -p build/classes build/test-classes
echo "== compile main =="
"$JDK/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp plugin.yml build/classes/

if [ "${1:-}" != "jar-only" ]; then
  echo "== compile + run tests =="
  "$JDK/javac" --release 21 -proc:none -nowarn -cp "build/classes" -d build/test-classes $(find tests -name '*.java')
  "$JDK/java" -cp "build/classes:build/test-classes" com.winkreyes.excelautosortbridge.AutoSortStatusTest
fi

OUT="../ExcelAutoSortBridge-0.1.0.jar"
rm -f "$OUT"
"$JDK/jar" --create --file "$OUT" --date=2026-11-03T00:00:00Z -C build/classes .
sha256sum "$OUT"
ls -l "$OUT"
