#!/usr/bin/env bash
# Build + test ExcelTogether 0.2.0. Needs JDK 25 (Paper API 26.2 is class v69).
set -euo pipefail
D="$(cd "$(dirname "$0")" && pwd)"; W=/workspace/wrv019; OUT=/workspace/wrv063
JDK=$W/jdk25/bin
API=/workspace/wrv015/scratch/libraries/io/papermc/paper/paper-api/26.2.build.133-stable/paper-api-26.2.build.133-stable.jar
CP="$API"
for j in $(find /workspace/wrv015/scratch/libraries/net/kyori /workspace/wrv015/scratch/libraries/org/jspecify /workspace/wrv015/scratch/libraries/com/google /workspace/wrv015/scratch/libraries/net/md-5 /workspace/wrv015/scratch/libraries/org/yaml /workspace/wrv015/scratch/libraries/org/slf4j /workspace/wrv015/scratch/libraries/org/joml /workspace/wrv015/scratch/libraries/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
JOBS="$W/inputs/Jobs.jar:$W/inputs/CMILib.jar"
FG=/workspace/mc/test26/plugins/floodgate.jar   # test only: real Cumulus classes to check form builders
O=$D/build; rm -rf $O; mkdir -p $O/classes $O/test
$JDK/javac --release 21 -proc:none -nowarn -cp "$CP:$JOBS" -d $O/classes $(find $D/src -name '*.java')
cp $D/plugin.yml $D/config.yml $O/classes/
$JDK/javac --release 21 -proc:none -nowarn -cp "$O/classes:$CP:$JOBS:$FG" -d $O/test $(find $D/tests -name '*.java')
set +e
( $JDK/java -cp "$O/test:$O/classes:$CP:$JOBS:$FG" com.winkreyes.exceltogether.PartyTest && $JDK/java -Dsrc=$D/src -Dcfg=$D/config.yml -Dplugin=$D/plugin.yml -cp "$O/test:$O/classes:$CP:$JOBS:$FG" com.winkreyes.exceltogether.PartyFeatureTest && $JDK/java -Dmcmmojar=/workspace/mmo-test/upstream/target/mcMMO.jar -cp "$O/test:$O/classes:$CP:$JOBS:$FG" com.winkreyes.exceltogether.McmmoLinkTest ) | tee $O/test.log
RC=${PIPESTATUS[0]}
set -e
[ $RC -eq 0 ] || { echo TESTS FAILED; exit 1; }
python3 - <<P
import zipfile,os
out="$OUT/ExcelTogether-0.2.0.jar"; root="$O/classes"
fs=sorted(os.path.join(dp,f) for dp,_,fn in os.walk(root) for f in fn)
with zipfile.ZipFile(out,"w",zipfile.ZIP_DEFLATED) as z:
    for f in fs:
        zi=zipfile.ZipInfo(os.path.relpath(f,root),(2026,10,16,0,0,0)); zi.compress_type=zipfile.ZIP_DEFLATED
        z.writestr(zi,open(f,"rb").read())
print("built",out)
P
