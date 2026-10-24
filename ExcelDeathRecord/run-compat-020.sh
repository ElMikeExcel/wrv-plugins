#!/bin/bash
# Rollback check for 0.3.0 -> 0.2.0: does the 0.2.0 reader accept
# Burn Ledger files 0.3.0 has marked through Lost & Found (restoredBy SELF)?
# Usage: ./run-compat-020.sh /path/to/0.2.0/source
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
OLD=${1:?path to ExcelDeathRecord 0.2.0 source}
T=build/compat020; rm -rf $T && mkdir -p $T/new $T/old $T/data
"$JDK/bin/javac" --release 21 -proc:none -nowarn -d $T/new $(find src/com/winkreyes/exceldeathrecord/core -name '*.java') tests-compat/WriteSelf.java
"$JDK/bin/java" -cp $T/new WriteSelf $T/data
"$JDK/bin/javac" --release 21 -proc:none -nowarn -d $T/old $(find "$OLD/src/com/winkreyes/exceldeathrecord/core" -name '*.java') tests-compat/ReadOld020.java
"$JDK/bin/java" -cp $T/old ReadOld020 $T/data
