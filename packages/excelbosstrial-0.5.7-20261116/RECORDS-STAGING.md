# ExcelBossTrial 0.5.7 — records package staging notes

Staged Nov 16, 2026 (worker W10, board supervisor run 427).
Published as an OPEN DRAFT record PR in wrv-plugins at
`packages/excelbosstrial-0.5.7-20261116/` — never merged.
Publication claim: wrv-plugins PR #___ (duplicate check run
2026-11-16T14:17:10Z, result NO MATCH for 'bosstrial-0.5.7';
also NO MATCH on 'excelbosstrial-057', 'boss-trial-057',
'bosstrial-057' in the same sweep) — PR number filled in at
publish time in the publish record, not in this file.

## Package

- File count: 230 files total on disk, including this
  RECORDS-STAGING.md and SHA256SUMS.
- SHA256SUMS: 229 entries — every file in the package
  except SHA256SUMS itself.
- Verification: `sha256sum -c SHA256SUMS` — see the
  publish record for the verified counts (local + Homebase).
- Jar: NOT included in the package (following the 0.5.6
  template, PR #228; no .jar in the GitHub-bound file set).
  Candidate jar of record stays in the build folder:
  ExcelBossTrial-0.5.7.jar — 472,525 B / sha256
  4b737c584dae9d9d6ebcffe85dc0899fbc6918d06f7c3818b9e8a5bd9bfc1e01,
  build EXC-BOSSTRIAL-0.5.7-WRV-20261116-01-bb18.
  INSTALLED Nov 16, 2026 (see INSTALLED.txt + OUTCOME.txt).

## What 0.5.7 is

One functional change on top of the fsync2 0.5.6 tree
(audit row BB-18): the /bossrewards claims chest ("Your
earned rewards", 54 slots) gains the fleet-standard Back
control (BB-14 BackNav contract) — BACK_SLOT = 48, ARROW
labelled "Back to Server Menu", lore "Return to the
Server Menu", action performCommand("menu"), failure line
"The Server Menu could not open." Exactly 5 paths changed
vs base; ONE production source file changed:
PersonalClaimsUi.java (166 to 213 lines). Companion paths:
plugin.yml (version + description sentence), excel-build.txt
(version / build-id / build-date), PluginYmlParseTest.java
(version pin + sentence check), and the new harness
PersonalClaimsUiBackNavTest.java (63 checks). No config,
data, or state-format change.

- Base: the fsync2 ExcelBossTrial 0.5.6 tree, first proven
  to rebuild byte-identical to the installed 0.5.6 candidate
  (471,888 B / 005bd005ff015dc7b7c1f9e271a3606e3601a9e2732cfc5ce603d8caea53449a;
  base suite 2,792 checks / 101 harnesses, 0 failures)
  before any edit.
- Tests: full suite 2,856 checks / 102 harnesses,
  0 failures. Deterministic: package.sh run twice, both
  jars byte-identical.
- Install: jar swap only, in place at the live filename
  plugins/ExcelBossTrial-0.4.1.jar, Nov 16, 2026 by
  wrv-install-failsafe (Homebase lane, panel backup/restart);
  placed jar pull-back EXACT at 472,525 B / 4b737c58….
  Boot: Enabling ExcelBossTrial v0.5.7 = 1, SEVERE = 0.
  KNOWN CANDIDATE DEFECT (recorded in OUTCOME.txt): the
  boot Build line prints the stale BuildInfo class stamp
  EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56 while
  excel-build.txt carries the 0.5.7 build id; placed bytes
  are byte-exact the packet candidate. Install stands.

## What was copied from where

Template: the 0.5.6 records package
(`packages/excelbosstrial-0.5.6-20261115/`, PR #228) —
same flattened layout: source tree at package root, plus
src/ and tests/.

- Source tree (223 files): copied verbatim from
  `~/workspace/board-r425/bb18-fix-build/work/bosstrial-057/`
  — its top-level files and its src/ and tests/ trees,
  flattened at the package root. Its SOURCE.md carries the
  appended 0.5.7 section (spliced at publication per chain
  step R-1; updated SOURCE.md 6,405 B / sha256
  15efa073ad74fbc17b7eadf4c3525f55772b5c28754fc1f4679f3b10f27d91da).
  Closing `diff -r` against the source tree was clean.
- Build record: REPORT.md + PersonalClaimsUi-0.5.7.diff +
  RIG-EVIDENCE.txt from `~/workspace/board-r425/bb18-fix-build/`
  (board run 425 W6 build of record).
- Install evidence: INSTALLED.txt from
  `~/workspace/board-r426/bb18-filing/`; OUTCOME.txt from
  `~/workspace/board-r426/bb18-install-window/`
  (failsafe install of Nov 16, 2026; placed jar pull-back
  EXACT at 472,525 B / 4b737c58…).
- SHA256SUMS was generated locally for this package.

## Proof still owed

Java /bossrewards Back tap rig (slot 48 -> Server Menu) +
one Bedrock spot-open (Mike or bot-as-Mike; not Kailey).
Until these land, ExcelBossTrial 0.5.7 is Installed,
not Proven.
