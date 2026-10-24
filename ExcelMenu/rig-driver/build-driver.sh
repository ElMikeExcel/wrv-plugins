#!/bin/bash
# Build MenuRigDriver.jar for the ExcelMenu scratch rig.
# Classpath: the rig's own Paper server jar (API + NMS) + its libraries.
set -euo pipefail
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
SERVER=~/workspace/excelmenu/build-0.1.3/rig-server
CP="$SERVER/versions/26.2/paper-26.2.jar"
for j in $(find "$SERVER/libraries" -name '*.jar'); do CP="$CP:$j"; done
rm -rf build/classes && mkdir -p build/classes
"$JDK/bin/javac" -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp plugin.yml build/classes/
"$JDK/bin/jar" --create --file MenuRigDriver.jar -C build/classes .
ls -l MenuRigDriver.jar

# ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
