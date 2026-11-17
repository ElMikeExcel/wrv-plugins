#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
JDK=/workspace/scratch/9e01236aa418/leaderboard-runtime-prep/jdk
CP="build/classes:/workspace/shared/wand-fix/deps/Geyser-Spigot-2.12.0-b1269.jar:$(find /workspace/scratch/9e01236aa418/leaderboard-runtime-prep/server/libraries -name '*.jar' | paste -sd:)"
mkdir -p build/tests
"$JDK/bin/javac" --release 21 -proc:none -cp "$CP" -d build/tests $(find test -name '*.java')
"$JDK/bin/java" -cp "build/tests:$CP" com.winkreyes.wanddiagnostic.DiagnosticTest
"$JDK/bin/java" -cp "build/tests:$CP" com.winkreyes.wanddiagnostic.PacketTest
