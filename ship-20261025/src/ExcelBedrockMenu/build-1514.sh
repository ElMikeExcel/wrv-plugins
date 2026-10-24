#!/usr/bin/env bash
# Reproducible build + test for ExcelBedrockMenu 1.5.14 (Utility Pouch
# main-menu button moves up: index 12 -> index 3, top daily-driver
# cluster; every other entry keeps its 1.5.13 relative order).
# Same shape as build-1513.sh; system JDK 25,
# Paper API 26.2 build 123 libraries from the weekendxp scratch set).
# build-157-local.sh in this folder reproduces the live 1.5.7 jar
# byte-for-byte from the same tree (provenance check, SHA-256 0787c161...).
set -euo pipefail
D="$(cd "$(dirname "$0")" && pwd)"; cd "$D"
JDK=/home/hatch/workspace/.jdk25/bin; L=/home/hatch/workspace/weekendxp-1.2.0/scratch/libraries
CP=$L/io/papermc/paper/paper-api/26.2.build.123-stable/paper-api-26.2.build.123-stable.jar
for j in $(find $L/net/kyori $L/org/jspecify $L/com/google $L/net/md-5 $L/org/yaml $L/org/slf4j $L/org/joml $L/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
rm -rf build; mkdir -p build/classes build/test
$JDK/javac --release 21 -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp plugin.yml config.yml build/classes/
$JDK/javac --release 21 -proc:none -nowarn -cp "build/classes:$CP" -d build/test $(find test -name '*.java')
$JDK/java -cp "build/test:build/classes:$CP" com.winkreyes.excelbedrockmenu.MenuLogicTest | tee build/test.log
python3 - <<P
import zipfile,os
out="$D/build/ExcelBedrockMenu-1.5.14.jar"; root="$D/build/classes"
fs=sorted(os.path.join(dp,f) for dp,_,fn in os.walk(root) for f in fn)
with zipfile.ZipFile(out,"w",zipfile.ZIP_DEFLATED) as z:
    for f in fs:
        zi=zipfile.ZipInfo(os.path.relpath(f,root),(2026,10,17,0,0,0)); zi.compress_type=zipfile.ZIP_DEFLATED
        z.writestr(zi,open(f,"rb").read())
print("built",out)
P
