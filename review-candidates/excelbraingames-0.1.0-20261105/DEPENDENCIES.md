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

The 0.1.0 product jar (ExcelBrainGames-0.1.0.jar, SHA-256
b23f916fe9f15f58f28f991ada9b13f5a758b609f212eb623c9b87f2ab5e86ef) is
deposited in Drive 'Minecraft WRV' > Build files (see
SOURCE-PROVENANCE.json). It is not included in this text record.
