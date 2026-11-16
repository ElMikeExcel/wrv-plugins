# ExcelBossTrial 0.5.7.1 — records package staging notes

Staged Nov 16, 2026 (worker W5, board supervisor run 430).
Published as an OPEN DRAFT record PR in wrv-plugins at
`packages/excelbosstrial-0.5.7.1-20261116/` — never merged.
Publication claim: wrv-plugins PR #___ (duplicate check run
2026-11-16T16:24:32Z, result NO MATCH for 'excelbosstrial-0.5.7.1';
also NO MATCH on 'bosstrial-0.5.7.1', 'bosstrial-0571',
'excelbosstrial-0571' in the same sweep) — PR number filled in at
publish time in the publish record, not in this file.
PR #242 (the 0.5.7 record package) stays UNTOUCHED.

## Package

- File count: 231 files total on disk, including this
  RECORDS-STAGING.md and SHA256SUMS.
- SHA256SUMS: 230 entries — every file in the package
  except SHA256SUMS itself.
- Verification: `sha256sum -c SHA256SUMS` — see the
  publish record for the verified counts (local + Homebase).
- Jar: NOT included in the package. Candidate jar of record
  stays in the build folder:
  ExcelBossTrial-0.5.7.1.jar — 472,617 B / sha256
  94cf076da9cbb489fa4ded38cd9b7599b4d72e36724f88017e0540ef7b01f899,
  build EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6.
  INSTALLED Nov 16, 2026 (see INSTALLED.txt + OUTCOME.txt).

## What 0.5.7.1 is

BuildInfo stamp fix only, on top of the 0.5.7 tree. Exactly
6 paths changed vs 0.5.7, ONE production source file:
BuildInfo.java — PART_B "0.5.6-WRV-" -> "0.5.7.1-WRV-",
PART_C "20261111-01-3c9e56" -> "20261116-01-471aa6",
VERSION "0.5.6" -> "0.5.7.1". Companion paths: plugin.yml
(version + description sentence), excel-build.txt (build-id /
version / build-date 2026-11-16), PluginYmlParseTest.java
(version pin + sentence check), SOURCE.md (appended 0.5.7.1
section, in-tree — no splice owed), and the NEW prevention
harness BuildInfoStampTest.java (8 checks: VERSION ==
plugin.yml version; BUILD_ID == excel-build.txt build-id;
excel-build.txt version == plugin.yml version; BUILD_ID
prefix agreement). No config, data, or state-format change;
no gameplay behaviour change from 0.5.7; Bedrock unaffected.

- Base gate: unmodified 0.5.7 tree rebuilt byte-identical to
  the installed 0.5.7 candidate (472,525 B /
  4b737c584dae9d9d6ebcffe85dc0899fbc6918d06f7c3818b9e8a5bd9bfc1e01;
  suite 2,856 checks / 102 harnesses, 0 failures).
- Tests: full suite 2,865 checks / 103 harnesses, 0 failures.
  Deterministic: package.sh run twice, both jars byte-identical
  at 94cf076d…f899.
- Install: jar swap only, in place at the retained live filename
  plugins/ExcelBossTrial-0.4.1.jar, Nov 16, 2026 by
  wrv-install-failsafe (09:33 CST window, Homebase lane, panel
  backup/restart, no fallback); placed jar pull-back EXACT at
  472,617 B / 94cf076d…. Boot: Done (24.175s), Build:
  EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6 x1, stale 0.5.6
  ID x0, Enabling ExcelBossTrial v0.5.7.1 x1, SEVERE 0.
  Per packet, the passing boot verify IS the proof: 0.5.7.1 is
  Installed AND Proven (no player-facing proof owed).

## What was copied from where

Template: the 0.5.7 records package
(`packages/excelbosstrial-0.5.7-20261116/`, PR #242) —
same flattened layout: source tree at package root, plus
src/ and tests/.

- Source tree (224 files): copied verbatim from
  `~/workspace/board-r428/bb18-stamp-build/work/bosstrial-0571/`
  — its top-level files and its src/ and tests/ trees,
  flattened at the package root. Its SOURCE.md carries the
  0.5.7.1 section in-tree (9,766 B / sha256
  ec1299d19e70c6ff081a788324988578fb655cf34aea748ac68d97ceb210bbfd).
  Closing `diff -r` against the source tree was clean.
- Build record: REPORT.md (= BUILD-RECORD.md) +
  BuildInfoStamp-0.5.7.1.diff + RIG-EVIDENCE.txt from
  `~/workspace/board-r428/bb18-stamp-build/`.
- Install evidence: INSTALLED.txt from
  `~/workspace/board-r428/bb18-stamp-filing/`; OUTCOME.txt from
  `~/workspace/board-r428/bb18-stamp-filing/window-20261116-0933/`
  (failsafe install of Nov 16, 2026; placed jar pull-back
  EXACT at 472,617 B / 94cf076d…).
- SHA256SUMS was generated locally for this package.
