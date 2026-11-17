#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
JDK=/workspace/scratch/9e01236aa418/leaderboard-runtime-prep/jdk
M2=/workspace/scratch/9e01236aa418/leaderboard-mobile-candidate/.m2/repository
CP=$(find "$M2" -name '*.jar' | sort | paste -sd:)
CP="$CP:/workspace/shared/wand-fix/deps/Geyser-Spigot-2.12.0-b1269.jar"
mkdir -p build/classes
"$JDK/bin/javac" --release 21 -proc:none -cp "$CP" -d build/classes $(find src -name '*.java' | sort)
cp plugin.yml config.yml build/classes/
"$JDK/bin/jar" --create --file WRVWandDiagnostic-0.1.0-offline.jar --date=2026-10-27T00:00:00Z -C build/classes .
sha256sum WRVWandDiagnostic-0.1.0-offline.jar
