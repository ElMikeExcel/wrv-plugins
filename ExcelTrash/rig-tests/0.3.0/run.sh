#!/bin/bash
# 0.2.0 -> 0.3.0 on the TEST rig only (/workspace/trash-rig, port 25951). Never the live server.
R=/workspace/trash-rig; T=/workspace/trash030/tests; export OUT=$T/run$1.log; : > $OUT
cd $R; if [ -f boot.log ] && ! grep -q EXITED boot.log; then ./ctl.sh stop pre030; fi
for f in plugins/ExcelTrash-0.[12]*.jar; do [ -f "$f" ] || continue; mv "$f" "$f.off-030"; done
mv plugins/RigTrashProbe.jar plugins/RigTrashProbe.jar.off-030 2>/dev/null; cp /workspace/trash030/probe/RigTrashProbe.jar plugins/; rm -f plugins/ExcelTrash/hinted.txt; [ -f plugins/ExcelTrash-0.3.0.jar ] || cp /workspace/trash030/ExcelTrash-0.3.0.jar plugins/; cmp /workspace/trash030/ExcelTrash-0.3.0.jar plugins/ExcelTrash-0.3.0.jar || cp /workspace/trash030/ExcelTrash-0.3.0.jar plugins/; ./ctl.sh start
grep -a -E "ExcelTrash|Done \(" boot.log | sed 's/\x1b\[[0-9;]*m//g' | grep -v " - " >> $OUT
grep -a -c -E "SEVERE|ERROR" boot.log | sed 's/^/boot SEVERE+ERROR lines: /' >> $OUT
cd $T && node t030.js > /dev/null 2>&1; echo "node exit $?" >> $OUT
cd $R; ./ctl.sh stop 030-$1; grep -a -E "ExcelTrash" boot-030-$1.log | sed 's/\x1b\[[0-9;]*m//g' | grep -i -E "exception|error" >> $OUT; echo DONE >> $OUT
