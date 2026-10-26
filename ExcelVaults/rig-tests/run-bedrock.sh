#!/bin/bash
# Retry the Bedrock test up to 6 times (bedrock-protocol checksum flakiness on join); keep failed tries.
cd "$(dirname "$0")"
export RIG_RCON_PASSWORD=$(grep '^rcon.password=' /workspace/excelvaults-rig/server.properties | cut -d= -f2-)
for t in 1 2 3 4 5 6; do
  timeout 300 node bedrock-test.js >/dev/null 2>&1
  if ! grep -q "could not join" bedrock-test.log; then echo "try $t"; grep RESULT bedrock-test.log; exit 0; fi
  cp bedrock-test.log attempts/bedrock-test.try$t.log; sleep 65
done; echo gave up; exit 1
