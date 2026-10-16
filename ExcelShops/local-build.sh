#!/usr/bin/env bash
# Local verification build for ExcelShops (Argus): javac + JUnit console, exact project sources.
export LANG=C.UTF-8 LC_ALL=C.UTF-8
set -euo pipefail
ROOT=/home/hatch/workspace/excelshops
PROJ=$ROOT/project
LIBS=$ROOT/build-libs
RT=/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries
M2=/home/hatch/workspace/.m2/repository
export JAVA_HOME=/home/hatch/workspace/.jdk25
JAVAC=$JAVA_HOME/bin/javac; JAVA=$JAVA_HOME/bin/java; JAR=$JAVA_HOME/bin/jar
GUARD=$ROOT/ci/ChestShop-ConfirmationGuard/target/ChestShop-3.13-pre-1-build474-confirmation.1.jar
CP_MAIN="$LIBS/paper-api.jar:$LIBS/VaultAPI-1.7.jar:$GUARD"
for j in adventure-api-4.20.0 adventure-key-4.20.0 adventure-text-minimessage-4.20.0 adventure-text-serializer-gson-4.20.0 adventure-text-serializer-json-4.20.0 adventure-text-serializer-legacy-4.20.0 adventure-text-serializer-plain-4.20.0 adventure-text-logger-slf4j-4.20.0 examination-api-1.3.0 examination-string-1.3.0 option-1.0.0 gson-2.11.0 guava-33.3.1-jre snakeyaml-2.2; do CP_MAIN="$CP_MAIN:$LIBS/$j.jar"; done
CP_MAIN="$CP_MAIN:$RT/org/joml/joml/1.10.8/joml-1.10.8.jar:$RT/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:$RT/it/unimi/dsi/fastutil/8.5.19/fastutil-8.5.19.jar:$RT/com/mojang/brigadier/1.3.10/brigadier-1.3.10.jar:$RT/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar:$RT/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar:$RT/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar:$RT/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar:$RT/org/apache/logging/log4j/log4j-api/2.26.0/log4j-api-2.26.0.jar"
OUT=$ROOT/build-out; rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/test-classes"
find "$PROJ/src" -name "*.java" > "$OUT/sources.txt"
echo "main sources: $(wc -l < "$OUT/sources.txt")"
$JAVAC --release 21 -proc:none -cp "$CP_MAIN" -d "$OUT/classes" @"$OUT/sources.txt"
cp "$PROJ/plugin.yml" "$PROJ/config.yml" "$PROJ/spawn-catalog.yml" "$PROJ/LICENSE" "$OUT/classes/" 2>/dev/null; cp -r "$PROJ/mobile-menus" "$OUT/classes/"
echo "MAIN COMPILE OK"
MOCKITO=$LIBS/mockito-core-5.15.2.jar
BB=$LIBS/byte-buddy-1.18.3.jar
BBA=$LIBS/byte-buddy-agent-1.18.3.jar
OBJ=$M2/org/objenesis/objenesis/3.3/objenesis-3.3.jar
LOG4J=$M2/org/apache/logging/log4j/log4j-core/2.25.2/log4j-core-2.25.2.jar
CP_TEST="$OUT/classes:$CP_MAIN:$LIBS/junit-platform-console-standalone-1.11.4.jar:$MOCKITO:$BB:$BBA:$OBJ:$LOG4J"
find "$PROJ/tests" -name "*.java" > "$OUT/test-sources.txt"
echo "test sources: $(wc -l < "$OUT/test-sources.txt")"
$JAVAC --release 21 -proc:none -cp "$CP_TEST" -d "$OUT/test-classes" @"$OUT/test-sources.txt"
echo "TEST COMPILE OK"
cd "$PROJ"
$JAVA -Dnet.bytebuddy.experimental=true -javaagent:"$MOCKITO" -cp "$LIBS/junit-platform-console-standalone-1.11.4.jar:$OUT/test-classes:$CP_TEST" org.junit.platform.console.ConsoleLauncher execute \
  --select-class com.winkreyes.shops.ShopUiTest \
  --select-class com.winkreyes.shops.CompassShortcutTest \
  --select-class com.winkreyes.shops.BedrockFormsTest \
  --select-class com.winkreyes.shops.MobileCatalogTest \
  --select-class com.winkreyes.shops.MobileMenuBridgeTest \
  --select-class com.winkreyes.shops.LegacyDataMigrationTest \
  --details summary --fail-if-no-tests > /tmp/junit-out.txt 2>&1; tail -16 /tmp/junit-out.txt
