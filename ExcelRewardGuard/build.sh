#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
RUNTIME="${RUNTIME:-/home/hatch/workspace/excelbuildmode-pilot/runtime-server}"
JAVA_HOME="${JAVA_HOME:-/home/hatch/workspace/.jdk25}"
LIBRARIES="$RUNTIME/libraries"
[[ -f "$LIBRARIES/io/papermc/paper/paper-api/26.2.build.123-stable/paper-api-26.2.build.123-stable.jar" ]] || { echo 'Exact Paper API missing'; exit 1; }
# The exact jars the live server runs; the integration code compiles against
# them and LiveApiSignatureTests verifies their API surface at build time.
JOBS_JAR="${JOBS_JAR:-/home/hatch/workspace/economy-jars/Jobs.jar}"
MCMMO_JAR="${MCMMO_JAR:-/home/hatch/workspace/mcmmo-edit/repo/target/mcMMO.jar}"
ELITE_JAR="${ELITE_JAR:-/home/hatch/workspace/minecraft-server/plugins/EliteMobs-10.9.8.jar}"
for jar in "$JOBS_JAR" "$MCMMO_JAR" "$ELITE_JAR"; do [[ -f "$jar" ]] || { echo "Provider jar missing: $jar"; exit 1; }; done
CP=$(find "$LIBRARIES" -name '*.jar' -type f | sort | paste -sd: -)
PROVIDERS="$JOBS_JAR:$MCMMO_JAR:$ELITE_JAR"
rm -rf target/classes target/test-classes
mkdir -p target/classes target/test-classes
find src/main/java -name '*.java' | sort > target/main-sources.txt
"$JAVA_HOME/bin/javac" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx256M -encoding UTF-8 --release 25 -proc:none -cp "$CP:$PROVIDERS" -d target/classes @target/main-sources.txt
cp src/main/resources/* target/classes/
find src/test/java -name '*.java' | sort > target/test-sources.txt
"$JAVA_HOME/bin/javac" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx256M -encoding UTF-8 --release 25 -proc:none -cp "target/classes:$CP:$PROVIDERS" -d target/test-classes @target/test-sources.txt
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.rewardguard.ProvenanceTests | tee target/test-results-provenance.txt
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.rewardguard.XpSuppressionTests | tee target/test-results-suppression.txt
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.rewardguard.RewardScaleTests | tee target/test-results-scale.txt
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP" com.winkreyes.rewardguard.FallDamageTrackerTests | tee target/test-results-fall.txt
"$JAVA_HOME/bin/java" -XX:-UsePerfData -XX:ActiveProcessorCount=2 -Xmx256M -ea -cp "target/classes:target/test-classes:$CP:$PROVIDERS" com.winkreyes.rewardguard.LiveApiSignatureTests | tee target/test-results-signatures.txt
"$JAVA_HOME/bin/jar" -J-XX:-UsePerfData -J-XX:ActiveProcessorCount=2 -J-Xmx128M --create --file target/ExcelRewardGuard-1.2.jar --date=2026-10-16T06:30:00Z -C target/classes .
sha256sum target/ExcelRewardGuard-1.2.jar
