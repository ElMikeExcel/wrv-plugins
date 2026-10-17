#!/usr/bin/env bash
set -euo pipefail
cd /workspace/ebmode13/repo/ExcelBuildMode
RUNTIME=/workspace/wrv063/scratch
JAVA_HOME=/workspace/wrv019/jdk25
LIBRARIES="$RUNTIME/libraries"
[[ -f "$LIBRARIES/io/papermc/paper/paper-api/26.2.build.133-stable/paper-api-26.2.build.133-stable.jar" ]] || { echo 'Exact Paper API missing'; exit 1; }
CP=$(find "$LIBRARIES" -name '*.jar' -type f | sort | paste -sd: -)
rm -rf target/classes target/test-classes
mkdir -p target/classes target/test-classes
find src/main/java -name '*.java' | sort > target/main-sources.txt
"$JAVA_HOME/bin/javac" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx256M -encoding UTF-8 --release 25 -proc:none -cp "$CP" -d target/classes @target/main-sources.txt
cp src/main/resources/* target/classes/
find src/test/java -name '*.java' | sort > target/test-sources.txt
"$JAVA_HOME/bin/javac" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx256M -encoding UTF-8 --release 25 -proc:none -cp "target/classes:$CP" -d target/test-classes @target/test-sources.txt
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.buildmode.CoreTests
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.buildmode.RewardTests
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.buildmode.RecoverySafetyTests
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.buildmode.core.PlayerDataProofTests
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.buildmode.PilotGateTests
"$JAVA_HOME/bin/jar" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx128M --create --file target/ExcelBuildMode-1.3.jar --date=2026-10-14T03:00:00Z -C target/classes .
sha256sum target/ExcelBuildMode-1.3.jar
