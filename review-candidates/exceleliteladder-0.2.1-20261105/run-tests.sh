#!/bin/bash
# Compile and run the ExcelEliteLadder plain-JVM suites (core only; Bukkit glue is
# proven on the rig). 0.2.0 adds LadderConfigSuite, which parses the shipped
# ladder.yml with SnakeYAML (the YAML engine family Bukkit itself uses), so it
# is on the test classpath.
set -euo pipefail
cd "$(dirname "$0")"
JDK=${JDK:-/home/hatch/workspace/.jdk25}
SNAKE=${SNAKE:-/home/hatch/workspace/excelbuildmode-pilot/runtime-server/libraries/org/yaml/snakeyaml/2.6/snakeyaml-2.6.jar}
rm -rf build/test-classes && mkdir -p build/test-classes
"$JDK/bin/javac" --release 21 -proc:none -nowarn -cp "$SNAKE" -d build/test-classes \
  $(find src/com/winkreyes/exceleliteladder/core -name '*.java') $(find tests -name '*.java')
"$JDK/bin/java" -cp "build/test-classes:$SNAKE" TestMain ladder.yml
