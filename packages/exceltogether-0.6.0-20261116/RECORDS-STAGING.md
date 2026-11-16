# ExcelTogether 0.6.0 — records package staging notes

Staged Nov 16, 2026 (worker W3, board supervisor run 430).
Published as an OPEN DRAFT record PR in wrv-plugins at
`packages/exceltogether-0.6.0-20261116/` — never merged.
Publication claim: wrv-plugins PR #___ (duplicate check run
2026-11-16T16:15:41Z, result NO MATCH for 'exceltogether-0.6.0';
also NO MATCH on 'together-060', 'exceltogether-060',
'exceltogether' in the same fresh sweep) — PR number filled
in at publish time in the publish record, not in this file.
Prep of record: ~/workspace/board-r428/together-postinstall/
staging/records/RECORDS-PREP.md (board run 428 W13).
Recording work pre-approved (Mike, Nov 4, 2026).

## Package

- File count: 95 files total on disk, including this
  RECORDS-STAGING.md and SHA256SUMS.
- SHA256SUMS: 94 entries — every file in the package
  except SHA256SUMS itself.
- Verification: `sha256sum -c SHA256SUMS` — see the
  publish record for the verified counts (local + Homebase).
- Jar: NOT included in the package (fleet precedent:
  BossTrial 0.5.7 PR #242, Downed 0.1.5 PR #243, Together
  0.5.0 package — jar identity recorded, jar itself not
  in the PR). Candidate jar of record stays in the build
  folder: ExcelTogether-0.6.0.jar — 195,872 B / SHA-256
  6e935f161e2b76c9349b71f73183970beeb8a659a306fb0279075b25f6ebc964
  / MD5 2565a3264081e85e20cbc1df92aca7eb.
  INSTALLED Nov 16, 2026 (see INSTALLED.txt + OUTCOME.txt).

## What 0.6.0 is

The one-roster rebuild: /em party works as a face (door) of
the ONE party roster of record — Mike's ruling Nov 16, 2026
("Both — /em party works as a face of the one party"),
clearing the D80 hold. Built in three parts per the run-418
EM-door scoping: Part 1 = the pending-invite read; Parts 2+3
(coupled) = the native-pending spare + the native-answer
completion, implemented in EmSync.java exactly to supervisor
decisions D88–D93 (DECIDE-AND-LOG, ratified run 426):
D88 pairing by enumeration (poll-atomic snapshot; no
inviter-returning accessor exists in EM 10.9.14), D89
completion via the unmodified Controller.acceptFrom door,
D90 origin RosterOrigin.DOOR, D91 observation map (60 s TTL),
D92 defaults (foreign/crossed never consumed blindly,
PARTY_FULL / INVITER_OFFLINE wordings unchanged, completion
after suppression, dungeon-busy deferral), D93 EmSyncTest
fake fix. Mirror Branch A GREEN (board run 427).

- Tests: full suite 1,375 passed, 0 failed (Part 1 baseline
  1,318 + 57 new). Deterministic: two independent build.sh
  runs byte-identical (195,872 B / 6e935f16…c964).
- Install: jar swap Nov 16, 2026 by wrv-install-failsafe
  (Homebase lane, panel backup/restart; window backup
  pre-together060-20261116); placed jar pull-back EXACT at
  195,872 B / 6e935f16…c964; boot Done (24.583s)!,
  verify 26 PASS / 0 FAIL, SEVERE = 0 (see OUTCOME.txt).
- Surfaces (TEXT) placed Nov 16, 2026, 10:00 CST (marker
  ~/workspace/board-r428/together-postinstall/INSTALLED.txt):
  What's New entry 2026-11-16-exceltogether-060 + Guide
  chunk together-two-faces; canonical registry 319 records.

## What was copied from where

Template: the BossTrial 0.5.7 records package
(`packages/excelbosstrial-0.5.7-20261116/`, PR #242) —
same flattened layout: source tree at package root.

- Source tree (82 files): copied verbatim from
  ~/workspace/board-r426/together-parts23/work/
  build-0.6.0-r426p23/ — src/ (46 .java), tests/ (30 files),
  and the 6 non-jar top-level files (build.sh, config.yml,
  plugin.yml, base-build.log, build-054.log,
  build-054-run2.log), flattened at the package root.
  STAGING DECISION (prep §2 leaves the exact count to
  staging): the tree's work/ subtree (166 files) is NOT in
  the package — it holds build intermediates only
  (compiled .class output under work/out/, a gate-test
  .class, and two dependency jars: work/magic-11.2.5.jar
  7,641,369 B and work/pulled-0.4.3.jar), and the candidate
  jar at the tree root is excluded per the jar rule above.
  Fleet precedent is uniform on this: BossTrial 0.5.7,
  Downed 0.1.5 (prep: "NOT in the package: repro jars,
  base-gate tree, rig jars/logs"), and the Together 0.5.0
  package (66 files, source only) all carry source, never
  build intermediates. The full 249-file tree remains of
  record in the build folder; its Drive source zip carries
  the same 82 source files plus the candidate jar.
- Build records: BUILD-RECORD-PART1.md =
  board-r426/together-rebuild/REPORT.md (Part 1);
  BUILD-RECORD-PARTS23.md = board-r426/together-parts23/
  REPORT.md (Parts 2+3, suite 1,375/0); MIRROR-A-REPORT.md
  + MIRROR-A-SESSION-LOG.md = board-r427/together-mirror-a/
  (Mirror Branch A GREEN).
- Change diffs: part1-emlink.diff, part1-emsynctest.diff,
  part1-reflectemlink.diff (board-r426/together-rebuild/);
  parts23-emsync.diff, parts23-emsynctest.diff
  (board-r426/together-parts23/). Lineage: the Parts 2+3
  tree continues the Part 1 tree (board-r426/together-
  rebuild/work/build-0.6.0-r426/); only EmSync.java and
  EmSyncTest.java differ between them.
- Install evidence: INSTALLED.txt from
  board-r427/together-filing/; OUTCOME.txt from
  board-r427/together-install-window/ (failsafe install of
  Nov 16, 2026; placed jar pull-back EXACT).
- No SOURCE.md exists in the Together tree (checked at
  prep) and none was invented; this file carries the
  provenance instead.
- SHA256SUMS was generated locally for this package.

## Proof still owed

Two-face live proof, BOTH directions (/em party invite +
accept completes and the Together face shows the same pair;
/party invite + EM popup accept completes and /em party
shows the same pair), plus spot checks (party chat
pass-through, leave-anywhere) — Mike or bot-as-Mike +
second real player, NOT Kailey. Until it lands,
ExcelTogether 0.6.0 is Installed, not Proven.
