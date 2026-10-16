#!/usr/bin/env bash
# Builds ExcelBedrockMenu-1.5.3.jar (renamed from BedrockMenu 1.4.2-menu.1;
# 1.5.1 adds the "Adventurers Guild" button, which runs /ag as the player;
# 1.5.3 adds the "Road to the Guild" button before it, which runs /seals,
# and makes the Guild button label show a lock while the guild is sealed).
# Unlike the old BedrockMenu build (ASM patch on 1.4.1 classes), the renamed
# plugin builds straight from source: this src/ tree is the 1.4.1 source with
# the 1.4.2 button patch (Spawn Shop) applied at source level, plus the rename
# and the legacy data-folder migration. Needs JDK 21+.
# Override the Paper API location with PAPER_API=... if your ~/.m2 differs.
set -euo pipefail
cd "$(dirname "$0")"
PAPER_API=${PAPER_API:-$HOME/.m2/repository/io/papermc/paper/paper-api/1.21.4-R0.1-SNAPSHOT/paper-api-1.21.4-R0.1-SNAPSHOT.jar}
CP="$PAPER_API:$(find $HOME/.m2/repository/net/kyori $HOME/.m2/repository/net/md-5 $HOME/.m2/repository/com/google/guava -name '*.jar' 2>/dev/null | tr '\n' ':')"
rm -rf build && mkdir -p build/classes
javac --release 21 -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp plugin.yml  build/classes/
(cd build/classes && jar cf ../ExcelBedrockMenu-1.5.3.jar .)
echo "built build/ExcelBedrockMenu-1.5.3.jar"
