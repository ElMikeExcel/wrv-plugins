#!/bin/bash
# Build ExcelDeathProtection-0.2.1.jar (deterministic: two builds byte-identical).
# House pattern (ExcelTour/ExcelTrainingDummy): manual javac, JDK 25 tree,
# --release 21, house build-libs Paper API (1.21.4) + VaultAPI 1.7.
# The 0.1.3 tree's original Maven build is preserved as build-maven.sh;
# this recipe exists because the offline VM has no cached Paper/Vault
# Maven artifacts, and it runs the full JUnit suite before packing.
# Working directory matters: ResourceConfigurationTest and
# PremiumCreditsTest read plugin.yml/config.yml relative to CWD,
# so the suite is run from this tree's root (surefire's convention).
set -euo pipefail
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
LIBS=/home/hatch/workspace/excelshops/build-libs
M2=/home/hatch/workspace/.m2/repository
CP="$LIBS/paper-api.jar:$LIBS/VaultAPI-1.7.jar:$HOME/workspace/excelitems-build-20261118/ExcelItems-0.1.0.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
CP="$CP:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"

python3 -c "import yaml,sys; [yaml.safe_load(open(f)) for f in ['plugin.yml','config.yml']]" \
  || { echo 'YAML parse failure — refusing to build'; exit 1; }

rm -rf build/classes build/test-classes && mkdir -p build/classes build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -encoding UTF-8 -cp "$CP" -d build/classes $(find src -name '*.java' | sort)

# Tests: JUnit 5 console standalone + Mockito (agent per pom.xml argLine).
# Test-runtime note: pom.xml pins Mockito 5.15.2 / ByteBuddy 1.15.11, whose
# inline mock-maker cannot modify classes on this VM's JDK 25 (and no JDK 21
# exists here), so the suite runs under Mockito 5.21.0 + ByteBuddy 1.18.3
# from the local Maven cache. Test sources are unmodified by this choice
# (except the ResourceConfigurationTest version pin). The launcher runs
# flat on -cp (not -jar) so Mockito's mock-maker sees ByteBuddy.
MOCK="$M2/org/mockito/mockito-core/5.21.0/mockito-core-5.21.0.jar"
TESTCP="build/classes:build/test-classes:$CP:$LIBS/junit-platform-console-standalone-1.11.4.jar:$MOCK:$LIBS/byte-buddy-1.18.3.jar:$LIBS/byte-buddy-agent-1.18.3.jar:$M2/org/objenesis/objenesis/3.3/objenesis-3.3.jar"
"$JDK/bin/javac" --release 21 -proc:none -nowarn -encoding UTF-8 -cp "$TESTCP" -d build/test-classes $(find test -name '*.java' | sort)
"$JDK/bin/java" -javaagent:"$MOCK" -cp "$TESTCP" org.junit.platform.console.ConsoleLauncher \
  execute --scan-class-path build/test-classes --details=tree --disable-banner \
  | tee ../TEST-RESULTS.txt
grep -q "tests failed" ../TEST-RESULTS.txt && grep -q "\[         0 tests failed" ../TEST-RESULTS.txt \
  || { echo 'TEST GATE FAIL: see TEST-RESULTS.txt'; exit 1; }

cp plugin.yml config.yml build/classes/
printf '%s\n' 'EXC-DPROTECT-0.2.1-WRV-20261118-01-a91c4e' > build/classes/excel-build.txt
# Pin every staged file's mtime so the double-build is byte-identical.
find build/classes -exec touch -d "2026-11-18 00:00:00 UTC" {} +
OUT=../ExcelDeathProtection-0.2.1.jar
OUT2=../ExcelDeathProtection-0.2.1-repro.jar
rm -f "$OUT" "$OUT2"
"$JDK/bin/jar" --create --file "$OUT" --no-manifest --date=2026-11-18T00:00:00Z -C build/classes .
"$JDK/bin/jar" --create --file "$OUT2" --no-manifest --date=2026-11-18T00:00:00Z -C build/classes .
cmp "$OUT" "$OUT2" && echo 'double-build: byte-identical'
rm -f "$OUT2"
unzip -p "$OUT" excel-build.txt | grep -q '^EXC-DPROTECT-0.2.1-WRV-20261118-01-a91c4e$' \
  || { echo 'FINGERPRINT GATE FAIL: excel-build.txt missing/incorrect in jar'; exit 1; }
echo "fingerprint gate: PASS (excel-build.txt EXC-DPROTECT-0.2.1-WRV-20261118-01-a91c4e)"
cd ..
sha256sum ExcelDeathProtection-0.2.1.jar
ls -l ExcelDeathProtection-0.2.1.jar
