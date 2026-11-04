#!/usr/bin/env bash
# Excel WRV portable source-record package; no installation or upload.
set -euo pipefail
cd "$(dirname "$0")"
: "${BUILD_DIR:?Set BUILD_DIR to an external build output directory}"
: "${OUTPUT_JAR:?Set OUTPUT_JAR to an external output artifact path}"
bash build-test.sh
cp plugin.yml config.yml "$BUILD_DIR/classes/"
JAR=${JAR:-jar}
"$JAR" --create --file "$OUTPUT_JAR" --date=2026-10-28T00:00:00Z -C "$BUILD_DIR/classes" .
sha256sum "$OUTPUT_JAR"
