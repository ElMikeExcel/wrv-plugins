#!/bin/bash
# Retry one phase up to 5 times (bedrock-protocol test client checksum flakiness); failed tries kept in attempts/.
cd "$(dirname "$0")"; PH=$1
for t in 1 2 3 4 5; do
  PHASE=$PH timeout 200 node parity-bed.js >/dev/null 2>&1
  if grep -q "RESULT" parity-bed-$PH.out; then echo "phase $PH try $t"; exit 0; fi
  cp parity-bed-$PH.out attempts/parity-bed-$PH.try$t.out; sleep 65
done; echo "phase $PH gave up"; exit 1
