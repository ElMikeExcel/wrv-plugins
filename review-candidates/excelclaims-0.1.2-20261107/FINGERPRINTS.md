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

---

# FINGERPRINTS — ExcelClaims 0.1.1 (addendum)

## Build identity (Layer B)

- Build ID: `EXC-CLAIMS-0.1.1-WRV-20261107-02-5a1e9c`
  (SKU CLAIMS · version 0.1.1 · channel WRV · 2026-11-07 · seq 02,
  continuing directly from 0.1.0's seq 01).
- Homes: unchanged — (1) `excel-build.txt` at the jar root;
  (2) the `BuildInfo` constant fragments; (3) `/excelclaims status`
  prints it live. The 0.1.1 build's fingerprint gate verified homes
  (1) and (2) in the shipped jar; the identity gate re-verified the
  GriefPrevention registration.

## Semantic markers (Layer C — persist version to version)

All five 0.1.0 markers persist unchanged in 0.1.1: C2 config
phrasing (config.yml is byte-identical to 0.1.0's), C3 data
dialect (same keys, same order, in claim files and
player-extra.yml), C4 algorithmic constants (sixtieths accrual;
full-column protection), C5 idiosyncrasies (trust-reporting split;
top-level-only `isAdminClaim()`). The 0.1.1 source delta touches
only the importer's date seeding, the sweep's last-seen basis,
the enable-line version string, and version identity — no marker
was added, moved, or removed, so no replacement was required.

---

# FINGERPRINTS — ExcelClaims 0.1.2 (addendum)

## Build identity (Layer B)

- Build ID: `EXC-CLAIMS-0.1.2-WRV-20261107-03-3b8f2d`
  (SKU CLAIMS · version 0.1.2 · channel WRV · 2026-11-07 · seq 03,
  continuing directly from 0.1.1's seq 02).
- Homes: unchanged — (1) `excel-build.txt` at the jar root;
  (2) the `BuildInfo` constant fragments; (3) `/excelclaims status`
  prints it live. The 0.1.2 build's fingerprint gate verified homes
  (1) and (2) in the shipped jar; the identity gate re-verified the
  GriefPrevention registration.

## Semantic markers (Layer C — persist version to version)

C3 data dialect (same keys, same order, in claim files and
player-extra.yml), C4 algorithmic constants (sixtieths accrual;
full-column protection), and C5 idiosyncrasies (trust-reporting
split; top-level-only `isAdminClaim()`) persist unchanged in
0.1.2. C2 config phrasing carries ONE documented replacement:
the expiration section's comments were rewritten in the same
plain house voice to describe the per-player design, because
the section they described (the three global clocks) no longer
exists — Mike's ruling of Nov 7, 2026 is the named reason, and
the replacement landed in the same pass, per the standard.
Every other config section is byte-identical to 0.1.1's. No
marker was added, moved, or silently dropped.
