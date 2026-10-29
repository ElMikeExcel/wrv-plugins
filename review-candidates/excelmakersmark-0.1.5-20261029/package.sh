#!/usr/bin/env bash
# Excel WRV optional source build packaging, not an installation command.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")" && pwd)
: "${JAVA_HOME:?}" "${BUILD_DIR:?}"
test -d "$BUILD_DIR/classes"
cp "$ROOT/plugin.yml" "$ROOT/config.yml" "$ROOT/LICENSE" "$BUILD_DIR/classes/"
if test -f "$ROOT/CHANGELOG.md"; then cp "$ROOT/CHANGELOG.md" "$BUILD_DIR/classes/"; fi
(cd "$BUILD_DIR/classes"; find . -type f | sort > "$BUILD_DIR/entries.txt"; "$JAVA_HOME/bin/jar" --create --file "$BUILD_DIR/public-source-rebuild.jar" --date=2026-10-29T00:00:00Z --no-manifest @"$BUILD_DIR/entries.txt")
sha256sum "$BUILD_DIR/public-source-rebuild.jar"
