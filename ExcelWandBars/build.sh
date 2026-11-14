#!/usr/bin/env bash
# ExcelWandBars 0.3.18 deterministic build (house method, cf. 0.3.17 SOURCE.md).
# Usage: build.sh <OUT_DIR>   (compiles tree-0.3.18, repacks from shipped 0.3.16)
set -euo pipefail
export LANG=C.UTF-8 LC_ALL=C.UTF-8
NEW=$HOME/workspace/wandbars-fsync-fix-20261114
JAVA_HOME=$HOME/workspace/.jdk25/jdk-25.0.5+8
OUT=$1
mkdir -p "$OUT/classes"
CP=$(cat "$NEW/work/cp.txt")
cd "$NEW/tree-0.3.18"
find src -name '*.java' | sort > "$OUT/sources.txt"
$JAVA_HOME/bin/javac -proc:none -cp "$CP" -d "$OUT/classes" @"$OUT/sources.txt"
python3 "$NEW/work/repack.py" "$NEW/work/shipped-0.3.16.jar" "$OUT/classes" \
  "$NEW/work/shipped-0318" "$OUT/candidate.jar"
