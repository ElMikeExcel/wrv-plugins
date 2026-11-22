#!/bin/bash
# Compile and run the ExcelEMLeash plain-JVM suites.
set -euo pipefail
cd "$(dirname "$0")"
JDK="${JDK:-$HOME/workspace/.jdk25}"
rm -rf test-classes
mkdir -p test-classes
"$JDK/bin/javac" -encoding UTF-8 -d test-classes \
  src/src/com/winkreyes/excelemleash/LeashRemovalPolicy.java \
  src/src/com/winkreyes/excelemleash/ReturnPolicy.java \
  src/tests/com/winkreyes/excelemleash/Check.java \
  src/tests/com/winkreyes/excelemleash/LeashRemovalPolicyTest.java \
  src/tests/com/winkreyes/excelemleash/ReturnPolicyTest.java \
  src/tests/com/winkreyes/excelemleash/TestMain.java
"$JDK/bin/java" -cp test-classes TestMain
