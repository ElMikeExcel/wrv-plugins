#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# JDK 21+ and Maven 3.9+; use the exact pinned dependency versions in pom.xml.
# MAVEN_COMMAND and MAVEN_REPO can point to an existing cache for offline builds.
# Remove only this module's generated build directory, avoiding an extra Maven
# clean-plugin download in the offline verification environment.
rm -rf -- target
args=(verify)
if [[ "${OFFLINE:-0}" == "1" ]]; then args=(-o "${args[@]}"); fi
if [[ -n "${MAVEN_REPO:-}" ]]; then args=("-Dmaven.repo.local=$MAVEN_REPO" "${args[@]}"); fi
"${MAVEN_COMMAND:-mvn}" "${args[@]}" "$@"
sha256sum target/ExcelMobVeil-0.1.0.jar
