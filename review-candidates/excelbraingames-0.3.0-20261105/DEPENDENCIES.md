# Dependencies

ExcelBrainGames compiles against the Paper API and the Vault economy
API, both hash-pinned in build.sh (binaries are NOT included in this
record; their SHA-256 hashes are in DEPENDENCIES.sha256):

- Paper API 26.3 build 231 (`paper-api-26.3.build.231-stable.jar`) plus
  its runtime library set from the local Paper server libraries folder
  (Kyori Adventure 5.2.0 family, Gson 2.14.0, Guava 33.6.0-jre,
  SnakeYAML 2.6, slf4j-api 2.0.18, bungeecord-chat, fastutil 8.5.19,
  joml 1.10.8, jspecify 1.0.0, protobuf-java 4.29.0 and the small
  annotation jars — all pinned individually in DEPENDENCIES.sha256).
- Vault 2.0.0 (`Vault-2.0.0.jar`) — economy provider API; compile-only,
  never shipped. plugin.yml declares `depend: [Vault]`; on live the
  provider is EssentialsX Economy.

All ExcelBrainGames code is the project's own; the upstreams are
unmodified. There is no upstream plugin source in this build.

Unchanged from 0.1.0: the Phase 2 and Phase 3 build.sh files pin the
same two primary inputs (Paper API 26.3 build 231, Vault 2.0.0
`8b2ce431…`), so DEPENDENCIES.sha256 is carried over verbatim.

The 0.3.0 product jar (ExcelBrainGames-0.3.0.jar, SHA-256
acfba81f800cf67e9bdf56e9eae016ec2d6ce8c42fce7c8558e9f3e694345b63) is
deposited in Drive 'Minecraft WRV' > Build files (see
SOURCE-PROVENANCE.json). It is not included in this text record. The
superseded 0.2.0 jar (SHA-256
36e9519345088c3dd34f75e68486a2b466cf90375debd671c37cf995a3d1b6b6) was
never installed and is not deposited; it is pinned here and in
SOURCE-PROVENANCE.json for provenance only.
