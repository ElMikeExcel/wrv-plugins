#!/bin/bash
# fresh 0.1.0 -> 0.2.0 upgrade on the TEST rig only
R=/workspace/trash-rig; T=/workspace/trash020/tests
cd $R; ./ctl.sh stop pre; rm -f plugins/ExcelTrash-*.jar; rm -rf plugins/ExcelTrash
cp /workspace/batch1023/upload/ExcelTrash-0.1.0.jar plugins/; ./ctl.sh start
cd $T && node phase0.js; cd $R
./ctl.sh stop p0; cp plugins/ExcelTrash/batches.dat $T/batches-0.1.0.dat
rm plugins/ExcelTrash-0.1.0.jar; cp /workspace/trash020/ExcelTrash-0.2.0.jar plugins/; ./ctl.sh start
grep -a "ExcelTrash" boot.log | sed 's/\x1b\[[0-9;]*m//g' | grep -v " - "; head -c 60 plugins/ExcelTrash/batches.dat; echo
