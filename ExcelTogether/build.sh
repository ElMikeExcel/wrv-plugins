#!/usr/bin/env bash
# Build + test ExcelTogether 0.3.0. Needs JDK 25 (Paper API 26.2 is class v69).
# 0.3.0: toolchain paths retargeted to this machine (the original /workspace/wrv* tree is
# not present here); jar now lands next to this script instead of /workspace/wrv063.
set -euo pipefail
D="$(cd "$(dirname "$0")" && pwd)"; OUT="$D"
JDK=/usr/lib/jvm/java-25-openjdk-amd64/bin
L="$HOME/workspace/excelbuildmode-pilot/runtime-server/libraries"
API=$L/io/papermc/paper/paper-api/26.2.build.123-stable/paper-api-26.2.build.123-stable.jar
CP="$API"
for j in $(find $L/net/kyori $L/org/jspecify $L/com/google $L/net/md-5 $L/org/yaml $L/org/slf4j $L/org/joml $L/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
JOBS="$HOME/workspace/economy-jars/Jobs.jar:$HOME/workspace/economy-jars/CMILib.jar"
FG=$HOME/workspace/minecraft-server/plugins/floodgate.jar   # test only: real Cumulus classes to check form builders
# Test-only mcMMO jars on this machine: a stock 2.3.003 build for the link signature probe
# (mcmmo-edit/repo/target/original-mcMMO.jar fails the probe's class resolution, verified Oct 17)
# and the live server's custom WRV build for the XP share test.
MMO_UP=$HOME/workspace/excelrewardguard/work/test-server-1.2-providers/plugins/mcMMO.jar
MMO_MINE=$HOME/workspace/backups/pre-guild-20261015/root/plugins/mcMMO-2.3.003-WRV-XP-2.jar
O=$D/build; rm -rf $O; mkdir -p $O/classes $O/test
$JDK/javac --release 21 -proc:none -nowarn -cp "$CP:$JOBS" -d $O/classes $(find $D/src -name '*.java')
cp $D/plugin.yml $D/config.yml $O/classes/
$JDK/javac --release 21 -proc:none -nowarn -cp "$O/classes:$CP:$JOBS:$FG" -d $O/test $(find $D/tests -name '*.java')
set +e
( $JDK/java -cp "$O/test:$O/classes:$CP:$JOBS:$FG" com.winkreyes.exceltogether.PartyTest && $JDK/java -Dsrc=$D/src -Dcfg=$D/config.yml -Dplugin=$D/plugin.yml -Dfgjar=$FG -cp "$O/test:$O/classes:$CP:$JOBS:$FG" com.winkreyes.exceltogether.PartyFeatureTest && $JDK/java -cp "$O/test:$O/classes:$CP:$JOBS:$FG" com.winkreyes.exceltogether.RangeIndicatorTest && $JDK/java -Dmcmmojar=$MMO_UP -cp "$O/test:$O/classes:$CP:$JOBS:$FG" com.winkreyes.exceltogether.McmmoLinkTest && $JDK/java -cp "$O/test:$O/classes:$CP:$JOBS:$FG" com.winkreyes.exceltogether.McmmoSyncTest && $JDK/java -cp "$O/test:$O/classes:$CP:$JOBS:$FG" com.winkreyes.exceltogether.ShareTest && $JDK/java -Dmcmmojar=$MMO_MINE -cp "$O/test:$O/classes:$CP:$JOBS:$FG" com.winkreyes.exceltogether.XpShareTest ) | tee $O/test.log
RC=${PIPESTATUS[0]}
set -e
[ $RC -eq 0 ] || { echo TESTS FAILED; exit 1; }
python3 - <<P
import zipfile,os
out="$OUT/ExcelTogether-0.3.0.jar"; root="$O/classes"
fs=sorted(os.path.join(dp,f) for dp,_,fn in os.walk(root) for f in fn)
with zipfile.ZipFile(out,"w",zipfile.ZIP_DEFLATED) as z:
    for f in fs:
        zi=zipfile.ZipInfo(os.path.relpath(f,root),(2026,10,16,0,0,0)); zi.compress_type=zipfile.ZIP_DEFLATED
        z.writestr(zi,open(f,"rb").read())
print("built",out)
P
