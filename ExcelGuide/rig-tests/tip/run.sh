#!/usr/bin/env bash
# Orchestrates the 0.3.0 tip/persistence/rollback rig run on the box guide-rig (port 25993).
set -u
R=/workspace/guide-rig; T=/workspace/guide-rig-tests/tip; J=/workspace/batch-next/guide-learning
export OUT=${OUT:-$T/run.log}; : > $OUT
start(){ rm -f $R/boot.log; (cd $R && setsid nohup ./start.sh >/dev/null 2>&1 &); for i in $(seq 1 120); do grep -q 'Done (' $R/boot.log 2>/dev/null && return 0; sleep 2; done; echo "boot timeout" >> $OUT; return 1; }
stop(){ python3 $R/rcon.py stop >/dev/null 2>&1; sleep 3; }
waitdown(){ for i in $(seq 1 60); do ss -ltn | grep -q ':25993 ' || return 0; sleep 2; done; }
jar(){ rm -f $R/plugins/ExcelGuide-*.jar; cp "$1" $R/plugins/; echo "== jar: $(basename $1) $(sha256sum $1|cut -c1-16)" >> $OUT; }
rc=0
jar $J/build/ExcelGuide-0.3.0.jar; start || exit 1
echo "== PHASE A (0.3.0, live-like [] / {} files, 0.2.3 config)" >> $OUT; (cd $T && node phaseA.js) || rc=1
stop; waitdown; start || exit 1
echo "== PHASE B (restart, persistence)" >> $OUT; (cd $T && node phaseB.js) || rc=1
stop; waitdown
# snapshot AFTER 0.3.0 has shut down (it flushes the review store on disable)
cp $R/plugins/ExcelGuide/peer-review.yml /tmp/pr.before; cp $R/plugins/ExcelGuide/peer-tip.yml /tmp/pt.before
jar $J/rig-jars/ExcelGuide-0.2.3-scrubbed.jar; start || exit 1
echo "== PHASE C (ROLLBACK to 0.2.3)" >> $OUT; (cd $T && node phaseC.js) || rc=1
stop; waitdown
cmp -s /tmp/pr.before $R/plugins/ExcelGuide/peer-review.yml && echo "PASS 0.2.3 left peer-review.yml untouched" >> $OUT || { echo "FAIL peer-review.yml changed under 0.2.3" >> $OUT; rc=1; }
cmp -s /tmp/pt.before $R/plugins/ExcelGuide/peer-tip.yml && echo "PASS 0.2.3 left peer-tip.yml untouched" >> $OUT || { echo "FAIL peer-tip.yml changed under 0.2.3" >> $OUT; rc=1; }
jar $J/build/ExcelGuide-0.3.0.jar; start || exit 1
echo "== PHASE D (forward again to 0.3.0)" >> $OUT; (cd $T && node phaseD.js) || rc=1
stop; waitdown
echo "== OVERALL rc=$rc" >> $OUT
exit $rc
