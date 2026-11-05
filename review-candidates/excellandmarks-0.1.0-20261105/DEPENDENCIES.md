# Dependencies

This plugin is the project's own code (Wink Reyes Valley). It has no
upstream plugin source dependency: it is compiled against the Paper API
plus the API jars of the plugins it hooks, all hash-pinned in build.sh
and in DEPENDENCIES.sha256:

- Paper API 26.3 build 231 (`paper-api-26.3.build.231-stable.jar`,
  SHA-256 `f0df9e7f…`) plus its runtime library set from this VM's pinned
  tree (`~/workspace/excelbuildmode-pilot/runtime-server/libraries`).
- Vault 2.0.0 (`Vault-2.0.0.jar`, SHA-256 `8b2ce431…`) — the economy
  rail; every charge goes through Vault (EssentialsX on the live
  server).
- GriefPrevention 16.18.7 (`GriefPrevention-16.18.7.jar`, SHA-256
  `45e9907c…`) — PLOT subdivisions at purchase (soft hook; absent =
  naming-only mode).
- Floodgate (`floodgate.jar` from the live server, SHA-256
  `21570aff…`) — the Cumulus form renderer for Bedrock menus (soft
  hook; the form path falls back to the inventory menu).
- ExcelProtect 0.1.1 is a runtime data hook only (no compile dep): the
  lapse finalize reads its `blocks-*.jsonl` placement attribution from
  disk; absent or unreadable = contents-only redemption, fail-soft.
- ExcelMenu 2.0 is a registry integration only (no compile dep): the
  "My Landmarks" door is a data entry (`menu-entry/landmarks.yml`), and
  the spot menu renders on the same dual stack.
- Toolchain: JDK 25 (Temurin 25.0.4.1, `~/workspace/.jdk25`),
  `javac --release 21`.

Rig boots ran the real Vault + EssentialsX + GriefPrevention 16.18.7 +
ExcelProtect 0.1.1 jars (Boot A) and a GP/Protect-absent set (Boot B).
No upstream source is included; dependency binaries are NOT included in
this record (hashes in DEPENDENCIES.sha256).

The 0.1.0 product jar (ExcelLandmarks-0.1.0.jar, SHA-256
466c14c4a4002b87fb72e399411355d4df442b8d19b6fcbc58122073521246dc) is
deposited in Drive 'Minecraft WRV' > Build files (see
SOURCE-PROVENANCE.json). It is not included in this text record.
