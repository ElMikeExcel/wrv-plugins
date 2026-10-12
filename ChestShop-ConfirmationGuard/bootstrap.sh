#!/usr/bin/env bash
# Downloads only the official pinned build, verifies it, and installs compile API.
set -euo pipefail
MODULE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(dirname "$MODULE")
EXPECTED=b31a40e9561d399d8b04fada4e863ae28c523618976ff3a3b1a6f16fe3aa8ab2
JAR=${CHESTSHOP_ORIGINAL_JAR:-"$MODULE/.cache/ChestShop-3.13-pre-1-build474.jar"}
if [[ ! -f "$JAR" ]]; then
  mkdir -p "$(dirname "$JAR")"
  TEMP=$(mktemp "$(dirname "$JAR")/.download.XXXXXX")
  trap 'rm -f "$TEMP"' EXIT
  curl --fail --location --retry 3 'https://ci.minebench.de/job/ChestShop-3/474/artifact/target/ChestShop.jar' -o "$TEMP"
  echo "$EXPECTED  $TEMP" | sha256sum --check --status || { echo 'ChestShop checksum mismatch' >&2; exit 1; }
  mv "$TEMP" "$JAR"
fi
echo "$EXPECTED  $JAR" | sha256sum --check --status || { echo 'ChestShop checksum mismatch' >&2; exit 1; }
# Keep a verified input copy at the deterministic packaging location, even when supplied externally.
mkdir -p "$MODULE/.cache"
CACHED="$MODULE/.cache/ChestShop-3.13-pre-1-build474.jar"
[[ "$(realpath "$JAR")" == "$(realpath -m "$CACHED")" ]] || cp "$JAR" "$CACHED"
SOURCE="$MODULE/.cache/upstream-source.tar.gz"
SOURCE_SHA=00f15e49bedd11c8300dd4ffaee6a783b92b26af39f5ba00a7683814c7564501
if [[ ! -f "$SOURCE" ]]; then
  curl --fail --location --retry 3 'https://codeload.github.com/ChestShop-authors/ChestShop-3/tar.gz/dde7ee2155303714c2d5054deeccef4618e3939c' -o "$SOURCE"
fi
echo "$SOURCE_SHA  $SOURCE" | sha256sum --check --status || { echo 'ChestShop source checksum mismatch' >&2; exit 1; }
"$ROOT/mvnw" -B -ntp -N -f "$ROOT/pom.xml" "$@" org.apache.maven.plugins:maven-install-plugin:3.1.3:install-file \
  -Dfile="$CACHED" -DgroupId=com.acrobot.chestshop -DartifactId=chestshop-original \
  -Dversion=3.13-pre-1-build474 -Dpackaging=jar -DgeneratePom=true
