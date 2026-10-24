#!/bin/bash
# Full run on the TEST rig (/workspace/trash-rig, port 25951) only.
N=$1; T=/workspace/trash020/tests; R=/workspace/trash-rig; export OUT=$T/run$N.log; : > $OUT
$T/upgrade.sh >> $OUT 2>&1
sleep 3; cd $T && RUN=1 node main.js > /dev/null 2>&1
sleep 3; node stopA.js > /dev/null 2>&1
for i in $(seq 1 90); do grep -q EXITED $R/boot.log && break; sleep 1; done
grep -a -E "ExcelTrash|Saving players" $R/boot.log | sed 's/\x1b\[[0-9;]*m//g' | tail -4 >> $OUT
mv $R/boot.log $R/boot-stop$N.log; cd $R && ./ctl.sh start; cd $T
sleep 3; node stopB.js > /dev/null 2>&1

# rollback check
sleep 3; node rollA.js > /dev/null 2>&1
cd $R && ./ctl.sh stop roll$N && rm plugins/ExcelTrash-0.2.0.jar && cp /workspace/batch1023/upload/ExcelTrash-0.1.0.jar plugins/ && ./ctl.sh start
grep -a -i -E "ExcelTrash" boot.log | sed 's/\x1b\[[0-9;]*m//g' | grep -v " - " >> $OUT; grep -a -c -i "exception" boot.log | sed 's/^/boot exceptions total: /' >> $OUT
cd $T; sleep 3; node rollB.js > /dev/null 2>&1
echo "ROLLDONE" >> $OUT
