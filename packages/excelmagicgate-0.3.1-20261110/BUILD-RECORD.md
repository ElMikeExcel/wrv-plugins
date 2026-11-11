# ExcelMagicGate 0.3.1 — Build Record

Date: 2026-11-10. This record covers the 0.3.1 build in this folder
(`~/workspace/magicgate-031-build/`), built by the board supervisor's
desk chain. Base tree: the verified live amended 0.3.0 build
(`magicgate-rank-build/build-0.3.0-amended/`, build -02), carried
whole into this NEW folder; the amended folder was not modified.
Base verification before any edit: the amended folder's
`sha256sum -c SHA256SUMS` passed on every entry (jar, records,
placement), and the copied source/stage trees diffed identical.

## Why this build exists

Mike's ruling (Nov 10, 2026, on the record): wand lore must show
(a) the player's credited cast count AND (b) clearly show how many
casts each milestone takes — Student 100 / Apprentice 400 /
Master 2,000. Parent-verified defect in the live -02 build: its
wand lore shows the credited count and the gap to the next rank,
but NOT the milestone numbers themselves.

## Artifact

- Jar: `ExcelMagicGate-0.3.1.jar` — **243885 B**, SHA-256 `454186c0dd145a57af08757d88b4de92cbd55551c5bdd71bc5677806e9bf42a2`
- Build ID: `EXC-MAGICGATE-0.3.1-WRV-20261110-01-7a0311`
  (excel-build.txt verified inside the jar; plugin.yml version 0.3.1).
- **Deterministic double-build: PASS** — second clean build
  byte-identical, same SHA-256.
- Stage packets: byte-identical to the amended 0.3.0 build (and to
  0.2.1). Placement `earned.yml`: identical. Shipped `config.yml`:
  byte-identical to the amended build (no config change).

## Diff summary — lore-only scope (the proof)

Source diff vs the amended 0.3.0 tree, file by file:

- `WandLoreHook.java` — THE ONLY behavior change:
  - new pure `milestoneLines(Thresholds)`: the milestone numbers,
    read from the same thresholds the earning logic uses (locked
    defaults render Student 100 / Apprentice 400 / Master 2,000);
  - `blockLines(...)` refactored to a pure static form (credited,
    current rank key, ceiling, thresholds) that the existing
    Player-facing method delegates to unchanged in shape, with the
    two milestone lines appended as the block's last lines;
  - `BLOCK_PREFIXES` gains the two milestone prefixes (append-seam
    orphan-strip only);
  - header comment records the 0.3.1 ruling.
- `BuildInfo.java` — version 0.3.1 + build ID rotation only.
- `plugin.yml` — `version: 0.3.1` only (description untouched).
- `excel-build.txt` — build ID + version only.
- `build.sh` — default output path/name for this folder + a
  three-line 0.3.1 header note only.
- Tests (`ExcelMagicGateTest.java`) — build-ID prefix updated to
  0.3.1-01, plus the new `testWandLore()` (8 checks, below).

NOTHING else changed: no rank logic, no thresholds, no reconcile
(rank, demotion, or inventory sync), no credit policy, no config,
no data format, no stage packets. Verified by `diff -r` over the
full src/tests/stages trees against the amended base (output
reviewed line by line at build time) and by the config/stages
byte-identity checks above.

## The exact lore lines added

```
§7Milestones: §fStudent 100 §7· §fApprentice 400
§7· §fMaster 2,000
```

(§ = the section-sign color-code character in the source strings.)
Two lines on purpose: the first rig boot of this build
(boot-031-2.log, single-line form) proved Magic wraps a 55-visible-
character lore line mid-phrase — "…· Master" / "2,000" split — and
Mike's ruling is that the numbers show CLEARLY. Each milestone
phrase (Student 100 · Apprentice 400 · Master 2,000) stays whole
in the shipped two-line form, rig-proven in boot-031-3.log.

Full block as rendered on the rig (RigSyncAppr, 500 credited):
Rank: Apprentice (ceiling: Master) / Credited casts: 500 /
Next: Master — 1,500 to go / the two milestone lines above.

## Tests

- **284 checks, 0 failures** (`run-tests.sh`, full output
  TEST-OUTPUT.txt): all 276 carried amended-0.3.0 checks (0.2.1
  regression, EarnedRank, CeilingPlan, CreditPolicy, EarnedStore,
  config agreement, §10 credit-logic simulations, placement
  reproduction EXACT over the 11 Nov 9 fixtures, WandInventorySync
  + the fixture grant pass) + **8 new lore checks**:
  - milestone lines render exactly with the locked numbers;
  - a 1,210-credited block (Mike's placement shape) contains the
    credited count, the gap ("790 to go"), and all three milestone
    numbers in one joined lore string;
  - the milestone lines are the block's last two lines;
  - beginner (12, gap 88), master (2,500, top-rank line), and
    ceiling-held blocks each still carry count + all milestones.
- Spell logic tests (spell-audit harness): NOT APPLICABLE — no
  spell config touched (stage packets byte-identical); the
  credit-logic simulations carried from 0.3.0 stand.

## Rig verdict (summary — full text in RIG-VERDICT.md)

PROVEN on the copied rig (Paper 26.2 + Magic 11.2.5, stage 4):
3 boots of the 0.3.1 jar. Boot 3 (the shipped two-line lore) drove
RigSyncAppr (500 credited): stepped beginner → apprentice, lore
property seam VERIFIED, inventory sync granted 35 spells verified
on the wand (carried behavior unchanged), and `frig lore` read the
live wand lore as the full 5-line block above — credited count,
next-rank gap, and both milestone lines, no wrap split. A Master
wand (2,500 credited, boot 2) read the same block shape with the
top-rank line. Zero SEVERE in the final boot; clean stops.

## Fingerprint (per FINGERPRINT-STANDARD.md)

See FINGERPRINTS.md. Layer A clean. Layer B build ID above.
Layer C: all carried markers + the new natural marker: the
`Milestones:` wand-lore lines. Registry row: NOT written by this
build (left to the supervisor's records leg with the MANIFEST
filing, as before).

## Scope compliance

Built and rig-proven only. NOT installed, NOT published, MANIFEST
entry NOT filed (draft staged as MANIFEST-ENTRY-DRAFT.md). No
MANIFEST, backlog, or live/server files were edited; no panel
window was opened. Install packet: INSTALL-PACKET.md (live gate =
the amended -02 jar). Records package staged under
`records/excelmagicgate-0.3.1-20261110/` in this folder.
