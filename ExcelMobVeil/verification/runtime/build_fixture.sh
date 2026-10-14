#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
JDK="${JAVA_HOME:?Set JAVA_HOME to the verified Java 25 installation}/bin"
VAULT="${VAULT_API_JAR:?Set VAULT_API_JAR to the verified Vault API 1.7 JAR}"
CP="${ANNOTATIONS_JAR:?Set ANNOTATIONS_JAR to the verified annotations 26.0.2 JAR}:server/versions/26.2/paper-26.2.jar:$VAULT:$(find server/libraries -name '*.jar' -printf '%p:' | sed 's/:$//')"
mkdir -p classes/vault classes/probe server/plugins
"$JDK/javac" -cp "$CP" -d classes/vault src/verification/FixtureVault.java
(cd classes/vault && "$JDK/jar" xf "$VAULT")
printf 'name: Vault\nversion: 0.0.0-fixture\nmain: verification.FixtureVault\napi-version: "1.21"\n' > classes/vault/plugin.yml
"$JDK/jar" cf server/plugins/FixtureVault.jar -C classes/vault .
"$JDK/javac" -cp "classes/vault:$CP" -d classes/probe src/verification/MobVeilProbe.java
printf 'name: MobVeilProbe\nversion: 0.0.0-fresh-fixture\nmain: verification.MobVeilProbe\napi-version: "1.21"\ndepend: [Vault]\nsoftdepend: [ExcelMobVeil, ExcelDeathProtection]\n' > classes/probe/plugin.yml
"$JDK/jar" cf server/plugins/MobVeilProbe.jar -C classes/probe .
