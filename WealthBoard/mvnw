#!/usr/bin/env sh
# Minimal checksum-verified Maven bootstrap. Requires JDK 21+, curl or wget, tar,
# and sha512sum or shasum. Always runs the pinned distribution.
set -eu
VERSION=3.9.9
SHA512=a555254d6b53d267965a3404ecb14e53c3827c09c3b94b5678835887ab404556bfaf78dcfe03ba76fa2508649dca8531c74bca4d5846513522404d48e8c4ac8b
BASE=${MAVEN_USER_HOME:-"$HOME/.m2"}/wrapper/dists/apache-maven-$VERSION
if [ ! -x "$BASE/bin/mvn" ]; then
  command -v java >/dev/null 2>&1 || { echo 'JDK 21+ is required.' >&2; exit 1; }
  TMP=$(mktemp -d)
  trap 'rm -rf "$TMP"' EXIT HUP INT TERM
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$VERSION/apache-maven-$VERSION-bin.tar.gz"
  if command -v curl >/dev/null 2>&1; then curl --fail --location --retry 3 "$URL" -o "$TMP/maven.tar.gz"
  elif command -v wget >/dev/null 2>&1; then wget -O "$TMP/maven.tar.gz" "$URL"
  else echo 'Install curl or wget, or Maven 3.9+.' >&2; exit 1; fi
  if command -v sha512sum >/dev/null 2>&1; then ACTUAL=$(sha512sum "$TMP/maven.tar.gz" | cut -d ' ' -f 1)
  elif command -v shasum >/dev/null 2>&1; then ACTUAL=$(shasum -a 512 "$TMP/maven.tar.gz" | cut -d ' ' -f 1)
  else echo 'Install sha512sum or shasum to verify Maven.' >&2; exit 1; fi
  [ "$ACTUAL" = "$SHA512" ] || { echo 'Maven checksum mismatch; refusing to run.' >&2; exit 1; }
  tar -xzf "$TMP/maven.tar.gz" -C "$TMP"
  mkdir -p "$(dirname "$BASE")"
  # Move only a fully downloaded and verified distribution into the cache.
  if [ ! -d "$BASE" ]; then mv "$TMP/apache-maven-$VERSION" "$BASE"; fi
fi
exec "$BASE/bin/mvn" "$@"
