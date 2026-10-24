#!/bin/bash
# Restart persistence + rollback run on mm-rig (TEST RIG ONLY). Usage: persist.sh <tag>  (env MMP = fresh player name)
set -u; T=/workspace/makersmark/tests; R=/workspace/mm-rig; TAG=$1; LOG=$T/$TAG-persist.log; HOLD=$R/rollback-hold
export OUT=$LOG
node $T/t4a.js >/dev/null 2>&1
echo "--- restart 1 (persistence) $(date)" >> $LOG; $R/ctl.sh stop $TAG-p1; $R/ctl.sh start
node $T/t4b.js >/dev/null 2>&1
echo "--- rollback: jar moved to rollback-hold, restart $(date)" >> $LOG; mkdir -p $HOLD; $R/ctl.sh stop $TAG-p2
mv $R/plugins/ExcelMakersMark-0.1.0.jar $HOLD/; $R/ctl.sh start
ROLL=out node $T/t4c.js >/dev/null 2>&1
echo "--- reinstall: jar back, restart $(date)" >> $LOG; $R/ctl.sh stop $TAG-p3
mv $HOLD/ExcelMakersMark-0.1.0.jar $R/plugins/; $R/ctl.sh start
ROLL=back node $T/t4c.js >/dev/null 2>&1
sha256sum $R/plugins/ExcelMakersMark-0.1.0.jar >> $LOG
grep -E "RESULT|FAIL|ERROR" $LOG
