#!/bin/bash
# Build CosmeticRigDriver.jar for the ExcelCosmetics scratch rig.
# Classpath: the rig's own Paper server jar (API + NMS) + its
# libraries + the Vault jar (economy API for the balance probe).
set -euo pipefail
cd "$(dirname "$0")"
JDK=~/workspace/.jdk25
SERVER=~/workspace/excelcosmetics/scratch/server
CP="$SERVER/versions/26.2/paper-26.2.jar:$SERVER/plugins/Vault-2.0.0.jar"
for j in $(find "$SERVER/libraries" -name '*.jar'); do CP="$CP:$j"; done
rm -rf build/classes && mkdir -p build/classes
"$JDK/bin/javac" -proc:none -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp plugin.yml build/classes/
"$JDK/bin/jar" --create --file CosmeticRigDriver.jar -C build/classes .
ls -l CosmeticRigDriver.jar
