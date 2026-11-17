#!/usr/bin/env bash
# ExcelSilkWatch - Excel signature - MikeExcel
# Sanitized for the records package (board-manager run 494): machine-specific
# default paths from the private build packet were replaced with required
# environment variables, and the loopback bind is written by name instead of
# as a numeric address. Set JDK_HOME, PAPER_SERVER_DIR, PAPER_JAR_FILE,
# ANNOTATIONS_JAR and PAPI_JAR before running.
set -euo pipefail
cd "$(dirname "$0")"
./build.sh
JDK=${JDK_HOME:?set JDK_HOME to a JDK 25 home}
BASE=${PAPER_SERVER_DIR:?set PAPER_SERVER_DIR to the disposable test server directory}
CP=$(find "$BASE/libraries" -name '*.jar' | sort | paste -sd:)
CP="$CP:${ANNOTATIONS_JAR:?set ANNOTATIONS_JAR to the JetBrains annotations jar}"
PAPI=${PAPI_JAR:?set PAPI_JAR to the PlaceholderAPI jar}
CP="$CP:$PAPI"
mkdir -p build/test-classes isolated-server/plugins
if [ "${TEST_PAPI:-0}" = 1 ]; then cp "${RUNTIME_PAPI:-$PAPI}" isolated-server/plugins/PlaceholderAPI.jar; else rm -f isolated-server/plugins/PlaceholderAPI.jar; fi
"$JDK/bin/javac" --release 21 -proc:none -cp "build/classes:$CP" -d build/test-classes tests/ObserverProbe.java
cp tests/plugin.yml build/test-classes/
"$JDK/bin/jar" --create --file isolated-server/plugins/ObserverProbe.jar -C build/test-classes .
cp build/ExcelSilkWatch-0.1.3-OFFLINE.jar isolated-server/plugins/
for dir in libraries versions cache; do test -e isolated-server/$dir || ln -s "$BASE/$dir" isolated-server/$dir; done
cp "$BASE/${PAPER_JAR_FILE:?set PAPER_JAR_FILE to the Paper server jar filename}" isolated-server/paper.jar
printf 'eula=true\n' > isolated-server/eula.txt
printf 'server-ip=localhost\nserver-port=0\nonline-mode=false\nlevel-name=probe-world\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\nmax-players=1\n' > isolated-server/server.properties
cd isolated-server
rm -f probe-results.txt
timeout 120 "$JDK/bin/java" -Dprobe.expectPapi="${TEST_PAPI:-0}" -Xms256M -Xmx768M -jar paper.jar --nogui > runtime.log 2>&1
cp probe-results.txt "probe-results-papi-${TEST_PAPI:-0}.txt"
cp runtime.log "runtime-papi-${TEST_PAPI:-0}.log"
cat probe-results.txt
! grep -q '^FAIL' probe-results.txt
grep -q '^PASS all ' probe-results.txt
