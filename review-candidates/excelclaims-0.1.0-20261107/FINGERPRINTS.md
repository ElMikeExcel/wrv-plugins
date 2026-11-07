# FINGERPRINTS — ExcelClaims 0.1.0

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-CLAIMS-0.1.0-WRV-20261107-01-7c1a3f`
  (SKU CLAIMS · version 0.1.0 · channel WRV · 2026-11-07 · seq 01).
- Homes: (1) `excel-build.txt` at the jar root; (2) the `BuildInfo`
  constant, assembled from string fragments in
  `src/com/winkreyes/excelclaims/BuildInfo.java`; (3) printed live by
  `/excelclaims status`, so the marker is load-bearing for support
  (the install packet's reconciliation gate reads it).
- The local build's fingerprint gate verifies homes (1) and (2) in
  the shipped jar on every build; it passed for this artifact. Seq
  01 is the first and only 0.1.0 build filed for install.

## Semantic markers (Layer C — persist version to version)

1. **C2, config phrasing:** `config.yml`'s header voice ("Our own
   land claims for Wink Reyes Valley", the HOW TO EDIT block) and
   its section commentary.
2. **C3, data dialect:** native claim files keep the old store's
   nine core keys first, then the Excel bookkeeping keys in order —
   `Inventory Only`, `Subclaim Restricted`, `Chest Claim`,
   `Created At`, `Last Modified At`; `player-extra.yml` uses the
   kebab-case dialect (`accrued-limit-override`, `last-seen-at`).
   (The exporter strips the extras — rollback files are pure —
   which is itself part of the dialect.)
3. **C4, algorithmic constants:** accrual carries the sub-block
   remainder in **sixtieths** (per-minute fraction kept exactly, no
   drift at any rate); claim columns protect from the claim's
   lesser Y to the sky.
4. **C5, idiosyncrasy:** the trust-reporting split — rights checks
   use the ladder (Manage implies all), while `getPermission`
   reports grants in Build → Container → Access → Manage order
   (the Landmarks dual-membership behavior, documented in
   BUILD-RECORD); `isAdminClaim()` is top-level-only at the facade
   while engine protection reads the top claim explicitly.
