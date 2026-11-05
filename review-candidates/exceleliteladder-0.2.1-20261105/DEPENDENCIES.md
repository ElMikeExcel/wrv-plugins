# ExcelEliteLadder 0.2.1 — dependencies

Compile-time (nothing below is bundled into the jar except our classes):

- **Paper API 26.3 build 231** (`paper-api-26.3.build.231-stable.jar`,
  from `~/workspace/magic-los-fix/cp/`) — the Bukkit/Paper API surface
  (events, scheduler, potion effects). Provided by the server at
  runtime.
- **EliteMobs 10.9.12** (`EliteMobs-10.9.12.jar`, the tree's rig copy,
  SHA-256 `3d796000f692f636651ee2e01ec27d407a043cc8fb75d08e68d488c5dee828d7`)
  — **compile-only, NEW in 0.2.1**: the elite-speed listener compiles
  against its API event classes (`EliteMobSpawnEvent`,
  `EliteMobEnterCombatEvent`, `EliteEntity`). Verified: the product jar
  contains NO `com/magmaguy` entries. At runtime EliteMobs remains a
  **softdepend**: absent EM = no listener, no errors, ladder behavior
  identical to 0.2.0.
- Transitive API libraries resolved from the local runtime set
  (`~/workspace/excelbuildmode-pilot/runtime-server/libraries`):
  adventure-api, gson, guava, bungeecord-chat, checker-qual,
  error_prone_annotations, failureaccess, jspecify, listenablefuture,
  perfmark, javax.inject, j2objc — the 0.2.0 set, unchanged.
- **SnakeYAML 2.6** (snakeyaml-2.6.jar, same source) — bundled into the
  plugin jar at build (as in 0.1.x/0.2.0; relocated-free, upstream's own
  classes) and used by the equality suite on the test classpath.

Runtime soft-dependencies (unchanged): EliteMobs 10.9.12 (configuration
target + speed events), ExcelGuildGate 0.4.0 (reflective seal-state
read — `ExcelGuildGateAPI.sealsBroken()`; no compile dependency).

Toolchain: JDK 25 (Temurin 25.0.4.1).
