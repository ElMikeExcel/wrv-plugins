# FINGERPRINTS — ExcelSummons 0.1.6

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a`
  (SKU SUMMONS · version 0.1.6 · channel WRV · 2026-11-12 · seq 07).
  Previous: `EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315` (seq 06),
  `EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19` (seq 05),
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
  "ExcelSummons 0.1.6 enabled (build
  EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a); registry holds N
  summon(s)." (rig-verified in `rig-0.1.6/boot.log`).
- Artifact identity: `ExcelSummons-0.1.6.jar` — 66,209 B —
  SHA-256 08da4f10a5154b28187f2b000535abb0154498b751efa19c0a3518e4e04f70f1,
  reproduced by the in-tree double-build, a clean-room rebuild,
  and a rebuild from the shipped source zip (SOURCE.md /
  BUILD-RECORD.md).

## Semantic markers (Layer C — persist version to version)

All 0.1.5 markers persist (see the 0.1.5 FINGERPRINTS in
`~/workspace/summons-015-build/FINGERPRINTS.md`): the registry
dialect (including the 0.1.4 `origin` field), the `summon_cap` /
`summon_duration_ms` convention pair, the dismissal voice, the
constants (server combat ceiling **12**, recent-fight window
**15 s**, target tick **40**, sweep tick **100**, expired-record
grace **30 s**, aggressor retaliation window **30 s**, Guardian
taunt radius **16**), the rank ladder **Beginner 1 · Student 2 ·
Apprentice 3 · Master 5** with earned-rank thresholds
**100 / 400 / 2,000**, the reserve dialect and percentages, the
0.1.4 refusal voice, and the 0.1.5 archer equipment spec
(BOW in hand at every tier; IRON_HELMET base, CHAINMAIL_HELMET
tiers 2+).

Amended by 0.1.6 (Mike's ruling, Nov 12, 2026):

- The reserve scope marker narrows by ORIGIN: only CAST-origin
  records hold mana; ADOPTED-origin records (hand-tamed / bred
  animals) hold **0% permanently**. The cast rates are unchanged
  (archer 10 · bruiser 15 · guardian 25 · revenant 20 · ghast
  25 · wolf 10 · familiar 5 · cat 5 · mount 10), as are the
  ceiling **75** and floor **20** — which from 0.1.6 bind the
  sync/re-derivation path (in registry order, skip-and-continue)
  as well as PreCast, so the applied reserve can never pass
  either number on any path.
