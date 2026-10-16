#!/usr/bin/env bash
# Build + test ExcelWaypoints 0.1.0. Needs JDK 25 (Paper API 26.2 is class v69). Output jar: /workspace/wrv017/ExcelWaypoints-0.1.0.jar
set -euo pipefail
D="$(cd "$(dirname "$0")" && pwd)"; W=/workspace/wrv017
JDK=/workspace/wrv019/jdk25/bin
L=/workspace/wrv015/scratch/libraries
API=$L/io/papermc/paper/paper-api/26.2.build.133-stable/paper-api-26.2.build.133-stable.jar
CP="$API"
for j in $(find $L/net/kyori $L/org/jspecify $L/com/google $L/net/md-5 $L/org/yaml $L/org/slf4j $L/org/joml $L/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
FG=/workspace/mc/test26/plugins/floodgate.jar   # test only: real Cumulus classes to check the form builders
O=$D/build; rm -rf $O; mkdir -p $O/classes $O/test
$JDK/javac --release 21 -proc:none -Xlint:-options -cp "$CP" -d $O/classes $(find $D/src -name '*.java')
cp $D/plugin.yml $D/config.yml $O/classes/
$JDK/javac --release 21 -proc:none -nowarn -cp "$O/classes:$CP:$FG" -d $O/test $(find $D/tests -name '*.java')
set +e
$JDK/java -Dsrc=$D/src -Dcfg=$D/config.yml -Dplugin=$D/plugin.yml -cp "$O/test:$O/classes:$CP:$FG" com.winkreyes.excelwaypoints.WaypointsTest | tee $O/test.log
RC=${PIPESTATUS[0]}
set -e
[ $RC -eq 0 ] || { echo "TESTS FAILED"; exit 1; }
python3 - <<P
import zipfile,os
out="$W/ExcelWaypoints-0.1.0.jar"; root="$O/classes"
fs=sorted(os.path.join(dp,f) for dp,_,fn in os.walk(root) for f in fn)
with zipfile.ZipFile(out,"w",zipfile.ZIP_DEFLATED) as z:
    for f in fs:
        zi=zipfile.ZipInfo(os.path.relpath(f,root),(2026,10,16,0,0,0)); zi.compress_type=zipfile.ZIP_DEFLATED
        z.writestr(zi,open(f,"rb").read())
print("built",out)
P
