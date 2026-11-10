# FINGERPRINTS — ExcelAutoSort 0.2.1

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331`
  (SKU AUTOSORT · version 0.2.1 · channel WRV · 2026-11-09 · seq 01;
  tag = first 6 hex of SHA-256 of the ID stem).
- Homes: (1) `excel-build.txt` at the jar root; (2) the `BuildInfo`
  constant, assembled from string fragments in
  `src/com/winkreyes/excelautosort/BuildInfo.java`; (3) the header
  stamped into every data file the plugin writes
  (`plugins/ExcelAutoSort/data/*.yml` carry the build line —
  verified live in the 0.2.1 rig: the phase-6 `maxorg.yml` header
  reads build `EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331`).
- The local build's fingerprint gate verifies homes (1) and (2) in
  the shipped jar on every build; it passed for this artifact.
- Prior build: `EXC-AUTOSORT-0.2.0-WRV-20261107-01-78a48a`.

## Semantic markers (Layer C — persist version to version)

All eight 0.2.0 markers carry forward unchanged (config voice and
header, the `world:x:y:z` key dialect and `owner-name` /
`suspend-reason` field dialect, "Recovery box" naming, buffer cap
**162** / hourly sweep / selector page size **28** / reach **6**,
the workflow-order `/autosort` help, the compass-tap settings
entry, the MaxOrg constants rent **1000** / **30** days / buy
**15000** / radius **16** / page size **45**, the "Renting never
counts toward buying" sentence, the `rent-until` / `owned` field
dialect, the three MaxOrg Protect stream types with the
`via: network-view` cause tag, and the network view's slot-**47**
smart-deposit layout signature). 0.2.1 adds:

9. **C4/C6, the admin-correction dialect:** the Protect stream
   types `autosort_maxorg_admin_grant` /
   `autosort_maxorg_admin_revoke` with the `via: admin` cause tag
   and the before/after field pair dialect (`owned_before` /
   `rent_until_before_ms` → `owned_after` /
   `rent_until_after_ms`) — a staff correction is greppable as a
   correction, never dressed as a purchase; and the console report
   voice "nothing held (no Max Organization entitlement)" for a
   no-op revoke.
