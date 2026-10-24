#!/bin/bash
# Reruns on final jar 7490006e after rig-only test fixes (anvil reset in t.js, 1000 HP bots + elite-husk retry in t7worlds, Bedrock spawn reset). Fresh names.
cd /workspace/makersmark/tests; S=/workspace/makersmark/tests/FINAL2-summary.log; : > $S
echo "jar: $(sha256sum /workspace/mm-rig/plugins/ExcelMakersMark-0.1.0.jar)" >> $S
OUT=$PWD/TIPS3.log MMP=MkTipE1 MMQ=MkTipF1 timeout 500 node t9tips.js >/dev/null 2>&1; echo "TIPS3: $(grep RESULT TIPS3.log)" >> $S; grep -E "FAIL|ERROR" TIPS3.log >> $S
for R in R3b R4b; do
  OUT=$PWD/$R-t.log MMP=Mk${R}P MMQ=Mk${R}Q timeout 600 node t.js >/dev/null 2>&1; echo "$R-t: $(grep RESULT $R-t.log)" >> $S; grep -E "FAIL|ERROR" $R-t.log >> $S
  OUT=$PWD/$R-t7worlds.log MMP=Mk${R}WP MMQ=Mk${R}WQ timeout 1500 node t7worlds.js >/dev/null 2>&1; echo "$R-t7worlds: $(grep RESULT $R-t7worlds.log)" >> $S; grep -E "FAIL|ERROR" $R-t7worlds.log >> $S
done
OUT=$PWD/R3b-bed.log MMB=MkR3bB timeout 260 node t5bed.js >/dev/null 2>&1; echo "R3b-bed: $(grep RESULT R3b-bed.log)" >> $S; grep FAIL R3b-bed.log >> $S
echo "jar after: $(sha256sum /workspace/mm-rig/plugins/ExcelMakersMark-0.1.0.jar)" >> $S; echo ALLDONE >> $S
