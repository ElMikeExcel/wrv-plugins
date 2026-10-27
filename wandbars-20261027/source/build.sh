#!/bin/bash
# Build ExcelWandBars 0.3.4-sp-display-candidate; no deployment implied (deterministic: fixed jar date, sorted inputs).
# Manual javac route, same as the other Excel plugins. Magic 11.2.5's jar is
# COMPILE-ONLY (soft-depend at runtime); nothing from Magic or Geyser is packaged.
# 0.2.7: Geyser-Spigot's jar is compile-only too (GeyserTapHook, loaded by name only when Geyser runs).
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/path/to/jdk}
LIBS=${LIBS:-/path/to/build-input}
MAGIC=${MAGIC:-/path/to/build-input}
CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
RT=${RT:-/path/to/build-input}
CP="$CP:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar"
GEYSER=${GEYSER:-/path/to/build-input}
CP="$CP:$MAGIC:$GEYSER"
# Optional explicit, reviewed classpath for offline recovery builds.
if [ -n "${CP_FILE:-}" ]; then CP=$(cat "$CP_FILE"); fi
rm -rf build/classes && mkdir -p build/classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java' | sort)
cp plugin.yml config.yml spellbook.yml build/classes/
OUT=${OUT:-../artifacts/ExcelWandBars-0.3.4-sp-display-candidate.jar}
mkdir -p "$(dirname "$OUT")"
rm -f "$OUT"
"$JDK/bin/jar" --create --file "$OUT" --date=2026-10-27T14:00:00Z -C build/classes .
sha256sum "$OUT"

