#!/usr/bin/env bash
# Local harness: compiles the test-relevant closure of the MakersMark tree
# (core + Accomplishments + Marks + history) and runs the recorded JUnit
# suite via the tree's own TestRunner. The 7 composed-Magic-dependent
# sources (bridge/registrations/main/wrv-magic) cannot compile outside the
# original offline env (wrv.magic.evidence exists only in the composed
# Magic-LEARNED jar); no recorded test touches them.
set -euo pipefail
ROOT="$HOME/workspace/makersmark-fsync-fix-20261114"
SRC="$ROOT/ExcelMakersMark"
JDK="$HOME/workspace/.jdk25"
OUT="$ROOT/test-out"; rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/tests"
LIBS=$(find "$HOME/workspace/excelshops/build-libs" -name '*.jar' ! -name 'mockito*' ! -name 'byte-buddy*' | sort | tr '\n' ':')
CP="${LIBS}$HOME/workspace/menujoin/lib/bungeecord-chat.jar:$HOME/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar:$HOME/workspace/magic-scratch/downloads/Magic-11.2.5.jar:$HOME/workspace/minecraft-server/plugins/PlaceholderAPI-2.12.3.jar"
JUNIT="$HOME/workspace/excelshops/build-libs/junit-platform-console-standalone-1.11.4.jar:$HOME/workspace/.jdk/apache-maven-3.9.9/lib/slf4j-api-1.7.36.jar:$HOME/workspace/m2-repo/org/apache/logging/log4j/log4j-api/2.25.5/log4j-api-2.25.5.jar"
{ find "$SRC/src/com/winkreyes/makersmark/core" "$SRC/src/com/winkreyes/makersmark/history" -name '*.java';
  echo "$SRC/src/com/winkreyes/makersmark/Accomplishments.java";
  echo "$SRC/src/com/winkreyes/makersmark/Marks.java";
  echo "$SRC/src/com/winkreyes/makersmark/OrdinaryHistoryRecorder.java";
  echo "$SRC/src/com/winkreyes/makersmark/HistoryRecoveryService.java";
  echo "$ROOT/test-stubs/com/winkreyes/makersmark/ExcelMakersMark.java"; } | sort > "$OUT/src.txt"
"$JDK/bin/javac" -proc:none -encoding UTF-8 -cp "$CP" -d "$OUT/classes" @"$OUT/src.txt"
find "$SRC/tests" -name '*.java' | sort > "$OUT/tests.txt"
"$JDK/bin/javac" -proc:none -encoding UTF-8 -cp "$OUT/classes:$CP:$JUNIT" -d "$OUT/tests" @"$OUT/tests.txt" "$SRC/TestRunner.java"
"$JDK/bin/java" -cp "$OUT/classes:$OUT/tests:$CP:$JUNIT" TestRunner
