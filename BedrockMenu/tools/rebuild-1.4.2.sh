#!/usr/bin/env bash
# Reconstruct the historical 1.4.2 payload from archived classes + original ASM
# patch. ZIP metadata may differ; this is not the active source build.
# Requires JDK 21+. Uses the pinned repository Maven bootstrap when ASM is absent.
# Override ASM=/readable/asm.jar (9.8+); override MVN=/path/to/mvn if needed.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$PWD/build/recovery-1.4.2"
mkdir -p "$OUT"
if [[ -z "${ASM:-}" ]]; then
  "${MVN:-$PWD/../mvnw}" -B -ntp -N -f "$PWD/../pom.xml" \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy \
    -Dartifact=org.ow2.asm:asm:9.9.1 \
    -DoutputDirectory="$OUT/dependencies"
  ASM="$OUT/dependencies/asm-9.9.1.jar"
fi
if [[ ! -f "$ASM" || ! -r "$ASM" ]]; then
  echo "ASM must name a readable ASM 9.8+ jar: $ASM" >&2
  exit 1
fi
# Isolate generated output, retaining downloaded dependencies between runs.
rm -rf "$OUT/tools" "$OUT/classes"
mkdir -p "$OUT/tools" "$OUT/classes"
cp -r classes-1.4.1/. "$OUT/classes/"
javac --release 21 -cp "$ASM" -d "$OUT/tools" tools/Patch.java
find "$OUT/classes" -name '*.class' -exec java -cp "$ASM:$OUT/tools" Patch {} +
cp tools/plugin-1.4.2.yml "$OUT/classes/plugin.yml"
jar --create --file "$OUT/BedrockMenu-1.4.2.jar" \
  --manifest "$OUT/classes/META-INF/MANIFEST.MF" -C "$OUT/classes" .
echo "built $OUT/BedrockMenu-1.4.2.jar"
