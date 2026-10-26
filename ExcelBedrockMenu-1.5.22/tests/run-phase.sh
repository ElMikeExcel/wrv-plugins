#!/bin/bash
# Retry one phase up to 6 times (test client checksum flakiness); keep failed tries.
cd "$(dirname "$0")"; PH=$1
for t in 1 2 3 4 5 6; do
  PHASE=$PH timeout 150 node bed122.js >/dev/null 2>&1
  if grep -q "^.\{9\}RESULT" bed122-$PH.out; then echo "phase $PH try $t"; exit 0; fi
  cp bed122-$PH.out bed122-attempts/bed122-$PH.try$t.out; sleep 65
done; echo "phase $PH gave up"; exit 1
