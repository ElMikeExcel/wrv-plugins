#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
"$ROOT/ChestShop-ConfirmationGuard/bootstrap.sh" "$@"
"$ROOT/mvnw" -f "$ROOT/pom.xml" -pl WRVShops -am clean verify "$@"
python3 "$ROOT/ChestShop-ConfirmationGuard/package.py"
