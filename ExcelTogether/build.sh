#!/usr/bin/env bash
# Build + test ExcelTogether 0.1.0. Needs JDK 25 (Paper API 26.2 is class v69).
set -euo pipefail
D="$(cd "$(dirname "$0")" && pwd)"; W=/workspace/wrv019
JDK=$W/jdk25/bin
API=/workspace/wrv015/scratch/libraries/io/papermc/paper/paper-api/26.2.build.133-stable/paper-api-26.2.build.133-stable.jar
CP="$API"
for j in $(find /workspace/wrv015/scratch/libraries/net/kyori /workspace/wrv015/scratch/libraries/org/jspecify /workspace/wrv015/scratch/libraries/com/google /workspace/wrv015/scratch/libraries/net/md-5 /workspace/wrv015/scratch/libraries/org/yaml /workspace/wrv015/scratch/libraries/org/slf4j /workspace/wrv015/scratch/libraries/org/joml /workspace/wrv015/scratch/libraries/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
JOBS="$W/inputs/Jobs.jar:$W/inputs/CMILib.jar"
O=$D/build; rm -rf $O; mkdir -p $O/classes $O/test
$JDK/javac --release 21 -proc:none -nowarn -cp "$CP:$JOBS" -d $O/classes $(find $D/src -name '*.java')
cp $D/plugin.yml $D/config.yml $O/classes/
$JDK/javac --release 21 -proc:none -nowarn -cp "$O/classes:$CP:$JOBS" -d $O/test $(find $D/tests -name '*.java')
$JDK/java -cp "$O/test:$O/classes:$CP:$JOBS" com.winkreyes.exceltogether.PartyTest | tee $O/test.log
python3 - <<P
import zipfile,os
out="$W/ExcelTogether-0.1.0.jar"; root="$O/classes"
fs=sorted(os.path.join(dp,f) for dp,_,fn in os.walk(root) for f in fn)
with zipfile.ZipFile(out,"w",zipfile.ZIP_DEFLATED) as z:
    for f in fs:
        zi=zipfile.ZipInfo(os.path.relpath(f,root),(2026,10,16,0,0,0)); zi.compress_type=zipfile.ZIP_DEFLATED
        z.writestr(zi,open(f,"rb").read())
print("built",out)
P
