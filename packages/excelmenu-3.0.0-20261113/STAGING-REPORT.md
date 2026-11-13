# STAGING REPORT — ExcelMenu 3.0.0 records package

Staged Nov 13, 2026 (desk staging only — no GitHub, Drive,
panel, or MANIFEST work; nothing sent or published).
Package: `~/workspace/records-catchup-20261103/packages/excelmenu-3.0.0-20261113/`
Pattern: the ExcelSummons 0.1.8 records package at
`~/workspace/records-catchup-20261103/packages/excelsummons-0.1.8-20261112/`
(most recent comparable package, Nov 12, 2026), mirrored
file-class for file-class, adapted to ExcelMenu's native
Maven tree exactly as the excelmenu-2.0-phase2-20261103
package carries it. The ExcelSkillsPanel 0.2.1 records package
staged Nov 13, 2026 in its build folder
(`~/workspace/skillspanel-021-build-20261113/records-package/`)
was also inspected: it is a minimal 7-file deposit set (jar +
sources zip + 5 docs, no expanded tree, no FILE-LIST) for a
different leg; where the two patterns differ, this package
follows the fuller records-catchup pattern (expanded tree +
FILE-LIST + SHA256SUMS over every file), and the jar is
EXCLUDED here per the task brief's house rule — the SkillsPanel
deposit set carries its jar, this GitHub-bound package does
not.
Sources: `~/workspace/excelmenu-30-build-20261113/`
(BUILD-REPORT.md, SHA256SUMS, packet/, rig-bm292/, src/,
fixtures/, tools/, build.sh) and
`~/workspace/excelmenu-30-design-20261113/DESIGN.md`.

## What was staged — 231 files total

Copied verbatim from the build/design folders (223 files):

- Top-level build/install docs + artifacts (10):
  BUILD-REPORT.md, DESIGN.md (74,533 B, SHA-256 bb82539e…),
  ExcelMenu-3.0.0-sources.zip (286,127 B, SHA-256
  792a2ec8c6bd54762ad4f40d592c14f43975f1a04fd3b0fa51f11382c5e382df),
  INSTALL-PACKET.md (the FINAL packet, from packet/),
  INSTALLED.txt, OUTCOME.txt, RIG-REPORT.md (from
  rig-bm292/), build.sh, plugin.yml, config.yml (the shipped
  copies from src/main/resources/).
- Install data placements (3), under filing-data/:
  config.yml (2,564 B / 910c2f83…),
  layouts/server_menu.yml (7,182 B / 9af8ddef…),
  menus/skills_perks.yml (1,969 B / 236f1cd2…) — byte-copies of
  the files the install window placed live (OUTCOME.txt).
- Expanded source tree (203): src/ in the native layout —
  src/main/java (73 .java), src/main/resources (99: 83
  entries, 12 menus, 2 layouts, config.yml, plugin.yml),
  src/test/java (31 .java).
- Fixtures (4) + tools (3): the conversion inputs and
  generators carried in the sources zip.

Authored/generated at staging (8 files): SOURCE.md (jar
329,425 B / SHA-256
306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51,
sources zip as above; tests + rig + install summaries; the
Drive trio named), README.md (package summary + Drive trio),
FINGERPRINTS.md (honest no-Layer-B statement + Layer C
markers), TEST-RESULTS.md (from build.log + BUILD-REPORT.md),
PUSH-NOTES.md (supervisor legs: G2 guard still to run, Drive
deposit, draft-PR branch record/excelmenu-3.0.0-20261113,
surfaces still owed), FILE-LIST.txt, SHA256SUMS, and this
report.

The jar itself is EXCLUDED (house pattern — the jar goes
only via the Drive leg): a find for *.jar in this package
returns 0 files. It is listed with size + hashes in
SOURCE.md, README.md, FINGERPRINTS.md and PUSH-NOTES.md.

## Pattern differences (named, not hidden)

- **RIG-REPORT.md, not RIG-RESULTS.md:** the pattern packages
  name their rig file RIG-RESULTS.md; the 3.0.0 source file is
  named RIG-REPORT.md in rig-bm292/ and is carried under its
  exact source name.
- **DESIGN.md, not DESIGN-<version>.md:** the design of record
  is a single DESIGN.md in its own folder; carried as-is.
- **build.sh, not local-build.sh:** ExcelMenu's build script
  name, carried as-is.
- **Native Maven tree:** src/main + src/test replace the
  pattern's flat src/com + tests/ split — this is ExcelMenu's
  own layout in both the build folder and the shipped zip,
  and matches the prior excelmenu packages.
- **filing-data/ added:** the pattern's menu-entry slot has no
  exact analogue; the install's three live-data placements
  are the data record of this packet and are carried under
  filing-data/ (the packet folder's own name for them).
- **No MANIFEST-ENTRY-DRAFT.md:** 3.0.0 was filed directly as
  a gated MANIFEST PENDING entry by supervisor run 292 (the
  FINAL INSTALL-PACKET.md in this package is that filing's
  packet); no draft file exists in the build folder and none
  was invented.
- **No separate TEST-RESULTS source file exists** in the build
  folder (the pattern copies one verbatim); TEST-RESULTS.md
  here is authored at staging from build.log (suite OK (163
  tests)) and BUILD-REPORT.md, and says so.

## Verification (all run at staging)

- Build folder: `sha256sum -c SHA256SUMS` in
  excelmenu-30-build-20261113/ — 5/5 OK (jar, sources zip,
  BUILD-REPORT.md, packet/build.log, base-reproduction jar).
  The jar on disk is 329,425 B / 306607ba…df51, exactly the
  task brief's fingerprint.
- Every copied file compared against its source (cmp /
  diff -r): src/ (203) + fixtures/ (4) + tools/ (3) +
  filing-data/ (3) + 10 top-level files — all identical,
  0 mismatches. The packet/ copies of the jar, sources zip,
  BUILD-REPORT.md and build.log are byte-identical to the
  build-root copies (hashes compared).
- Sources zip cross-check: the zip's 211 entries extracted
  and diffed against the build tree — src/, fixtures/,
  tools/ and build.sh all byte-identical, so the package
  trees match BOTH the build tree and the shipped zip.
- Jar spot-checks: the shipped jar's plugin.yml reads version
  3.0.0; the jar contains NO excel-build.txt and the tree NO
  BuildInfo class (FINGERPRINTS.md records identity by hash).
- Package: `sha256sum -c SHA256SUMS` in this package —
  230/230 OK (SHA256SUMS covers every package file except
  itself, the pattern convention; FILE-LIST.txt lists the
  same 230 files).

## Still owed (supervisor / parent lanes — PUSH-NOTES.md)

G2 duplicate guard (not yet run for this package), GitHub
draft record PR (branch record/excelmenu-3.0.0-20261113, left
OPEN as draft), the Drive deposit (jar + sources zip +
SOURCE.md trio), the same-pass surfaces (Guide §5.4, website
pair, What's New BOTH, tips checked), and Mike's five
DESIGN §7 player proofs.
