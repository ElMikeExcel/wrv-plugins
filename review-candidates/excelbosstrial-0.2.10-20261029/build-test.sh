#!/usr/bin/env bash
# Excel WRV: portable full-source compile and standalone test harnesses.
set -euo pipefail
cd "$(dirname "$0")"
: "${DEPENDENCY_CLASSPATH:?Supply Paper API/transitives, PlaceholderAPI and ExcelGuildGate API classpath}"
JAVAC=${JAVAC:-javac}; JAVA=${JAVA:-java}
OUT=${BUILD_DIR:-$(mktemp -d)}
mkdir -p "$OUT/classes" "$OUT/tests"
find src -name '*.java' | sort > "$OUT/source-list"
"$JAVAC" --release 21 -proc:none -cp "$DEPENDENCY_CLASSPATH" -d "$OUT/classes" @"$OUT/source-list"
find tests -name '*.java' | sort > "$OUT/test-list"
"$JAVAC" --release 21 -proc:none -cp "$OUT/classes:$DEPENDENCY_CLASSPATH" -d "$OUT/tests" @"$OUT/test-list"
while read -r source; do
 if grep -q 'public static void main' "$source"; then
  package=$(sed -n 's/.*package \([^;]*\);.*/\1/p' "$source" | head -1)
  class=$(basename "$source" .java)
  "$JAVA" -cp "$OUT/tests:$OUT/classes:$DEPENDENCY_CLASSPATH" "${package:+$package.}$class"
 fi
done < "$OUT/test-list"
printf 'PASS: compiled all source and ran all main-method harnesses. Build output: ephemeral local directory.\n'
