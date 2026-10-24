#!/bin/bash
# RIG-ONLY full round for ExcelWandBars 0.2.2 (box test rig /workspace/ss022-rig, port 25941). Never touches the live server.
# usage: round.sh <SFX>   -> tests/run-<SFX>.log
set -u
SFX=$1; R=/workspace/ss022-rig; T=/workspace/batch-next/spellscreen-0.2.2/tests
export OUT=$T/run-$SFX.log SFX NODE_PATH=/workspace/ship1025-tests/node_modules
cd $T
ph(){ echo "--- phase $1 $(date +%T)" >> $OUT; PHASE=$1 timeout 700 node t3.js > /dev/null 2>&1 || echo "PHASE $1 exit $?" >> $OUT; }
pgrep -f "ss022-rig" >/dev/null; [ -f $R/boot.log ] && mv $R/boot.log $R/boot-prev-$SFX.log
$R/ctl.sh to021; $R/ctl.sh start >> $OUT; ph pre; $R/ctl.sh stop pre-$SFX
$R/ctl.sh to022; $R/ctl.sh start >> $OUT; ph main; $R/ctl.sh stop main-$SFX
$R/ctl.sh start >> $OUT; ph restart; ph reload
cp $R/plugins/Magic/spells.yml $R/bak-ss022/spells.yml.round-$SFX
python3 - <<'PY'
p='/workspace/ss022-rig/plugins/Magic/spells.yml';s=open(p).read()
a="boon|2:\n  upgrade_required_path: student\n  upgrade_required_casts: 50"
assert a in s; open(p,'w').write(s.replace(a,a[:-2]+"2",1))
PY
echo "magic load" > $R/rigconsole; sleep 12; ph rankup
cp $R/bak-ss022/Magic-spells.yml.bak $R/plugins/Magic/spells.yml; $R/ctl.sh stop rk-$SFX
cp /workspace/batch-next/tips/ExcelTips-0.1.0.jar $R/plugins/; $R/ctl.sh start >> $OUT; ph tips; $R/ctl.sh stop tips-$SFX; rm -f $R/plugins/ExcelTips-0.1.0.jar
$R/ctl.sh to021; $R/ctl.sh start >> $OUT; ph rollback; $R/ctl.sh stop rb-$SFX
cmp $R/plugins/Magic/spells.yml $R/bak-ss022/Magic-spells.yml.bak && echo "spells.yml restored" >> $OUT
echo "=== ROUND $SFX DONE $(date +%T)" >> $OUT
