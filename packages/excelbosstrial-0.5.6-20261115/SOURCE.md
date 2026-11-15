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
