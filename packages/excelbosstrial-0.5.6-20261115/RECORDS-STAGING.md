# ExcelBossTrial 0.5.6 — records package staging notes

Staged Nov 15, 2026 (worker W6, board supervisor run 390).
Published as an OPEN DRAFT record PR in wrv-plugins at
`packages/excelbosstrial-0.5.6-20261115/` — never merged.
Publication claim: wrv-plugins PR #___ (duplicate check run
2026-11-15T17:00:19Z, result NO MATCH for
'excelbosstrial-0.5.6') — PR number filled in at publish
time in the publish record, not in this file.

## Package

- File count: 236 files total on disk, including this
  RECORDS-STAGING.md, FILE-LIST.txt, and SHA256SUMS.
- SHA256SUMS: 235 entries — every file in the package
  except SHA256SUMS itself.
- Verification: `sha256sum -c SHA256SUMS` — see the
  publish record for the verified count.
- Jar: NOT included in the package (following the 0.5.5
  template exactly; no .jar in the GitHub-bound file set).
  Candidate jar of record stays in the build folder:
  ExcelBossTrial-0.5.6.jar — 471,888 B / sha256
  005bd005ff015dc7b7c1f9e271a3606e3601a9e2732cfc5ce603d8caea53449a,
  build EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56.
  INSTALLED Nov 15, 2026 (see INSTALLED.txt + OUTCOME.txt).

## Three-generation candidate chain (all of record)

| Gen | Jar | Size | SHA-256 | Record in this package |
|---|---|---|---|---|
| 1 — original build | ExcelBossTrial-0.5.6.jar | 469,839 B | 5cd9f76b729dd53567ab7d7b7ae97e70a326e6a1784f34283282793782c0409c | BUILD-RECORD.md, FINGERPRINTS.md, SOURCE.md (gen-1 section) |
| 2 — fsync rebuild (AtomicStateFile) | ExcelBossTrial-0.5.6.jar | 470,592 B | eb11a6e2f2c74436a505a7199c2f5a686fe45742f00257549d61530f712bd265 | REBUILD-RECORD-FSYNC1.md, ATOMICSTATEFILE.diff |
| 3 — fsync2 rebuild (MarkerRegistry + TeamRewardLedger) — FINAL, INSTALLED | ExcelBossTrial-0.5.6.jar | 471,888 B | 005bd005ff015dc7b7c1f9e271a3606e3601a9e2732cfc5ce603d8caea53449a | REBUILD-RECORD.md, TEST-RESULTS.md, MARKERREGISTRY.diff, TEAMREWARDLEDGER.diff |

Version and build ID are unchanged across all three
generations (0.5.6 never shipped before gen 3). The source
tree in this package is the gen-3 (fsync2) tree; its
SOURCE.md documents all three generations in-file.
FINGERPRINTS.md fingerprints the gen-1 jar only; the gen-3
jar's fingerprints are in REBUILD-RECORD.md.

## What was copied from where

Template: the 0.5.5 records package
(`packages/excelbosstrial-0.5.5-20261110/`, PR #179) —
same flattened layout: source tree at package root, plus
src/ and tests/.

- Source tree (222 files): copied verbatim from
  `~/workspace/bosstrial-056-fsync2-rebuild-20261114/bosstrial/`
  — its top-level files (CHANGELOG-0.3.5/0.3.6/0.3.7.md,
  DEPENDENCIES.md, DEPENDENCIES.sha256,
  FUNCTIONAL-CHANGE.diff, README.md, SANITIZATION.diff,
  SOURCE-PROVENANCE.json, SOURCE.md, TEST-RESULTS.txt,
  build-test.sh, config.yml, excel-build.txt, package.sh,
  plugin.yml) and its src/ and tests/ trees, flattened at
  the package root. Closing `diff -r` against the source
  tree was clean.
- Build-record chain overlays:
  - BUILD-RECORD.md + FINGERPRINTS.md + INSTALL-PACKET.md
    from `~/workspace/bosstrial-056-build/` (gen 1).
  - REBUILD-RECORD-FSYNC1.md from
    `~/workspace/bosstrial-056-fsync-rebuild-20261114/REBUILD-RECORD.md`
    (gen 2).
  - REBUILD-RECORD.md + TEST-RESULTS.md +
    ATOMICSTATEFILE.diff + MARKERREGISTRY.diff +
    TEAMREWARDLEDGER.diff from
    `~/workspace/bosstrial-056-fsync2-rebuild-20261114/`
    (gen 3).
- Install evidence: INSTALLED.txt from the fsync2 folder;
  OUTCOME.txt from its `install-20261115/` subfolder
  (failsafe install of Nov 15, 2026; placed jar pull-back
  EXACT at 471,888 B / 005bd005…).
- FILE-LIST.txt and SHA256SUMS were generated locally
  for this package.

## Tests

Full suite on the gen-3 build: 2,792 checks / 101
harnesses, 0 failures (see TEST-RESULTS.md). Gen-1 suite:
2,765 checks / 98 harnesses; gen-2 suite: 2,775 checks /
99 harnesses.

## Still owed (outside this package)

- Drive deposit of the jar / source zip / SOURCE note:
  local staging folder prepared at
  `~/workspace/bosstrial056-drive/` following the
  bosstrial055-drive pattern; the Drive upload itself is
  still owed — no proven Drive upload route is documented
  in the prep or the prior records records.
- Player-facing surfaces for 0.5.6 are staged separately
  (board-r389/bosstrial-surfaces, provisional seq 17) and
  file on their own sequence turn.
