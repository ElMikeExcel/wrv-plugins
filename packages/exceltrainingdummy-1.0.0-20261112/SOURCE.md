# ExcelTrainingDummy 1.0.0 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 12, 2026.
Design of record: `~/workspace/training-dummy-20261110/DESIGN.md`
(Mike's direction Nov 10, 2026; copied into the source tree as
DESIGN.md).

## Provenance

- Project tree: `~/workspace/training-dummy-build-20261112/build-1.0.0/ExcelTrainingDummy/`
  (plugin.yml version 1.0.0, main
  com.winkreyes.exceltrainingdummy.ExcelTrainingDummy, api-version '1.21').
- New plugin — no base version. House patterns copied from the
  ExcelTour 1.0.0 tree (`~/workspace/exceltour-build-20261111/`):
  the deterministic build recipe (build.sh), PlatformDetect and
  BedrockForms (reflection-only Geyser/Floodgate, chat fallback),
  the menu-holder shape, the rig pattern (Paper 26.3 tree + NMS
  fake-player driver), and the fingerprint scheme
  (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`).
- Claim facts come from the ExcelClaims fork's GriefPrevention API
  surface, verified against its source in
  `~/workspace/excelclaims-build/src/me/ryanhamshire/GriefPrevention/`
  (DataStore.getClaimAt, Claim.getOwnerID / isAdminClaim / parent) —
  reflection only, no compile-time dependency.
- ExcelDetails 0.2.2's source was read (details-meter build tree)
  to check for a consumable segment API: none exists; the report
  is built from this plugin's own tracking (BUILD-RECORD §2.1).
- Ownership: all code is ours (Iris / Mike lineage, Excel
  signature). No new dependency of any kind; softdepend only
  (floodgate, Geyser-Spigot, ExcelMenu, ExcelBedrockMenu,
  GriefPrevention) so the plugin enables anywhere.

## Artifacts

- ExcelTrainingDummy-1.0.0.jar — 49,376 bytes
  SHA-256 b46250ed7a408a447749b8f62e10c432c235c9050a1e4dcff06a8138c516c098
  Build EXC-TDUMMY-1.0.0-WRV-20261112-01-178829 (fingerprint gate PASS;
  in-tree double-build byte-identical).
- ExcelTrainingDummy-1.0.0-source.zip — 37,537 bytes
  SHA-256 85490d4aff4b333636960ec0add79a1e81bf5ec3883210514b18270b6813b829
  (src/ + tests/ + plugin.yml + config.yml + build.sh + DESIGN.md;
  zip entries timestamp-pinned 2026-11-12.)
- Packet companions: config.yml ships in the jar (saveDefaultConfig
  on first enable). The compass-menu entries for ExcelMenu +
  ExcelBedrockMenu are install-packet surfaces (draft MANIFEST
  entry), not files in this build.

## Verification summary

DummyTest 67/67 (Bukkit-free core). Desk rig on Paper 26.3-235
(rig-1.0.0 in the build folder): clean enable with Geyser +
floodgate co-loaded, SEVERE 0, 11/11 behavior checks (spawn via
real command, PDC stamp, virtual-pool intake 295.2 for a 300 hit
with the entity's real health unmoved, finish → report + removal,
3-cluster, remove-all, console version). One first-boot intake
anomaly (329.2) did not reproduce across two instrumented runs —
see RIG-RESULTS.md. NOT installed — built + staged only.
