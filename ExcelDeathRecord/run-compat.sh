#!/bin/bash
# Rollback check: does the 0.1.0 reader accept files 0.2.0 has marked?
# Usage: ./run-compat.sh /path/to/0.1.0/source
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/box/tools/jdk-25.0.5+8}
OLD=${1:?path to ExcelDeathRecord 0.1.0 source}
T=build/compat; rm -rf $T && mkdir -p $T/new $T/old $T/data
"$JDK/bin/javac" --release 21 -proc:none -nowarn -d $T/new $(find src/com/winkreyes/exceldeathrecord/core -name '*.java') tests/Check.java tests/RecordFormatTest.java tests-compat/WriteMarked.java
"$JDK/bin/java" -cp $T/new WriteMarked $T/data
"$JDK/bin/javac" --release 21 -proc:none -nowarn -d $T/old $(find "$OLD/src/com/winkreyes/exceldeathrecord/core" -name '*.java') tests-compat/ReadOld.java
"$JDK/bin/java" -cp $T/old ReadOld $T/data
