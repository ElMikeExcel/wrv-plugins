#!/usr/bin/env bash
# ExcelShops 1.3.1 build + test (JDK 25). Usage: build.sh [src_dir] [out_dir]
set -euo pipefail
export LANG=C.UTF-8 LC_ALL=C.UTF-8
J=/workspace/wrv019/jdk25/bin
SRC=${1:-/workspace/excelshops-src-pr25/ExcelShops}; OUT=${2:-/workspace/excelshops131/out}
L=/workspace/excelshops13/libs; M=$HOME/.m2/repository
CP="$L/paper-api.jar:$L/guard.jar:/workspace/wrv019/scratch/plugins/Vault.jar"
for j in $(find /workspace/wrv019/scratch/libraries -name '*.jar' ! -name 'paper-api*' ! -path '*/io/papermc/paper/*'); do CP="$CP:$j"; done
rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/test-classes"
find "$SRC/src" -name '*.java' > "$OUT/s.txt"
$J/javac --release 21 -proc:none -cp "$CP" -d "$OUT/classes" @"$OUT/s.txt"
cp "$SRC/plugin.yml" "$SRC/config.yml" "$SRC/spawn-catalog.yml" "$SRC/LICENSE" "$OUT/classes/"; cp -r "$SRC/mobile-menus" "$OUT/classes/"
echo MAIN_COMPILE_OK
TCP="$OUT/classes:$CP:$L/junit-platform-console-standalone-1.11.4.jar:$M/org/mockito/mockito-core/5.21.0/mockito-core-5.21.0.jar:$M/net/bytebuddy/byte-buddy/1.17.7/byte-buddy-1.17.7.jar:$M/net/bytebuddy/byte-buddy-agent/1.17.7/byte-buddy-agent-1.17.7.jar:$M/org/objenesis/objenesis/3.3/objenesis-3.3.jar:$M/org/apache/logging/log4j/log4j-core/2.25.4/log4j-core-2.25.4.jar"
find "$SRC/tests" -name '*.java' > "$OUT/t.txt"
$J/javac --release 21 -proc:none -cp "$TCP" -d "$OUT/test-classes" @"$OUT/t.txt"
echo TEST_COMPILE_OK
cd "$SRC"
set +e
$J/java -Dnet.bytebuddy.experimental=true -javaagent:$M/org/mockito/mockito-core/5.21.0/mockito-core-5.21.0.jar -cp "$OUT/test-classes:$TCP" org.junit.platform.console.ConsoleLauncher execute \
 --scan-classpath "$OUT/test-classes" --details summary --fail-if-no-tests > "$OUT/junit.txt" 2>&1
echo JUNIT_EXIT=$?; tail -25 "$OUT/junit.txt"
