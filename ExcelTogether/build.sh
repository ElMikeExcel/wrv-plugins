#!/usr/bin/env bash
# Build + test ExcelTogether 0.4.0 on the box. Needs JDK 25 (Paper API 26.2 is class v69).
# Paths default to the shared box (Steve's box); override with env vars elsewhere.
set -euo pipefail
D="$(cd "$(dirname "$0")" && pwd)"; OUT="$D"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}/bin
L=${LIBS:-/workspace/build-libs-wrv/paper26.2}
CP="$(ls $L/*.jar | tr '\n' ':')"
JOBS=${JOBS:-/workspace/wrv063/scratch/plugins/Jobs.jar:/workspace/wrv063/scratch/plugins/CMILib.jar}
FG=${FG:-/workspace/wrv063/scratch/plugins/floodgate.jar}
MMO_UP=${MMO_UP:-/workspace/acro/scratch/plugins/mcMMO.jar}
MMO_MINE=${MMO_MINE:-$MMO_UP}
O=$D/build; rm -rf $O; mkdir -p $O/classes $O/test
$JDK/javac --release 21 -proc:none -nowarn -cp "$CP:$JOBS" -d $O/classes $(find $D/src -name '*.java')
cp $D/plugin.yml $D/config.yml $O/classes/
$JDK/javac --release 21 -proc:none -nowarn -cp "$O/classes:$CP:$JOBS:$FG" -d $O/test $(find $D/tests -name '*.java')
R="$O/test:$O/classes:$CP:$JOBS:$FG"
set +e
( $JDK/java -cp "$R" com.winkreyes.exceltogether.PartyTest && $JDK/java -Dsrc=$D/src -Dcfg=$D/config.yml -Dplugin=$D/plugin.yml -Dfgjar=$FG -cp "$R" com.winkreyes.exceltogether.PartyFeatureTest && $JDK/java -cp "$R" com.winkreyes.exceltogether.RangeIndicatorTest && $JDK/java -Dmcmmojar=$MMO_UP -cp "$R" com.winkreyes.exceltogether.McmmoLinkTest && $JDK/java -cp "$R" com.winkreyes.exceltogether.McmmoSyncTest && $JDK/java -cp "$R" com.winkreyes.exceltogether.ShareTest && $JDK/java -Dmcmmojar=$MMO_MINE -cp "$R" com.winkreyes.exceltogether.XpShareTest && $JDK/java -cp "$R" com.winkreyes.exceltogether.PartyRulesTest ) 2>&1 | tee $O/test.log
RC=${PIPESTATUS[0]}
set -e
[ $RC -eq 0 ] || { echo TESTS FAILED; exit 1; }
python3 - <<P
import zipfile,os
out="$OUT/ExcelTogether-0.4.0.jar"; root="$O/classes"
fs=sorted(os.path.join(dp,f) for dp,_,fn in os.walk(root) for f in fn)
with zipfile.ZipFile(out,"w",zipfile.ZIP_DEFLATED) as z:
    for f in fs:
        zi=zipfile.ZipInfo(os.path.relpath(f,root),(2026,10,23,0,0,0)); zi.compress_type=zipfile.ZIP_DEFLATED
        z.writestr(zi,open(f,"rb").read())
print("built",out)
P
