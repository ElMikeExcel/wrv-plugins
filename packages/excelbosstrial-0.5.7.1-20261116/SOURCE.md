# SOURCE — ExcelBossTrial 0.5.6 (cash taper + seal cycle + per-player first-break extras)

- **Artifact:** `ExcelBossTrial-0.5.6.jar` — 469,839 B / sha256
  `5cd9f76b729dd53567ab7d7b7ae97e70a326e6a1784f34283282793782c0409c`
- **Build ID:** `EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56`
- **Source tree:** `bosstrial/` in the build folder (the full
  portable source-record tree: src, tests, config.yml,
  plugin.yml, package.sh, build-test.sh).
- **Base:** the shipped ExcelBossTrial 0.5.5 tree
  (`~/workspace/bosstrial-055-build/bosstrial/`), first proven
  to rebuild byte-identical to the shipped 0.5.5 jar
  (455,307 B / sha256
  `eade52aa49ab6eb46acc09dd5deed1c23b601be0b1aa5092f947f3cb89aaf0cb`)
  before any edit. (`~/workspace/excelbosstrial/` tops out at
  the 0.2.x lineage and is NOT the 0.5.5 base.)
- **Change vs base:** see `BUILD-RECORD.md` in the build
  folder. New pure cores `CashTaper`, `SealCycle`,
  `FirstBreakBook`, `CounterSeeding`; `MonumentTracker`
  gains counted-elder timestamps + partial-trio expiry; the
  host prices every cash path by the taper, marks per-player
  first breaks before payment, re-keys its books at a cycle
  boundary, and enforces elder expiry at boot, in maintenance,
  and before counting a new elder.
- **Install packet:** `INSTALL-PACKET.md` in the build folder
  (DRAFT — release after the Warden attempt, Sat Nov 14, 2026;
  the board supervisor files it as HELD).
- **Ownership:** Wink Reyes Valley Excel plugin (Mike Reyes /
  Iris); WRV channel build, never distributed.

## FSYNC REBUILD (Nov 14, 2026 — this tree)

This copy of the tree carries ONE further change on top of
the 0.5.6 build above: the Windows-safe directory-force fix
in `core/AtomicStateFile.java` (backlog P-FSYNC-1), plus its
new harness `tests/.../core/AtomicStateFileTest.java`. The
rebuilt candidate is `ExcelBossTrial-0.5.6.jar` — 470,592 B /
sha256
`eb11a6e2f2c74436a505a7199c2f5a686fe45742f00257549d61530f712bd265`
— superseding the jar named at the top of this file, which
remains the candidate of record in the ORIGINAL build folder
(`~/workspace/bosstrial-056-build/`, untouched). Version and
build ID are unchanged (0.5.6 never shipped). Full record:
`../REBUILD-RECORD.md` in the rebuild folder
(`~/workspace/bosstrial-056-fsync-rebuild-20261114/`).

## FSYNC FOLLOW-UP REBUILD (Nov 14, 2026 — this tree)

This copy of the tree carries ONE further change on top of
the FSYNC REBUILD above: the same Windows-safe
directory-force fix applied to the two remaining sites the
first fsync lane flagged — `teamrewards/marker/MarkerRegistry.java`
(`mutate()`) and `wrv/rewards/TeamRewardLedger.java`
(`persist()`) — each behind its own package-private
`DirectoryForce` seam in the AtomicStateFile shape, plus
two new harnesses
(`tests/.../marker/MarkerRegistryDirectoryForceTest.java`,
`tests/wrv/rewards/TeamRewardLedgerDirectoryForceTest.java`).
The rebuilt candidate is `ExcelBossTrial-0.5.6.jar` —
471,888 B / sha256
`005bd005ff015dc7b7c1f9e271a3606e3601a9e2732cfc5ce603d8caea53449a`
— superseding `eb11a6e2…` (470,592 B), which remains the
candidate of record in the FIRST fsync rebuild folder
(`~/workspace/bosstrial-056-fsync-rebuild-20261114/`,
untouched). Version and build ID are unchanged (0.5.6
never shipped). Full record: `../REBUILD-RECORD.md` in
the follow-up rebuild folder
(`~/workspace/bosstrial-056-fsync2-rebuild-20261114/`).


## 0.5.7 (Nov 16, 2026 - this tree)

This copy of the tree carries ONE functional change on top
of the fsync2 0.5.6 tree above: the claims-chest Back
control (audit row BB-18). The rebuilt candidate is
`ExcelBossTrial-0.5.7.jar` - 472,525 B / sha256
`4b737c584dae9d9d6ebcffe85dc0899fbc6918d06f7c3818b9e8a5bd9bfc1e01`.

- **Artifact:** `ExcelBossTrial-0.5.7.jar` - 472,525 B /
  sha256
  `4b737c584dae9d9d6ebcffe85dc0899fbc6918d06f7c3818b9e8a5bd9bfc1e01`
  (deterministic: `package.sh` run twice on the fixed tree,
  both jars byte-identical).
- **Build ID:** `EXC-BOSSTRIAL-0.5.7-WRV-20261116-01-bb18`,
  version 0.5.7, build-date 2026-11-16.
- **Base:** the fsync2 ExcelBossTrial 0.5.6 tree
  (`~/workspace/bosstrial-056-fsync2-rebuild-20261114/bosstrial/`),
  first proven to rebuild byte-identical to the installed
  0.5.6 candidate (471,888 B / sha256
  `005bd005ff015dc7b7c1f9e271a3606e3601a9e2732cfc5ce603d8caea53449a`;
  base suite 2,792 checks / 101 harnesses, 0 failures)
  before any edit.
- **Change vs base:** exactly 5 paths; ONE production
  source file changed. `PersonalClaimsUi.java` (166 to 213
  lines): the `/bossrewards` claims chest ("Your earned
  rewards", 54 slots) gains the fleet-standard Back control
  (BB-14 BackNav contract) - `BACK_SLOT = 48`, an ARROW
  labelled "Back to Server Menu" (section-sign colour codes
  in the built jar) with lore "Return to the Server Menu",
  rendered between Previous page (45) and Refresh (49);
  Next page stays at 53 and claim entries still fill slots
  0-44 only. A Back tap schedules next tick, closes the
  chest (`closeInventory`) and runs `performCommand("menu")`
  (the Server Menu); on failure the player is sent "The
  Server Menu could not open." Companion paths:
  `plugin.yml` (version 0.5.6 to 0.5.7 + the 0.5.7
  description sentence), `excel-build.txt` (version /
  build-id / build-date bump), `PluginYmlParseTest.java`
  (version pin + one description-sentence check), and the
  new harness `PersonalClaimsUiBackNavTest.java` (63
  checks). No config, data, or state-format change;
  Bedrock is served the same chest UI. Full diff:
  `PersonalClaimsUi-0.5.7.diff` in the build folder
  (`~/workspace/board-r425/bb18-fix-build/`).
- **Tests:** full suite 2,856 checks / 102 harnesses,
  0 failures (base 2,792 / 101; delta = the new 63-check
  `PersonalClaimsUiBackNavTest` + 1 `PluginYmlParseTest`
  sentence check). Bytecode/strings verification of the
  built jar pins BACK_SLOT = 48 and the fleet label, lore,
  command, and failure line (see `RIG-EVIDENCE.txt` in the
  build folder).
- **Install:** jar swap only, in place at the live filename
  `plugins/ExcelBossTrial-0.4.1.jar` (filename retained,
  content becomes 0.5.7); rollback = restore the pre-0.5.7
  backup jar at the same filename and restart. Install
  packet: `~/workspace/board-r426/bb18-filing-final/INSTALL-PACKET.md`.
- **Ownership:** unchanged - Wink Reyes Valley Excel
  plugin (Mike Reyes / Iris); WRV channel build, never
  distributed.


## 0.5.7.1 (Nov 16, 2026 - this tree)

This copy of the tree carries ONE correction on top of the
0.5.7 tree above: the BuildInfo build stamp. The shipped
0.5.7 jar carried the stale 0.5.6 BuildInfo stamp (its boot
Build line read EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56)
because that build updated plugin.yml and excel-build.txt
only. 0.5.7 is a fresh version (0.5.7 already shipped and is
of record in PR #242 / Drive / live; it is never reissued).
The rebuilt candidate is `ExcelBossTrial-0.5.7.1.jar` -
472,617 B / sha256
`94cf076da9cbb489fa4ded38cd9b7599b4d72e36724f88017e0540ef7b01f899`
(deterministic: `package.sh` run twice on the fixed tree,
both jars byte-identical).

- **Artifact:** `ExcelBossTrial-0.5.7.1.jar` - 472,617 B /
  sha256
  `94cf076da9cbb489fa4ded38cd9b7599b4d72e36724f88017e0540ef7b01f899`
- **Build ID:** `EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6`,
  version 0.5.7.1, build-date 2026-11-16. Fresh tag 471aa6 -
  neither 0.5.7's bb18 nor 0.5.6's 3c9e56 is reused.
- **Base:** the 0.5.7 tree of record
  (`~/workspace/board-r425/bb18-fix-build/work/bosstrial-057/`),
  first proven to rebuild byte-identical to the installed
  0.5.7 candidate (472,525 B / sha256
  `4b737c584dae9d9d6ebcffe85dc0899fbc6918d06f7c3818b9e8a5bd9bfc1e01`;
  base suite 2,856 checks / 102 harnesses, 0 failures)
  before any edit.
- **Change vs base:** exactly 6 paths; ONE production source
  file changed. `core/BuildInfo.java` (PART_B, PART_C,
  VERSION and the javadoc build-ID paragraph re-stamped to
  0.5.7.1). Companion paths: `plugin.yml` (version 0.5.7.1 +
  the 0.5.7.1 description sentence), `excel-build.txt`
  (version / build-id bump), `PluginYmlParseTest.java`
  (version pin + one description-sentence check), the NEW
  harness `core/BuildInfoStampTest.java` (the three-home
  stamp guard - BuildInfo.VERSION == plugin.yml version,
  BuildInfo.BUILD_ID == excel-build.txt build-id, prefix
  agreement; it fails on the pre-edit tree, 4 passed /
  4 failed, and passes after, 8 passed / 0 failed), and this
  SOURCE.md section. No config, data, or state-format
  change; no gameplay behaviour changes from 0.5.7; Bedrock
  unaffected (no UI touched).
- **Tests:** full suite 2,865 checks / 103 harnesses,
  0 failures (base 2,856 / 102; delta = the new 8-check
  `BuildInfoStampTest` + 1 `PluginYmlParseTest` sentence
  check). Jar-entry diff vs the 0.5.7 candidate:
  content-changed entries are `BuildInfo.class`,
  `ExcelBossTrial.class` (its only changed string is the
  inlined `Build: ` + BUILD_ID constant), `plugin.yml` and
  `excel-build.txt` - nothing else; 244 entries both jars.
  Desk rig boot on the Homebase test-server mirror: boot
  line `Build: EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6`
  (count 1), old ID count 0, `Enabling ExcelBossTrial
  v0.5.7.1` count 1, SEVERE 0; mirror restored after.
- **Install:** jar swap only, in place at the live filename
  `plugins/ExcelBossTrial-0.4.1.jar` (filename retained,
  content becomes 0.5.7.1); rollback = restore the pre-swap
  backup jar at the same filename and restart.
- **What's New:** checked - no entry (supervisor decision,
  run 428): a cosmetic boot-log stamp correction with no
  player-facing behaviour change needs no player entry.
- **Ownership:** unchanged - Wink Reyes Valley Excel
  plugin (Mike Reyes / Iris); WRV channel build, never
  distributed.
