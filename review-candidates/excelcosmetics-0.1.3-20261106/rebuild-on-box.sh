#!/usr/bin/env bash
# Rebuild on the WRV box (read-only on the packet: works in a scratch copy, deletes it after).
set -euo pipefail
H=$(cd "$(dirname "$0")/.." && pwd); W=$(mktemp -d); trap 'rm -rf "$W"' EXIT
cp -r "$H/source/." "$W/"; cd "$W"
JDK=/home/box/tools/jdk-25.0.5+8 LIBS=/workspace/build-libs-wrv/paper1.21.4 RT=/workspace/build-libs-wrv/runtime-libraries bash ec012/build.sh | head -1 || true
echo "target  45fc556ca0f84f77209faa1fb13a540f39e5a31248b0b75467d6d5586636509b"
