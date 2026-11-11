# FINGERPRINTS — ExcelSummons 0.1.4

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19`
  (SKU SUMMONS · version 0.1.4 · channel WRV · 2026-11-10 · seq 05).
  Previous: `EXC-SUMMONS-0.1.3-WRV-20261110-04-c9f312` (seq 04),
  `EXC-SUMMONS-0.1.2-WRV-20261108-03-a4d217` (seq 03).
- Homes: (1) `excel-build.txt` at the jar root; (2) the `BuildInfo`
  constant, assembled from string fragments in
  `src/com/winkreyes/excelsummons/BuildInfo.java`; (3) the header
  stamped into the data file the plugin writes
  (`plugins/ExcelSummons/summons-data.yml` carries the build line).
- The local build's fingerprint gate verifies homes (1) and (2) in
  the shipped jar on every build; it passed for this artifact.
- Startup proof: the enable line in the server log reads
  "ExcelSummons 0.1.4 enabled (build
  EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19); registry holds N
  summon(s)." (rig-verified in `rig-0.1.4/boot.log`).
- Artifact identity: `ExcelSummons-0.1.4.jar` — 63,642 B —
  SHA-256 a34d7bdf54c9017eb3e4290d2fa89c3685e0adb3c9f40eef53f959f6322cc25a,
  reproduced by the in-tree double-build, a clean-room rebuild,
  and a rebuild from the shipped source zip (SOURCE.md /
  BUILD-RECORD.md).

## Semantic markers (Layer C — persist version to version)

All 0.1.3 markers persist (see the 0.1.3 FINGERPRINTS in
`~/workspace/summons-013-build/FINGERPRINTS.md`): the registry
dialect, the `summon_cap` / `summon_duration_ms` convention pair,
the dismissal voice ("Your mount drifts home."), the constants
(server combat ceiling **12**, recent-fight window **15 s**,
target tick **40**, sweep tick **100**, expired-record grace
**30 s**, aggressor retaliation window **30 s**, Guardian taunt
radius **16**), the 0.1.3 admin audit line, and the reserve
dialect (`wrv_summon_reserve_<family>` keys, count-tiered `…2`
suffix).

Changed / added by 0.1.4:

- The per-caster combat cap **3** marker is RETIRED with the key
  it named (`per-caster-combat-cap`); its successor marker is
  the rank ladder **Beginner 1 · Student 2 · Apprentice 3 ·
  Master 5** under `summon-cap-by-rank`, with the earned-rank
  thresholds **100 / 400 / 2,000** mirrored from ExcelMagicGate.
- Reserve percentages extend **10 / 15 / 25 / 20 / 25** (combat)
  with **wolf 10 · familiar 5 · cat 5 · mount 10**; the tiered
  key dialect extends to `…_wolf5`, `…_familiar3`, `…_cat2`,
  `…_mount` (18 templates total live after install).
- The registry dialect gains one field: `origin` (`cast` /
  `adopted`; a row without the key reads adopted).
- Refusal voice, 0.1.4: "Summon limit reached — your rank
  (Rank) allows N summon(s), and you have M out: …" ·
  "Your <Spell> limit is reached — n of cap already out." ·
  "The valley is holding as many combat summons as it can right
  now (12). Try again shortly." · "<Spell> would hold p% mana,
  but your summons already hold cur% of 75% allowed. …"
- Marker added by 0.1.4, greppable as an operator act in the
  server log: the INFO line "Summons admin dismiss by
  <CONSOLE|operator> for <player> (<uuid>): dismissed N
  summon(s) (cast pets only)." for the admin pets scope.
