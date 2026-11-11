#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
RUNTIME="${RUNTIME:-/home/hatch/workspace/excelbuildmode-pilot/runtime-server}"
JAVA_HOME="${JAVA_HOME:-/home/hatch/workspace/.jdk25}"
LIBRARIES="$RUNTIME/libraries"
[[ -f "$LIBRARIES/io/papermc/paper/paper-api/26.2.build.123-stable/paper-api-26.2.build.123-stable.jar" ]] || { echo 'Exact Paper API missing'; exit 1; }
# 1.3.2: WorldEditLimitBridge compiles against the exact WorldEdit build the
# server runs (7.4.5, staged with the install packets). Compile/test only —
# the jar is never shaded; WorldEdit stays a softdepend at runtime.
WEJAR="${WORLDEDIT_JAR:-/home/hatch/workspace/excelbuildmode-pilot/install-staging/worldedit-bukkit-7.4.5.jar}"
[[ -f "$WEJAR" ]] || { echo 'Exact WorldEdit jar missing'; exit 1; }
CP=$(find "$LIBRARIES" -name '*.jar' -type f | sort | paste -sd: -):$WEJAR
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
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.buildmode.BuildReturnTests
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.buildmode.AdminTests
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.buildmode.ReviewedTests
"$JAVA_HOME/bin/jar" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx128M --create --file target/ExcelBuildMode-1.3.4.jar --date=2026-11-10T18:00:00Z -C target/classes .
sha256sum target/ExcelBuildMode-1.3.4.jar
