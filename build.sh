#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./mvnw -B -ntp clean verify "$@"
python3 -m unittest discover -s tests -v
python3 -m unittest discover -s SpawnShop-DeluxeMenus/tests -v

python3 -m unittest discover -s WRVShops/tests_python -v
