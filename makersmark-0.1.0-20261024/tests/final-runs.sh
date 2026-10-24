#!/bin/bash
# Final verification on the final jar (7490006e): TIPS2, then R3 and R4 (round.sh incl. restart + rollback, then t7worlds).
cd /workspace/makersmark/tests; S=/workspace/makersmark/tests/FINAL-summary.log; : > $S
echo "jar: $(sha256sum /workspace/mm-rig/plugins/ExcelMakersMark-0.1.0.jar)" >> $S
OUT=$PWD/TIPS2.log MMP=MkTipC1 MMQ=MkTipD1 timeout 500 node t9tips.js >/dev/null 2>&1; echo "TIPS2: $(grep RESULT TIPS2.log)" >> $S; grep FAIL TIPS2.log >> $S
for R in R3 R4; do
  ./round.sh $R Mk${R}P Mk${R}Q Mk${R}B >> $S 2>&1
  OUT=$PWD/$R-t7worlds.log MMP=Mk${R}WP MMQ=Mk${R}WQ timeout 1500 node t7worlds.js >/dev/null 2>&1; echo "$R-t7worlds: $(grep RESULT $R-t7worlds.log)" >> $S; grep FAIL $R-t7worlds.log >> $S
done
echo "jar after: $(sha256sum /workspace/mm-rig/plugins/ExcelMakersMark-0.1.0.jar)" >> $S; echo ALLDONE >> $S
