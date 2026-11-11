# ExcelBossTrial 0.5.5 — records package staging notes

Staged Nov 10, 2026 (desk lane, board supervisor run 157 dispatch).
Desk only: no panel, no MANIFEST edits, no GitHub/Drive sends, no installs
were performed by this lane.

## Package

- Path: `~/workspace/records-catchup-20261103/packages/excelbosstrial-0.5.5-20261110/`
- File count: 216 files total on disk, including this STAGING-NOTES.md,
  FILE-LIST.txt, and SHA256SUMS.
- SHA256SUMS: 215 entries — every file in the package except SHA256SUMS
  itself (a file cannot hash itself).
- Verification: `sha256sum -c SHA256SUMS` — 215/215 OK.
- Jar: NOT included in the package (following the 0.5.4 template exactly;
  no .jar in the GitHub-bound file set). Candidate jar of record stays in
  the build folder: ExcelBossTrial-0.5.5.jar — 455,307 B / sha256
  eade52aa49ab6eb46acc09dd5deed1c23b601be0b1aa5092f947f3cb89aaf0cb,
  build EXC-BOSSTRIAL-0.5.5-WRV-20261110-01-887e6f.

## What was copied from where

Template: `~/workspace/records-catchup-20261103/packages/excelbosstrial-0.5.4-20261110/`
(same flattened layout: source tree at package root, plus src/ and tests/).

- Source tree (209 files): copied from
  `~/workspace/bosstrial-055-build/bosstrial/` — its top-level files
  (CHANGELOG-0.3.5/0.3.6/0.3.7.md, DEPENDENCIES.md, DEPENDENCIES.sha256,
  FUNCTIONAL-CHANGE.diff, README.md, SANITIZATION.diff,
  SOURCE-PROVENANCE.json, build-test.sh, config.yml, excel-build.txt,
  package.sh, plugin.yml) and its src/ and tests/ trees, flattened at the
  package root as in the template.
- Build-folder overlays (from `~/workspace/bosstrial-055-build/`):
  BUILD-RECORD.md, INSTALL-PACKET.md, FINGERPRINTS.md, INSTALLED.txt
  (new top-level files), plus SOURCE.md and TEST-RESULTS.txt, which
  overwrite the stale 0.5.4 copies carried inside the bosstrial/ tree
  (verified identical to the build-folder copies after staging).
- INSTALLED.txt records the install of record: installed Nov 10, 2026 by
  the wrv-install-failsafe restart leg 16:33–16:46 CST, boot Done 20.124s,
  First-break layer ON, guild LOCKED 4 of 5, SEVERE 0.
- FILE-LIST.txt and SHA256SUMS were generated locally for this package.

## Still owed to the supervisor chain

- Publication legs are NOT done by this desk staging:
  - GitHub draft record PR in wrv-plugins (package at
    `packages/excelbosstrial-0.5.5-20261110/`, left OPEN as a draft,
    never merged), and
  - Drive deposit of the jar / source zip / SOURCE.md into the shared
    Drive "Minecraft WRV" folder.
- Both legs remain owed to the supervisor chain.
