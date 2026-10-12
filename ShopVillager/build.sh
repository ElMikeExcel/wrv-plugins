#!/usr/bin/env bash
# Builds ShopVillager-1.0.0.jar. Needs JDK 21+. Compiles to Java 21 bytecode (--release 21).
# Override jar locations with PAPER_API=... PAPI=... if your ~/.m2 differs.
set -euo pipefail
cd "$(dirname "$0")"
PAPER_API=${PAPER_API:-$HOME/.m2/repository/io/papermc/paper/paper-api/1.21.4-R0.1-SNAPSHOT/paper-api-1.21.4-R0.1-SNAPSHOT.jar}
PAPI=${PAPI:-$HOME/.m2/repository/me/clip/placeholderapi/2.11.6/placeholderapi-2.11.6.jar}
# Paper API transitive deps (adventure, bungee-chat, etc.) from the local maven repo:
CP="$PAPER_API:$PAPI:$(find $HOME/.m2/repository/net/kyori $HOME/.m2/repository/net/md-5 $HOME/.m2/repository/com/google/guava -name '*.jar' 2>/dev/null | tr '\n' ':')"
rm -rf build && mkdir -p build/classes
javac --release 21 -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp plugin.yml config.yml build/classes/
(cd build/classes && jar cf ../ShopVillager-1.0.0.jar .)
echo "built build/ShopVillager-1.0.0.jar"
