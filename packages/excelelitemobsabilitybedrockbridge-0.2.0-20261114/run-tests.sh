#!/bin/bash
# Compile and run the ExcelEliteMobsAbilityBedrockBridge suites.
#  Run A: plain JVM, EliteMobs jar ABSENT from the runtime classpath
#         (pure logic + the fail-soft probe contract).
#  Run B: same TestMain plus EmMappingTestMain, with the pinned
#         EliteMobs jar present (enum mapping pins + probe honesty).
# Compile always uses the full house classpath incl. the EM jar -
# compile-time-only, exactly like build.sh.
set -euo pipefail
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
LIBS=/home/hatch/workspace/excelshops/build-libs
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
EM=${EM_JAR:-$PWD/../EliteMobs-10.9.14.jar}

EM_MD5=$(md5sum "$EM" | awk '{print $1}')
if [ "$EM_MD5" != "9e5f16298ac558e30197a1529b1ef83c" ]; then
  echo "REFUSING TO TEST: EliteMobs jar MD5 $EM_MD5 != pinned 9e5f16298ac558e30197a1529b1ef83c" >&2
  exit 1
fi

CP="$LIBS/paper-api.jar"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP="$CP:$LIBS/$j.jar"; done
CP="$CP:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"

rm -rf build/test-classes && mkdir -p build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$CP:$EM" -d build/test-classes \
  $(find src -name '*.java') \
  $(find tests -name '*.java')

echo "== Run A: no EliteMobs jar at runtime =="
"$JDK/bin/java" -DprobeExpect=UNAVAILABLE -cp "build/test-classes:$CP" \
  com.winkreyes.exceleitemobsabilitybedrockbridge.TestMain
"$JDK/bin/java" -cp "build/test-classes:$CP" \
  com.winkreyes.exceleitemobsabilitybedrockbridge.StatusCommandTestMain

echo "== Run B: pinned EliteMobs jar present =="
"$JDK/bin/java" -DprobeExpect=NOT_READY -cp "build/test-classes:$CP:$EM" \
  com.winkreyes.exceleitemobsabilitybedrockbridge.TestMain
"$JDK/bin/java" -cp "build/test-classes:$CP:$EM" \
  com.winkreyes.exceleitemobsabilitybedrockbridge.EmMappingTestMain
"$JDK/bin/java" -cp "build/test-classes:$CP:$EM" \
  com.winkreyes.exceleitemobsabilitybedrockbridge.StatusCommandTestMain
