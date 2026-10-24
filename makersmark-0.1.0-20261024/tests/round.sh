#!/bin/bash
# One full test round on mm-rig with brand-new player names. Usage: round.sh <tag> <java-player> <helper> <bedrock-name>
T=/workspace/makersmark/tests; cd $T; TAG=$1; export MMP=$2 MMQ=$3
for t in t t2 t3 t6mend; do OUT=$T/$TAG-$t.log timeout 600 node $t.js >/dev/null 2>&1; done
OUT=$T/$TAG-bed.log MMB=$4 timeout 260 node t5bed.js >/dev/null 2>&1
./persist.sh $TAG >/dev/null 2>&1
for f in $TAG-t $TAG-t2 $TAG-t3 $TAG-t6mend $TAG-bed $TAG-persist; do echo "$f: $(grep RESULT $f.log | tr '\n' ' ')"; done
grep -h -E "FAIL|ERROR" $TAG-*.log
