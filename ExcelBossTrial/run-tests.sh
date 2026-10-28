#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 25}"
: "${BUILD_CP:?Set BUILD_CP to full compile/runtime dependency classpath}"
mkdir -p build/test-classes
find src tests -name '*.java' | sort > build/test-sources.txt
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$BUILD_CP" -d build/test-classes @build/test-sources.txt
while IFS= read -r file; do
 class=${file#tests/}; class=${class%.java}; class=${class//\//.}
 "$JAVA_HOME/bin/java" -cp "build/test-classes:$BUILD_CP" "$class"
done < <(grep -rlE 'public static void main' tests --include='*.java' | sort)
