# FINGERPRINTS — ExcelSummons 0.1.5

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315`
  (SKU SUMMONS · version 0.1.5 · channel WRV · 2026-11-11 · seq 06).
  Previous: `EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19` (seq 05),
  `EXC-SUMMONS-0.1.3-WRV-20261110-04-c9f312` (seq 04),
  `EXC-SUMMONS-0.1.2-WRV-20261108-03-a4d217` (seq 03).
- Homes: (1) `excel-build.txt` at the jar root; (2) the `BuildInfo`
  constant, assembled from string fragments in
  `src/com/winkreyes/excelsummons/BuildInfo.java`; (3) the header
  stamped into the data file the plugin writes
  (`plugins/ExcelSummons/summons-data.yml` carries the build line).
- The local build's fingerprint gate verifies homes (1) and (2) in
  the shipped jar on every build; it passed for this artifact.
- Startup proof: the enable line in the server log reads
  "ExcelSummons 0.1.5 enabled (build
  EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315); registry holds N
  summon(s)." (rig-verified in `rig-0.1.5/boot.log`).
- Artifact identity: `ExcelSummons-0.1.5.jar` — 65,253 B —
  SHA-256 5cdf16a0b4178adc7e264320996f6d7f350cb14343dfdc59ae965a0bd619f61e,
  reproduced by the in-tree double-build, a clean-room rebuild,
  and a rebuild from the shipped source zip (SOURCE.md /
  BUILD-RECORD.md).

## Semantic markers (Layer C — persist version to version)

All 0.1.4 markers persist (see the 0.1.4 FINGERPRINTS in
`~/workspace/summons-014-build/FINGERPRINTS.md`): the registry
dialect (including the 0.1.4 `origin` field), the `summon_cap` /
`summon_duration_ms` convention pair, the dismissal voice, the
constants (server combat ceiling **12**, recent-fight window
**15 s**, target tick **40**, sweep tick **100**, expired-record
grace **30 s**, aggressor retaliation window **30 s**, Guardian
taunt radius **16**), the rank ladder **Beginner 1 · Student 2 ·
Apprentice 3 · Master 5** with earned-rank thresholds
**100 / 400 / 2,000**, the reserve dialect and percentages, and
the 0.1.4 refusal voice.

Added by 0.1.5:

- The archer equipment spec, greppable in `ArcherEquipment`:
  hand **BOW** at every tier; helmet **IRON_HELMET** at the base
  tier, **CHAINMAIL_HELMET** at tiers 2+ — the same values the
  archer spell definitions carry in `entity_parameters`, now
  guaranteed bridge-side.
- Marker lines added by 0.1.5, greppable as soft-fail events in
  the server log (INFO, never an error): "ExcelSummons: archer
  equipment skipped (<cause>); the summon fights on regardless."
  and "ExcelSummons: archer equipment for <key> did not verify
  on read-back; the summon fights on regardless." A healthy
  install logs NEITHER (rig: 0 occurrences).
