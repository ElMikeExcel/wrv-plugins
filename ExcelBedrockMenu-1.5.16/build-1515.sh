#!/usr/bin/env bash
# usage: mk.sh <tree> <version>
set -euo pipefail
D=$1; V=$2; cd "$D"
JDK=/workspace/wrv019/jdk25/bin; L=/workspace/acro/scratch/libraries
CP=$(find $L -name '*.jar' | tr '\n' ':')
rm -rf build; mkdir -p build/classes build/test
$JDK/javac --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp plugin.yml config.yml build/classes/
$JDK/javac --release 21 -proc:none -nowarn -cp "build/classes:$CP" -d build/test $(find test -name '*.java')
$JDK/java -cp "build/test:build/classes:$CP" com.winkreyes.excelbedrockmenu.MenuLogicTest > build/test.log; tail -2 build/test.log
python3 - "$D/build/ExcelBedrockMenu-$V.jar" "$D/build/classes" <<'P'
import zipfile,os,sys
out,root=sys.argv[1],sys.argv[2]
fs=sorted(os.path.join(dp,f) for dp,_,fn in os.walk(root) for f in fn)
with zipfile.ZipFile(out,"w",zipfile.ZIP_DEFLATED) as z:
    for f in fs:
        zi=zipfile.ZipInfo(os.path.relpath(f,root),(2026,10,17,0,0,0)); zi.compress_type=zipfile.ZIP_DEFLATED
        z.writestr(zi,open(f,"rb").read())
P
sha256sum build/*.jar
