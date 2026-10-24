#!/bin/bash
# runs t8bedworlds one world per Bedrock session (fresh name per world), retrying a session up to 6 times if the jsp-raknet client desyncs (client-side test artifact)
cd /workspace/makersmark/tests; B=$1; OUT=$2; : > $OUT; WS=${WORLDS:-overworld nether end em_id_test_dungeon em_adventurers_guild}; i=0
for w in $WS; do i=$((i+1))
  for try in 1 2 3 4 5 6; do ONLY=$w MMB=$B$i$try timeout 300 node t8bedworlds.js > /tmp/t8-$w.out 2>&1; rc=$?
    grep -q "aborted=1\|TIMEOUT\|Connect timed out\|already logged in" /tmp/t8-$w.out && ! grep -q "^RESULT pass=[0-9]* fail=[0-9]*$" /tmp/t8-$w.out || break; echo "   (retry $w after client desync, try $try)" >> $OUT; sleep 8; done
  cat /tmp/t8-$w.out >> $OUT; sleep 4; done
echo "TOTAL pass=$(grep -c '  PASS' $OUT) fail=$(grep -c '  FAIL' $OUT)" >> $OUT
