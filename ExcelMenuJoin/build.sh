#!/usr/bin/env bash
# Builds ExcelMenuJoin-1.3.0.jar (renamed from MenuJoin 1.2.0). Needs JDK 21+.
# Override jar locations with PAPER_API=... PAPI=... if your ~/.m2 differs.
set -euo pipefail
cd "$(dirname "$0")"
PAPER_API=${PAPER_API:-$HOME/.m2/repository/io/papermc/paper/paper-api/1.21.4-R0.1-SNAPSHOT/paper-api-1.21.4-R0.1-SNAPSHOT.jar}
PAPI=${PAPI:-$HOME/.m2/repository/me/clip/placeholderapi/2.11.6/placeholderapi-2.11.6.jar}
CP="$PAPER_API:$PAPI:$(find $HOME/.m2/repository/net/kyori $HOME/.m2/repository/net/md-5 $HOME/.m2/repository/com/google/guava -name '*.jar' 2>/dev/null | tr '\n' ':')"
rm -rf build && mkdir -p build/classes
javac --release 21 -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp plugin.yml  build/classes/
(cd build/classes && jar cf ../ExcelMenuJoin-1.3.0.jar .)
echo "built build/ExcelMenuJoin-1.3.0.jar"
