# PACKAGE NOTES — ExcelSkillsPanel 0.2.2 records package

Staged Nov 13, 2026 (desk staging only — no GitHub, Drive,
panel, or MANIFEST work; nothing sent or published).
Package: `~/workspace/records-catchup-20261103/packages/excelskillspanel-0.2.2-20261113/`
Pattern: the ExcelSummons 0.1.8 records package at
`~/workspace/records-catchup-20261103/packages/excelsummons-0.1.8-20261112/`,
mirrored file-class for file-class.
Sources: `~/workspace/skillspanel-022-build-20261113/`
(BUILD-RECORD.md, DIAGNOSIS.md, INSTALL-PACKET.md,
MANIFEST-ENTRY-DRAFT.md, INSTALLED.txt, OUTCOME.txt,
SHA256SUMS, ExcelSkillsPanel/ source tree, rig/, base-gate/).

## What was staged — 35 files total

Copied verbatim from the build folder (27 files):

- Top-level build docs (6): BUILD-RECORD.md, DIAGNOSIS.md,
  INSTALL-PACKET.md, INSTALLED.txt, MANIFEST-ENTRY-DRAFT.md,
  OUTCOME.txt.
- Plugin files (2): plugin.yml, local-build.sh (from
  ExcelSkillsPanel/; no config.yml exists — the plugin ships
  none).
- Source tree (18): src/com/winkreyes/excelskillspanel/*.java
  (incl. the new MagicGateReads.java).
- Tests (1): tests/com/winkreyes/excelskillspanel/PanelTest.java.

Authored at staging (6 files): SOURCE.md (jar 62,142 B /
SHA-256 374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e,
build EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9),
RIG-RESULTS.md (phase A 71/0, phase B 71/0), TEST-RESULTS.md
(JUnit 38/38), FINGERPRINTS.md (build ID + semantic markers),
PUSH-NOTES.md (supervisor legs: G2 guard NOT yet run, Drive
deposit, draft-PR branch record/excelskillspanel-0.2.2-20261113,
same-pass surfaces owed), and this file.

Generated at staging (2 files): FILE-LIST.txt (34 entries —
every package file except SHA256SUMS) and SHA256SUMS (34
entries — every package file except itself).

The jar itself is EXCLUDED (house rule — the jar goes only
via the Drive leg): a find for *.jar in this package returns
0 files.

## Verification (all run at staging)

- Build folder: `sha256sum -c SHA256SUMS` in
  skillspanel-022-build-20261113/ — 6/6 OK (candidate jar +
  repro jar, PanelRig.jar, both phase results, base-gate 0.2.1
  jar).
- Every copied file compared against its build-folder source
  (cmp per file + diff -r on the trees): 8 top-level files +
  src/ tree (18) + tests/ tree (1) — all identical, 0
  mismatches.
- Src-tree completeness: the package src/ + tests/ trees were
  diffed file-for-file against the build folder's
  ExcelSkillsPanel/ tree (excluding the two jars) — identical,
  no missing or extra files.
- Package: `sha256sum -c SHA256SUMS` in this package — 34/34
  OK (SHA256SUMS covers every package file except itself;
  FILE-LIST.txt lists the same 34 files).
- Jar fingerprint spot-checks: the candidate jar's MD5
  9a9a920a68aed5e2fca0546d0be2a359 matches the repro jar's MD5
  exactly; BuildInfo.java in the shipped source assembles the
  build ID EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9.

## Pattern classes that do NOT exist for 0.2.2 (named, not invented)

- **Sources zip — ABSENT.** The 0.2.2 build shipped no
  `-sources.zip` (unlike Summons 0.1.7/0.1.8). None was created
  at staging — a staging-created zip would misrepresent
  provenance. The src/ + tests/ trees in this package are the
  source of record. Drive leg takes the jar + SOURCE.md only.
- **config.yml — ABSENT, by design.** The plugin ships no
  config.yml and reads no plugin data files of its own; it only
  reads ExcelMagicLevel's live `config.yml` reward table.
- **menu-entry file — ABSENT.** The build folder contains no
  menu-entry file and the packet changes no ExcelMenu entry.
- **base-gate/ + rig/ trees — NOT carried.** The base-gate
  (0.2.1 source + rebuilt jar) and the raw rig tree (544 MB of
  server worlds, boot logs, phase results) stay in the build
  folder as build evidence; their results are summarised in
  BUILD-RECORD.md, RIG-RESULTS.md, and TEST-RESULTS.md, with
  hashes cited there.
- **latest.log — NOT carried.** The 111,165 B live boot log
  stays in the build folder; the boot lines it proves are
  quoted verbatim in OUTCOME.txt.

## PR body facts (for the supervisor's publication leg)

Suggested title: `record: ExcelSkillsPanel 0.2.2 (Menu Programme Items 12 + 13)`
Suggested branch: `record/excelskillspanel-0.2.2-20261113` (draft PR, left OPEN, never merged).

- Installed Nov 13, 2026 — wrv-install-failsafe window
  scheduled 15:03 CST (file-and-fire; supervisor run 309
  filing), PC lane, no fallback. Boot 3:48:09 PM CST: Done
  (22.360s), SEVERE 0, 87 plugins, guild LOCKED 4 of 5.
  Outgoing 0.2.1 preserved as
  `.bak-20261113-pre022` (57,652 B / ce430d1f…).
- What changed: Item 12 removes the "Class: Mage" line on all
  surfaces; Item 13 reads rank / next rank / mana / regen from
  the player's carried wand (WRV order Apprentice → Master,
  never "Wizard"), mana as the effective pool, casts from
  MagicGate's ledger labelled "Credited casts".
- Tests: JUnit 38/38 pass (4 containers). Rig: phase A 71/0,
  phase B 71/0 (real Paper 26.3 + Magic 11.2.5; held /
  not-held / two-wand scenarios; Bedrock body checks).
  Prior run stood at 64/7; all 7 resolved (BUILD-RECORD §2).
- Jar: 62,142 B / SHA-256
  374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e;
  build EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9; repro jar
  identical hash. Base gate: 0.2.1 rebuilt byte-identical to
  live (57,652 B / ce430d1f…), JUnit 33/33.
- Proof owed: Mike's live `/skillpanel` check while holding
  food (wand NOT in hand), Java + Bedrock — Rank Apprentice,
  Next rank Master, Mana 150 / 150, regen 25, Credited casts
  1,215, no Class line. No live two-wand check owed.
- Same-pass surfaces owed (supervisor legs): Guide
  skills-panel chunk, website pair, What's New BOTH surfaces,
  broadcaster tips check, in-game menus check.
- G2 duplicate guard NOT yet run — the supervisor runs
  `pr-exists.sh` before any push; publication claim line goes
  in the close-out record.

## Still owed (supervisor / parent lanes — PUSH-NOTES.md)

GitHub draft record PR (branch above, left OPEN as draft) and
the Drive deposit (jar + SOURCE.md). G2 duplicate guard before
any push. Post-install proof owed: Mike's live `/skillpanel`
check (holding food). Same-pass surfaces owed per above.
