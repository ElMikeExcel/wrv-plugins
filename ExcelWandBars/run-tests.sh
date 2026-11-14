#!/usr/bin/env bash
# Run all plain-main suites against a compiled classes dir.
# Usage: run-tests.sh <CLASSES_DIR> <LOG_FILE>
set -uo pipefail
NEW=$HOME/workspace/wandbars-fsync-fix-20261114
JAVA_HOME=$HOME/workspace/.jdk25/jdk-25.0.5+8
CLASSES=$1; LOG=$2
LIBS=$HOME/workspace/excelshops/build-libs
MOCKITO=$LIBS/mockito-core-5.15.2.jar
BB=$LIBS/byte-buddy-1.18.3.jar
BBA=$LIBS/byte-buddy-agent-1.18.3.jar
OBJ=$HOME/workspace/.m2/repository/org/objenesis/objenesis/3.3/objenesis-3.3.jar
CP_MAIN=$(cat "$NEW/work/cp.txt")
TC=$NEW/work/test-classes-run
rm -rf "$TC"; mkdir -p "$TC"
cd "$NEW/tree-0.3.18"
find tests -name '*.java' | sort > "$NEW/work/test-sources.txt"
$JAVA_HOME/bin/javac -proc:none -cp "$CLASSES:$CP_MAIN:$MOCKITO:$BB:$BBA:$OBJ" \
  -d "$TC" @"$NEW/work/test-sources.txt" > "$LOG" 2>&1 || { echo "TEST COMPILE FAILED"; cat "$LOG"; exit 1; }
echo "TEST COMPILE OK" >> "$LOG"
TOTAL=0; FAILED=0; SUITES=0; FAILED_SUITES=""
for f in tests/*.java; do
  name=$(basename "$f" .java)
  out=$($JAVA_HOME/bin/java -Dnet.bytebuddy.experimental=true -javaagent:"$MOCKITO" \
    -cp "$TC:$CLASSES:$CP_MAIN:$MOCKITO:$BB:$BBA:$OBJ" \
    "com.winkreyes.excelwandbars.$name" 2>&1)
  rc=$?
  SUITES=$((SUITES+1))
  echo "--- $name rc=$rc: $out" >> "$LOG"
  if [ $rc -ne 0 ]; then FAILED=$((FAILED+1)); FAILED_SUITES="$FAILED_SUITES $name"; else
    # count checks from "PASS <n> ..." line when present
    cnt=$(echo "$out" | sed -n 's/.*PASS \([0-9][0-9]*\).*/\1/p' | head -1)
    [ -n "$cnt" ] && TOTAL=$((TOTAL+cnt))
  fi
done
echo "SUITES=$SUITES CHECKS=$TOTAL FAILED_SUITES=$FAILED$FAILED_SUITES" | tee -a "$LOG"
