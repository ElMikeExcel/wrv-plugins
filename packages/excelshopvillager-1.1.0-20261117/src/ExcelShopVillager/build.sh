#!/usr/bin/env bash
# Builds ExcelShopVillager-1.1.0.jar (renamed from ShopVillager 1.0.0). Needs JDK 21+.
# Override the Paper API location with PAPER_API=... if your ~/.m2 differs.
set -euo pipefail
cd "$(dirname "$0")"
PAPER_API=${PAPER_API:-$HOME/.m2/repository/io/papermc/paper/paper-api/1.21.4-R0.1-SNAPSHOT/paper-api-1.21.4-R0.1-SNAPSHOT.jar}
CP="$PAPER_API:$(find $HOME/.m2/repository/net/kyori $HOME/.m2/repository/net/md-5 $HOME/.m2/repository/com/google/guava -name '*.jar' 2>/dev/null | tr '\n' ':')"
rm -rf build && mkdir -p build/classes
javac --release 21 -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp plugin.yml config.yml build/classes/
(cd build/classes && jar cf ../ExcelShopVillager-1.1.0.jar .)
echo "built build/ExcelShopVillager-1.1.0.jar"
