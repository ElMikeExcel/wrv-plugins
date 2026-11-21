# RECORDS RECORD — ExcelEMWeekendXP 1.3.0

Prepared: Nov 20, 2026, ~6:59 PM CST (America/Chicago), records-prep
worker R1 dispatched by board supervisor run 669. PREP ONLY: no
GitHub push, no Drive upload, no install, no panel, no live server
changes, no emails/messages in this lane. Recording work is
pre-approved standard workflow (Mike, Nov 4, 2026); publication
(GitHub record PR + Drive deposits) is a later lane and is NOT
claimed as done here.
Structure follows the 1.2.1 record of record:
~/workspace/weekendxp-records-20261120/RECORDS-RECORD.md
(ExcelWeekendXP 1.2.1, PR #269).

Install of record: ExcelEMWeekendXP 1.3.0 INSTALLED Nov 20, 2026,
runner-hybrid window (~/workspace/wrv-install-queue/runs/
20261120-1335-runner-hybrid), Packet B 20261120-1814-hybrid-b
(12/12 legs exit 0; post-swap verification 10/10); panel Start
18:19 CT, Done (23.437s) 18:20:01 CT; window closed Segment 3
~18:33 CT Nov 20, 2026. Window backup wrv-fullpush-20261120
(Manual, 8.1 GB). See INSTALLED.txt in the build folder and the
hybrid RUN-RECORD.md Segment 3 lines quoted under Install
evidence below. MANIFEST: the PENDING entry was converted to an
INSTALLED tombstone at Segment 3 close-out and its full text
moved verbatim into MANIFEST-ARCHIVE.md under the header "Full
PENDING entries for the Nov 20, 2026 runner-hybrid chain".

## Publication claim line

Publication claim: wrv-plugins PR #NONE-YET (duplicate check run 2026-11-21T00:55:16Z, result NO MATCH for version-specific fragments 'weekendxp-1.3.0' exit 0 and 'excelweekendxp-1.3.0' exit 0; generic fragment 'weekendxp' exit 3 matched only pre-1.3.0 PRs #269 / #36 / #30 — no 1.3.0 PR exists; source: board-r669 W3.md §2)

## ExcelEMWeekendXP 1.3.0 record package — PREPARED, NOT PUBLISHED

- PR: NONE-YET — no record PR for 1.3.0 exists (duplicate guard
  G2 above). A later publication lane creates it as an OPEN DRAFT
  in wrv-plugins, never merged, per house rule.
- Expected package identity (from the 1.2.1 pattern, not yet
  created): branch record/excelemweekendxp-1.3.0-20261120;
  package path packages/excelemweekendxp-1.3.0-20261120/.
- Source of record (hash-verified this run, not guessed): build
  folder ~/workspace/weekendxp-130-build-20261120/ —
  `sha256sum -c SHA256SUMS` returned OK for both artifacts this
  run (2/2); the installed jar hash in INSTALLED.txt matches the
  candidate exactly (see Sums verification summary).
- Key hashes (re-hashed by this worker Nov 21, 2026, 00:59 UTC;
  all MATCH the build folder, INSTALLED.txt, INSTALL-PACKET.md,
  MANIFEST-ARCHIVE.md, and W3.md §1a — no mismatch found):
  - Jar ExcelEMWeekendXP-1.3.0.jar: 18,943 B /
    SHA-256 9bc65521d16079921cd1239b18f3fb056c1058e617bb658bc4f47e6374413f70 /
    MD5 55095e8db4a9f12e4423f673cea6375d
  - Source zip ExcelEMWeekendXP-1.3.0-source.zip: 16,967 B /
    SHA-256 39e27419e6d9e00bf04bcdec157d3240af707906840641f75019b9693b8cd494 /
    MD5 5b09e3040a0786d8d54b9b07bb9d4eb1 (20 files in zip listing)
  - plugin.yml inside the jar (read via unzip -p this run):
    name: ExcelEMWeekendXP, version: 1.3.0,
    main: com.winkreyes.excelweekendxp.ExcelWeekendXP,
    depend: [mcMMO], softdepend: [EliteMobs]
  - Build-folder docs: BUILD-RECORD.md 21,854 B /
    68d7a8dc8e30b1f4cf090e993ba20d30ab30452f211ab866e926af9d2799f7d3;
    INSTALLED.txt 862 B /
    f7a6bdbf0b9e993afa5d0c36bed5348b3c7588b3ee505a57b50c0ea29f519d0f;
    INSTALL-PACKET.md 11,380 B /
    af4714a79c21ee99d3c78c368773e0ac4fef3b21831a993fb90cba7a8bd5904f;
    RECEIPTS.md 2,938 B /
    af3a5dad536cc2f16d1959271b95c1050389513f6818459bf99ca059cbe2b2ce
- Prep staging retained (this lane's only output):
  ~/workspace/board-r669/records/weekendxp-1.3.0/
  (this RECORDS-RECORD.md + SHA256SUMS).

## Build / test summary (from BUILD-RECORD.md)

- Scope: D155 — the Fri–Sun 1.2x weekend bonus gains a third leg:
  EliteMobs skill XP from combat gains scales 1.2x via EliteMobs'
  own EliteSkillXpGainEvent (HIGHEST, ignoreCancelled; round to
  nearest long; never reduces; weekday = event untouched). The
  mcMMO mcmmo:xprate leg and the vanilla PlayerExpChangeEvent leg
  are behavior-identical to 1.2.1. Programmatic grants
  (ExcelEMQuestXP payouts, quest backpay via PlayerData.addSkillXP)
  do NOT fire that event and stay exact — no grant-path code.
- Rename: D163/D171 — ExcelWeekendXP 1.3.0 renamed to
  ExcelEMWeekendXP BEFORE install: plugin.yml name + jar filename
  only; main class/package unchanged; no commands, no config.
  Entry-by-entry diff vs the pre-rename candidate: only
  plugin.yml line 1 differs; every class file byte-identical.
- Tests: 38/38 pass on BOTH builds (8 containers, 0 failed) —
  25 pre-existing 1.2.1 tests unmodified + 13 new D155 tests
  (EmSkillXpMultiplierTest 5, WeekendScheduleTest three-leg 6,
  ExcelWeekendXPTest wiring 2); the rename re-ran the full suite
  38/38 on both rename builds.
- Deterministic double-build: byte-identical (cmp clean), both
  builds 18,943 B / 9bc65521... (renamed candidate).
- Rig proof: 3 boots on the build rig (Paper 26.2-123 + live
  EliteMobs + live patched mcMMO), all SEVERE 0 — boot 1 EM
  present (all three legs 1.2x), boot 2 EM absent (two-leg
  fallback), boot 3 renamed jar (enables as ExcelEMWeekendXP
  1.3.0, all three legs 1.2x, no data folder created). Honest
  limit in the build record: no player was on any rig boot, so
  no live EliteSkillXpGainEvent fired on a server; in-game
  scaling is unit-proven and owed as player proof (below).
- Live premise (G1, RECEIPTS.md): fresh pull Nov 20, 2026,
  15:08 UTC via Homebase — live ExcelWeekendXP-1.2.1.jar
  15,148 B / b4725c555bbf218135d4c3b6eb40ddcdbecb05e46c48e2dec1e49cb1e1d791e6
  (byte-identical to the 1.2.1 build of record); live EliteMobs
  jar 14,563,211 B / 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd
  with EliteSkillXpGainEvent + Reason.COMBAT verified via javap.

## Install evidence

- INSTALLED.txt (build folder, 862 B / f7a6bdbf...): installed
  2026-11-20, runner-hybrid window, Packet B
  20261120-1814-hybrid-b (12/12 legs exit 0; post-swap
  verification 10/10); panel Start 18:19 CT, Done (23.437s)
  18:20:01 CT; backup wrv-fullpush-20261120; placed jar
  18,943 B / 9bc65521...; old ExcelWeekendXP-1.2.1.jar renamed
  to .bak-20261120; boot "Enabling ExcelEMWeekendXP v1.3.0" x1,
  old name x0, Friday verified-rate line 1.2x on all three legs,
  SEVERE 0; end state server ONLINE, whitelist OFF. Data-folder
  migration resolved to no-op: live plugins/ExcelWeekendXP/
  did not exist (hybrid RUN-RECORD Segment 2 finding), so there
  was nothing to copy.
- Hybrid RUN-RECORD.md
  (~/workspace/wrv-install-queue/runs/20261120-1335-runner-hybrid/RUN-RECORD.md):
  - 18:15:41 CST — Packet B COMPLETE, 12/12 legs exit 0.
  - 18:15–18:21 CST — post-swap verification PASS: placed live
    ExcelEMWeekendXP-1.3.0.jar hash 9bc65521... EXACT (10/10
    placed files exact across the whole chain).
  - Segment 3: server Started via panel 18:19 CT, Done (23.437s)
    18:20:01, Online 0/10; parent-captured boot lines: WeekendXP
    1.3.0 + 1.2x legs, no old-name line; SEVERE 0.
  - Window closed ~18:33 CST: "ExcelEMWeekendXP 1.3.0 (enable
    x1, old name x0, Friday 1.2x all three legs)"; guild "5 of 5
    seals broken, guild OPEN"; end state ONLINE + whitelist OFF;
    MANIFEST 6 PENDING converted to INSTALLED tombstones and
    INSTALLED.txt markers written x6 including this build folder.

## Drive deposits — NOT DONE (owed, later lane)

- Prep only: no Drive upload was made or attempted in this lane.
- Owed at publication: ExcelEMWeekendXP-1.3.0.jar to the Build
  files folder and ExcelEMWeekendXP-1.3.0-source.zip (+ SOURCE
  doc, per the 1.2.1 pattern) to the Source folder, each verified
  by Drive-reported size + MD5 EXACT and download-back MD5 +
  SHA-256 EXACT, as in the 1.2.1 record. Local MD5/SHA-256 for
  both artifacts are in the Key hashes section above.

## Surfaces state (evidence-bounded; unproven = OWED-UNVERIFIED)

Per the INSTALL-PACKET.md same-pass surfaces list. No surface
completion evidence exists in the build folder, INSTALLED.txt,
the hybrid RUN-RECORD, or W3.md for this version, so nothing
below is claimed done:

- Website pair (page + ai-knowledge; must name EliteMobs skill
  XP in the weekend bonus and the plugin as ExcelEMWeekendXP):
  OWED-UNVERIFIED
- What's New — website section: OWED-UNVERIFIED
- What's New — in-game ExcelWhatsNew changelog: OWED-UNVERIFIED
  (cf. W3.md §3: the P-0029 changelog candidate placement is
  separately recorded as STILL OWED; no 1.3.0 changelog line is
  proven by any evidence read in this lane)
- Broadcaster tips check (any tip scoping the bonus to
  mcMMO/vanilla only): OWED-UNVERIFIED
- In-game menus check: no menu or command changes in 1.3.0 per
  the packet (the plugin defines no commands) — confirmation
  check itself OWED-UNVERIFIED
- Guide knowledge/intents: intentionally NOT updated — Guide
  development hibernated (Nov 16 ruling); the website stays
  canon. This is the packet's stated disposition, not an
  omission.

## Proofs still owed (player lane)

From INSTALLED.txt, INSTALL-PACKET.md, and the hybrid
RUN-RECORD carried-out proof list ("WeekendXP 1.2x kill proof"):

1. Weekend leg — on a Fri–Sun day, a player kills an elite mob
   and the EliteMobs skill-XP gain pays 1.2x (skill XP
   before/after against the unscaled gain for the same kill).
2. Weekday leg — on a Mon–Thu day, the same gain is exactly
   1.0x (untouched).
3. No "schedule incomplete" WARN across one boot + one
   30-minute cycle.
Testers: Jester or Marvin (D154 pool; Mike does not test);
Java + Bedrock both. A proof is discharged only by the tester's
own report.

## Sums verification summary

- Build-folder SHA256SUMS: 2/2 OK locally this run
  (jar + source zip, re-hashed Nov 21, 2026, 00:59 UTC).
- Re-hash vs task/W3 values: jar 18,943 B / 9bc65521... MATCH;
  INSTALLED.txt 862 B / f7a6bdbf... MATCH; BUILD-RECORD.md
  21,854 B / 68d7a8dc... MATCH. Mismatches found: NONE.
- Install-side verification (installer evidence, not this
  lane's): Packet B post-swap pull 10/10 EXACT incl. this jar.
- This folder's SHA256SUMS covers this RECORDS-RECORD.md plus
  the referenced jar, source zip, and build-folder docs by
  absolute build-folder path.

## Notes / blockers

- Blockers for prep: NONE. Package prep complete.
- Still owed elsewhere (not this lane): publication (wrv-plugins
  record PR, OPEN DRAFT + Drive deposits above), surfaces
  close-out (above), and player proofs (above).
