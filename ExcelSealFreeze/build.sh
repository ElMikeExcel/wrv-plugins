#!/usr/bin/env bash
set -euo pipefail; cd "$(dirname "$0")"
export JAVA_HOME=${JAVA_HOME:-$HOME/workspace/.jdk25/jdk-25.0.5+8}; export PATH=$JAVA_HOME/bin:$PATH
PAPER=${PAPER_API:-"$HOME/workspace/menujoin/lib/*"}
rm -rf work/classes work/test-classes; mkdir -p work/classes work/test-classes
find src/main/java -name '*.java' > work/sources.txt
javac -cp "$PAPER" -d work/classes @work/sources.txt
cp src/main/resources/plugin.yml src/main/resources/config.yml work/classes/
printf 'ExcelSealFreeze 0.1.0\nBuild: EXC-SEALFREEZE-0.1.0-WRV-20261114-01\n' > work/classes/excel-build.txt
SQLITE=${SQLITE_JDBC:-"$HOME/workspace/spell-audit/rig/server/libraries/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar"}
EMDB=${EM_DB:-"$HOME/workspace/g4-pull-bm326/em_player_data.db"}
javac -cp work/classes -d work/test-classes src/test/java/com/winkreyes/excelsealfreeze/SealFreezeTest.java
java -cp "work/classes:work/test-classes:$SQLITE" -Dem.db="$EMDB" com.winkreyes.excelsealfreeze.SealFreezeTest | tee TEST-RESULTS.txt
jar --create --file ExcelSealFreeze-0.1.0.jar --date=2026-10-28T00:00:00Z -C work/classes .
sha256sum ExcelSealFreeze-0.1.0.jar
