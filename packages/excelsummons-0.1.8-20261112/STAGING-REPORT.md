# STAGING REPORT — ExcelSummons 0.1.8 records package

Staged Nov 12, 2026 (desk staging only — no GitHub, Drive,
panel, or MANIFEST work; nothing sent or published).
Package: `~/workspace/records-catchup-20261103/packages/excelsummons-0.1.8-20261112/`
Pattern: the ExcelSummons 0.1.7 records package at
`~/workspace/records-catchup-20261103/packages/excelsummons-0.1.7-20261112/`,
mirrored file-class for file-class.
Sources: `~/workspace/summons-menu-017-build/build-0.1.8/`
(BUILD-RECORD.md, RIG-RESULTS.md, TEST-RESULTS.md, SHA256SUMS,
ExcelSummons/ source tree, packet-0118-20261112/).

## What was staged — 47 files total

Copied verbatim from the build folder (41 files):

- Top-level build docs (4): BUILD-RECORD.md, RIG-RESULTS.md,
  TEST-RESULTS.md, ExcelSummons-0.1.8-sources.zip (117,441 B,
  SHA-256 6c6e2943bdb35f2fd7ce17adc0eb5bb50d1f7ad4f65437442beaa77eb9029974;
  carried under its exact build name — 0.1.7's artifact was
  named "-source.zip", 0.1.8's is "-sources.zip").
- Design docs (6): DESIGN-0.1.3.md … DESIGN-0.1.8.md
  (0.1.8 adds DESIGN-0.1.8.md; the 0.1.7 package carried
  0.1.3–0.1.7).
- Packet record (3): INSTALL-PACKET.md, INSTALLED.txt,
  OUTCOME.txt — from packet-0118-20261112/ (INSTALLED.txt is
  the packet folder's copy, per the task).
- Plugin files (3): config.yml (byte-identical to 0.1.7,
  SHA-256 659d9e2c…c3a9), local-build.sh, plugin.yml.
- Source tree (24): src/com/winkreyes/excelsummons/*.java.
- Tests (1): tests/com/winkreyes/excelsummons/SummonsTest.java.

Authored at staging (6 files): SOURCE.md (jar 92,387 B /
SHA-256 835419aaeff434b61e6be2f5cda1340233299e11262feebf96206cdd00bb8841,
sources zip as above, build
EXC-SUMMONS-0.1.8-WRV-20261112-01-f8d6ba), FINGERPRINTS.md
(0.1.7 shape, updated for 0.1.8 from the shipped BuildInfo,
the jar's excel-build.txt, DESIGN-0.1.8.md, and OUTCOME.txt),
PUSH-NOTES.md (supervisor legs: G2 guard already run,
Drive deposit, draft-PR branch
record/excelsummons-0.1.8-20261112, no new Guide/What's New
entry owed), FILE-LIST.txt, SHA256SUMS, and this report.

The jar itself is EXCLUDED (house pattern — the jar goes
only via the Drive leg): a find for *.jar in this package
returns 0 files.

## Pattern classes that do NOT exist for 0.1.8 (named, not invented)

- **menu-entry/summons.yml — ABSENT, by design.** The 0.1.7
  package carried the staged compass door; the 0.1.8 packet
  changes no ExcelMenu entry (the 0.1.7 door is already live,
  registry stays 94), and the 0.1.8 build folder contains no
  menu-entry file. Nothing was copied from 0.1.7 to fill the
  slot.
- **README — not a gap:** the 0.1.7 pattern package contains
  no README either (nor a STAGING-REPORT.md; this report is
  the one addition the 0.1.8 task brief requires beyond the
  0.1.7 class list).

## Verification (all run at staging)

- Build folder: `sha256sum -c SHA256SUMS` in
  build-0.1.8/ — 2/2 OK (ExcelSummons-0.1.8.jar +
  ExcelSummons-0.1.8-sources.zip).
- Every copied file compared against its build-folder source
  (cmp / diff -r): 16 top-level files + src/ tree (24) +
  tests/ tree (1) — all identical, 0 mismatches.
- Sources zip cross-check: the zip's contents extracted and
  diffed against the build tree — src/, tests/, plugin.yml,
  config.yml, local-build.sh, and DESIGN-0.1.3…0.1.8.md all
  byte-identical, so the package trees match BOTH the build
  tree and the shipped zip.
- Jar fingerprint spot-checks: the shipped jar's
  excel-build.txt reads EXC-SUMMONS-0.1.8-WRV-20261112-01-f8d6ba;
  its plugin.yml reads version 0.1.8; BuildInfo.java in the
  shipped source assembles the same build ID.
- Package: `sha256sum -c SHA256SUMS` in this package —
  46/46 OK (SHA256SUMS covers every package file except
  itself, the 0.1.7 convention; FILE-LIST.txt lists the same
  46 files).

## Still owed (supervisor / parent lanes — PUSH-NOTES.md)

GitHub draft record PR (branch record/excelsummons-0.1.8-20261112,
left OPEN as draft) and the Drive deposit (jar + sources zip
+ SOURCE.md). G2 duplicate guard already run Nov 12, 2026
~8:25 PM CST by the supervisor: NO MATCH for
excelsummons-0.1.8. Post-install proof owed: Mike's live
Bedrock tap test.
