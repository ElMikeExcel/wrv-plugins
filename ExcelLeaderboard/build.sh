#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
RUNTIME="${RUNTIME:-/home/hatch/workspace/excelbuildmode-pilot/runtime-server}"
JAVA_HOME="${JAVA_HOME:-/home/hatch/workspace/.jdk25}"
LIBRARIES="$RUNTIME/libraries"
[[ -f "$LIBRARIES/io/papermc/paper/paper-api/26.2.build.123-stable/paper-api-26.2.build.123-stable.jar" ]] || { echo 'Exact Paper API missing'; exit 1; }
CP=$(find "$LIBRARIES" -name '*.jar' -type f | sort | paste -sd: -)
rm -rf target/classes target/test-classes
mkdir -p target/classes target/test-classes
find src/main/java -name '*.java' | sort > target/main-sources.txt
"$JAVA_HOME/bin/javac" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx256M -encoding UTF-8 --release 25 -proc:none -cp "$CP" -d target/classes @target/main-sources.txt
cp src/main/resources/* target/classes/
find src/test/java -name '*.java' | sort > target/test-sources.txt
"$JAVA_HOME/bin/javac" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx256M -encoding UTF-8 --release 25 -proc:none -cp "target/classes:$CP" -d target/test-classes @target/test-sources.txt
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.leaderboard.BoardMathTests
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.leaderboard.LedgerTests
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.leaderboard.VaultModeTests
"$JAVA_HOME/bin/jar" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx128M --create --file target/ExcelLeaderboard-1.0.jar --date=2026-10-14T03:00:00Z -C target/classes .
sha256sum target/ExcelLeaderboard-1.0.jar
