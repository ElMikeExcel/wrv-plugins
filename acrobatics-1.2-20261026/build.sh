#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
RUNTIME="${RUNTIME:-/workspace/menu-fix/scratch}"
JAVA_HOME="${JAVA_HOME:-/workspace/wrv019/jdk25}"
MMO_JAR="${MMO_JAR:-/workspace/mmo-test/mcMMO-together-bonus.jar}"
LIBRARIES="$RUNTIME/libraries"
CP=$(find "$LIBRARIES" -name '*.jar' -type f ! -path '*/paper-api/26.2.*/*' | sort | paste -sd: -)
[[ -f "$MMO_JAR" ]] && CP="$CP:$MMO_JAR"
rm -rf target/classes target/test-classes
mkdir -p target/classes target/test-classes
find src/main/java -name '*.java' | sort > target/main-sources.txt
"$JAVA_HOME/bin/javac" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx256M -encoding UTF-8 --release 25 -proc:none -cp "$CP" -d target/classes @target/main-sources.txt
cp src/main/resources/* target/classes/
find src/test/java -name '*.java' | sort > target/test-sources.txt
"$JAVA_HOME/bin/javac" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx256M -encoding UTF-8 --release 25 -proc:none -cp "target/classes:$CP" -d target/test-classes @target/test-sources.txt
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.acrobatics.PayoutTests
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.acrobatics.DeathTests
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.acrobatics.FallTrackerTests
"$JAVA_HOME/bin/jar" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx128M --create --file target/ExcelAcrobatics-1.2-candidate.jar --date=2026-10-27T01:42:00Z -C target/classes .
sha256sum target/ExcelAcrobatics-1.2-candidate.jar

