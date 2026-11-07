# Dependencies

ExcelProtect compiles against three upstream plugin APIs, pinned as jars in the build folder's `libs/` (binaries are NOT included in this record; their SHA-256 hashes are in DEPENDENCIES.sha256):

- GriefPrevention 16.18.7 (adapter: claims)
- Vault 2.0.0 (adapter: economy)
- mcMMO (adapter: skills)

It also compiles against the Paper API (26.3, build 231) and tests against JUnit 5 (console 1.11.4), both from the local build-libs cache.

EliteMobs 10.9.12 is a **runtime soft dependency only** — nothing is linked. Damage-incident elite attribution resolves EliteMobs' `EntityTracker` API reflectively at runtime (`EliteProbe`), so Protect runs identically when EliteMobs is absent.

All ExcelProtect code is the project's own; the upstreams are unmodified.

The base this build succeeds is ExcelProtect 0.1.3 (165,366 B, SHA-256 f0ebd67e3d8794def0e83e32def3eb86ff5693d976c11ccc5bf003a650bb9177), recorded in the 0.1.3 record package; 0.1.3 itself succeeds the LIVE 0.1.2 product jar (138,039 B, SHA-256 26b7f5ba1b01e37843647244e025da30ec51db6019395c4bfbc38f9428863393), deposited in Drive 'Minecraft WRV' > Build files at its publication. Neither jar binary is included in this text record.

## 0.1.6 addition (Nov 6, 2026)

- **WorldEdit API (compile-only, softdepend):** worldedit-core
  7.2.7 + worldedit-bukkit 7.2.0-SNAPSHOT from the local Maven repo
  (`~/.m2/repository/com/sk89q/worldedit/…`), appended to the compile
  classpath by local-build.sh when present. No WorldEdit code is
  bundled into the jar; at runtime the hook loads only when the
  WorldEdit plugin is present (WRV runs 7.4.6; the used API surface —
  EditSessionEvent, extent delegate, BukkitAdapter — is stable
  across 7.2–7.4 and was rig-verified against the genuine 7.4.6 jar).
