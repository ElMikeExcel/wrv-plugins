# Dependencies

ExcelProtect compiles against three upstream plugin APIs, pinned as jars in the build folder's `libs/` (binaries are NOT included in this record; their SHA-256 hashes are in DEPENDENCIES.sha256):

- GriefPrevention 16.18.7 (adapter: claims)
- Vault 2.0.0 (adapter: economy)
- mcMMO (adapter: skills)

It also compiles against the Paper API (26.3, build 231) and tests against JUnit 5 (console 1.11.3), both from the local build-libs cache.

EliteMobs 10.9.12 is a **runtime soft dependency only** — nothing is linked. Damage-incident elite attribution resolves EliteMobs' `EntityTracker` API reflectively at runtime (`EliteProbe`), so Protect runs identically when EliteMobs is absent.

All ExcelProtect code is the project's own; the upstreams are unmodified.

The base jar this build succeeds (ExcelProtect-0.1.1.jar, 122,081 B, SHA-256 b24c0ef2b16eae8cc381a721ce2d4b252904dd0dcecbe40a8ab5f80130d1312b) is recorded in the 0.1.1 record package / Drive deposits; the 0.1.2 product jar (138,039 B, SHA-256 26b7f5ba1b01e37843647244e025da30ec51db6019395c4bfbc38f9428863393) is deposited in Drive 'Minecraft WRV' > Build files at publication. Neither jar binary is included in this text record.
