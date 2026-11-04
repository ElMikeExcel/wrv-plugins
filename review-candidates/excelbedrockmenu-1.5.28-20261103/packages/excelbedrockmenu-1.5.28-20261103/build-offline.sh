#!/usr/bin/env bash
# Excel signature: compile only; does not install or run a server.
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to JDK25 (exact: ~/workspace/.jdk25, Temurin 25.0.4.1 — the JDK of record for the 1.5.27 base proof)}" "${PAPER_LIBRARIES:?Set exact Paper26.3-214 libraries directory (~/workspace/paper231-rerun/bootstrap/libraries)}" "${OUTPUT_DIR:?Set fresh output directory}"
mkdir -p "$OUTPUT_DIR/classes"
CP=$(find "$PAPER_LIBRARIES" -name '*.jar' | sort | paste -sd:)
"$JAVA_HOME/bin/javac" --release 21 -proc:none -cp "$CP" -d "$OUTPUT_DIR/classes" $(find src -name '*.java' | sort)
cp plugin.yml config.yml "$OUTPUT_DIR/classes/"
"$JAVA_HOME/bin/jar" --create --date=2026-11-01T00:00:00Z --file "$OUTPUT_DIR/ExcelBedrockMenu-1.5.28.jar" -C "$OUTPUT_DIR/classes" .
sha256sum "$OUTPUT_DIR/ExcelBedrockMenu-1.5.28.jar"
