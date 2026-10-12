#!/usr/bin/env bash
# Rebuilds BedrockMenu-1.4.2.jar exactly as it was produced:
# Argus's compiled 1.4.1 classes (classes-1.4.1/), patched with ASM (tools/Patch.java) to
#  - rewrite BedrockMenu.<clinit> BUTTONS/COMMANDS (adds "Spawn Shop" -> dm open spawnshop)
#  - set classfile version 65 (Java 21) on every class
# Needs: JDK 21+, ASM 9.x jar (set ASM=path).
set -euo pipefail
cd "$(dirname "$0")"
ASM=${ASM:-$HOME/.m2/repository/org/ow2/asm/asm/9.9.1/asm-9.9.1.jar}  # ASM >= 9.8 needed (input classes are Java 25)
rm -rf build && mkdir -p build/tools build/classes
cp -r classes-1.4.1/. build/classes/
javac -cp "$ASM" -d build/tools tools/Patch.java
java -cp "$ASM:build/tools" Patch $(find build/classes -name '*.class')
cp tools/plugin-1.4.2.yml build/classes/plugin.yml
(cd build/classes && jar cfm ../BedrockMenu-1.4.2.jar META-INF/MANIFEST.MF $(find . -type f ! -path ./META-INF/MANIFEST.MF))
echo "built build/BedrockMenu-1.4.2.jar"
