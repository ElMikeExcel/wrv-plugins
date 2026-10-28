#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${CLASSPATH:?Set CLASSPATH to compatible Paper API and dependencies (BossTrial also needs GuildGate API)}"
mkdir -p build/classes
javac --release 21 -proc:none -cp "$CLASSPATH" -d build/classes $(find src -name '*.java' | sort)
cp plugin.yml config.yml build/classes/
(cd build/classes; jar --create --file ../candidate.jar --date=2026-10-28T00:00:00Z $(find . -type f | sort))
