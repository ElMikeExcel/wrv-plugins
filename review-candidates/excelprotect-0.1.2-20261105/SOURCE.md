# ExcelProtect 0.1.2 — SOURCE

Ownership split (house rule): the plugin code in `src/` (all 0.1.0–0.1.2
classes, including the four new 0.1.2 files — `DamageIncidents.java`,
`DamageWatch.java`, `EliteProbe.java`, `DamageReport.java`) is **ours**
(Mike's / written for Wink Reyes Valley). Everything else is upstream
dependency surface used at build time only:

| Dependency | Version | Role | Source |
|---|---|---|---|
| Paper API | 26.3 build 231 | compile API | PaperMC (local build-libs cache) |
| GriefPrevention | 16.18.7 | compile API (claims) | upstream jar, `libs/GriefPrevention-16.18.7.jar` |
| Vault | 2.0.0 (VaultAPI) | compile API (economy) | upstream jar, `libs/vault-2.0.0.jar` |
| mcMMO | (build-libs copy) | compile API (skills) | upstream jar, build-libs cache |
| JUnit 5 console | 1.11.3 | tests only | upstream |
| EliteMobs | 10.9.12 | **runtime softdepend only — NOT linked.** Attribution is reflective (`EliteProbe`), resolved at runtime from the live plugin when present | MagmaGuy — mechanism verified via `javap` against the real jar at `~/workspace/elitemobs-10.9.12/EliteMobs-10.9.12.jar` |

Base: ExcelProtect 0.1.1 record package
(`~/workspace/records-catchup-20261103/packages/excelprotect-0.1.1-20261104/`,
itself the deposit of the Nov 4, 2026 live install). Dependency jar hashes:
`DEPENDENCIES.sha256` in this folder. No credentials, tokens, or server
addresses appear anywhere in this package.
