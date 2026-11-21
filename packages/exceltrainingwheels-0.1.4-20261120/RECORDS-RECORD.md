# RECORDS RECORD — ExcelTrainingWheels 0.1.4

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

Install of record: ExcelTrainingWheels 0.1.4 INSTALLED Nov 20, 2026,
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

Publication claim: wrv-plugins PR #NONE-YET (duplicate check run 2026-11-21T00:55:16Z, result NO MATCH for version-specific fragment 'trainingwheels-0.1.4' exit 0; generic fragment 'trainingwheels' exit 3 matched only other versions' PRs #131 / #72 / #69 / #57 (0.1.3 / 0.1.7 / 0.1.6) — no 0.1.4 PR exists; source: board-r669 W3.md §2)

## ExcelTrainingWheels 0.1.4 record package — PREPARED, NOT PUBLISHED

- PR: NONE-YET — no record PR for 0.1.4 exists (duplicate guard
  G2 above). A later publication lane creates it as an OPEN DRAFT
  in wrv-plugins, never merged, per house rule.
- Expected package identity (from the fleet records pattern, not
  yet created): branch record/exceltrainingwheels-0.1.4-20261120;
  package path packages/exceltrainingwheels-0.1.4-20261120/.
- Source of record (hash-verified this run, not guessed): build
  folder ~/workspace/trainingwheels-elitefix-build-20261120/ —
  `sha256sum -c SHA256SUMS` returned OK for all 5 listed files
  this run (jar, source zip, BUILD-RECORD, INSTALL-PACKET,
  RECEIPTS); the installed jar hash in INSTALLED.txt matches the
  candidate exactly (see Sums verification summary).
- Key hashes (re-hashed by this worker Nov 21, 2026, 00:59 UTC;
  all MATCH the build folder, INSTALLED.txt, INSTALL-PACKET.md,
  MANIFEST-ARCHIVE.md, and W3.md §1b — no mismatch found):
  - Jar ExcelTrainingWheels-0.1.4.jar: 41,632 B /
    SHA-256 d60416f3de432bc2c8ac618b425510565681d3a8b29222f222fd20bec165abd0 /
    MD5 1cd65024b74e370fa578ad738c20f490
  - Source zip ExcelTrainingWheels-0.1.4-source.zip: 51,590 B /
    SHA-256 959eb9ed1e8be9fe122c1d7889994fd7303139ab3e89c3209f1086045a2cfb42 /
    MD5 4d511807381675ea3569d2b6239a5e73 (30 files in zip listing)
  - plugin.yml inside the jar (read via unzip -p this run):
    name: ExcelTrainingWheels, version: 0.1.4 (jar filename and
    internal version AGREE — the live predecessor file was named
    0.1.7 while its code reported 0.1.3),
    main: com.winkreyes.exceltrainingwheels.ExcelTrainingWheels
  - Build-folder docs: BUILD-RECORD.md 9,045 B /
    433b1d301035e2bd4d0ad4ed85f011f9ccf1e0fbe324ebcfc8c1cca50ca1b4f7;
    INSTALLED.txt 847 B /
    bd4bdf31091228bdb7e878793f25d6e62416f8646718597d666107cd53e31454;
    INSTALL-PACKET.md 4,918 B /
    d598a3574e31c17d37825577d6cedf271809f00f8bb9f20e9969432c77ff889e;
    RECEIPTS.md 5,347 B /
    c0ecbbcc8d4d21f724e841f442134e0ee51cc1d1dc776e6d57a79c67f8a0af5e
- Prep staging retained (this lane's only output):
  ~/workspace/board-r669/records/trainingwheels-0.1.4/
  (this RECORDS-RECORD.md + SHA256SUMS).

## Build / test summary (from BUILD-RECORD.md)

- Bug: the kid shield's hostile gate was `damager instanceof
  Enemy`; EliteMobs Primis rabbit bosses are Animals, never
  Enemy, so they hit the protected player (.RylieDSunflower,
  factor 0.25) completely unshielded — verified live Nov 20,
  2026 (full 0.8 hit in em_primis, zero SHIELD lines logged).
- Fix (D158, Mike's direct Nov 20, 2026, 10:22 AM CT,
  superseding D157): cover everything — ALL incoming damage to
  protected players scales by the player's factor (0.25 for
  Rylie), EXCEPT the environmental exclusion set held in code
  (FALL, SUFFOCATION, DROWNING, STARVATION, FIRE, FIRE_TICK,
  LAVA, CONTACT, HOT_FLOOR, FREEZE, CRAMMING, VOID,
  FLY_INTO_WALL, plus code-held WORLD_BORDER, CAMPFIRE, DRYOUT,
  MELTING). The Enemy gate is removed entirely. EliteMobs-elite
  damagers keep the compensation (effective factor = base /
  live dealt multiplier — seal 5: 0.25 / 2.35 = 0.1064), so a
  protected player takes a quarter of the UNSCALED hit; armor
  reduces afterwards as vanilla. Untouched: live config (shape
  byte-identical), door hold, iron siege, commands, factor
  semantics.
- Source of record: delta on the 0.1.3 source
  (~/workspace/exceltrainingwheels/src-0.1.3/); P0 proved the
  unmodified copy rebuilds byte-identical to the 0.1.3 candidate
  (39,077 B / 81e76017...) with 280/280 checks.
- Tests: 303/303 pass (246 carried checks in the door/siege/seam
  suites unmodified + 57 in the rewritten ShieldRuleTest;
  superseded 0.1.3 admission checks removed and listed in the
  test file's header).
- Deterministic double-build: byte-identical (cmp clean), both
  builds 41,632 B / d60416f3...
- Rig proof: Paper 26.3-235 + EliteMobs 10.9.8 + TW 0.1.4,
  RIGRESULT 10/10 PASS, SEVERE 0 — elite rabbit composition
  4.7 -> 0.5 (ratio exactly 0.25/2.35) and the literal live
  incident 0.8 -> 0.0851, plain rabbit 4.0 -> 1.0, killer bunny
  8.0 -> 2.0, zombie control 8.0 -> 2.0, witch MAGIC 6.0 -> 1.5,
  unattributed POISON 1.0 -> 0.25, FALL 10.0 -> 10.0 unscaled
  with NO SHIELD line, tamed wolf 8.0 -> 2.0. RIG LIMIT, stated
  plainly in the build record: a full EliteMobs CustomBossEntity
  spawn was NOT achieved on the rig (custom-boss registry empty,
  EMREGISTRY size=0); the elite case was proven against a real
  rabbit-base entity carrying EliteMobs' real elite tag
  (PersistentTagger.tagElite, isEliteEntity=true) through the
  real Bukkit event pipeline. The genuine-boss link is covered
  by the post-install proof owed below.
- Live premise (G1, RECEIPTS.md): Nov 20, 2026 live check —
  live boot log showed internal version 0.1.3 (file named
  ExcelTrainingWheels-0.1.7.jar), shield ON (1 protected),
  "shield compensation: seal 5, elite factor 0.1064 (dealt
  x2.35, base factor 0.25, source: GuildGate)"; live jar bytes
  were not pulled at build time, so the packet gate was filename
  + internal plugin.yml version 0.1.3, verified by the installer
  (below).

## Install evidence

- INSTALLED.txt (build folder, 847 B / bd4bdf31...): installed
  2026-11-20, runner-hybrid window, Packet B
  20261120-1814-hybrid-b (12/12 legs exit 0; post-swap
  verification 10/10); panel Start 18:19 CT, Done (23.437s)
  18:20:01 CT; backup wrv-fullpush-20261120; gate passed — live
  ExcelTrainingWheels-0.1.7.jar internal plugin.yml version read
  0.1.3 at pull, renamed to
  ExcelTrainingWheels-0.1.7.jar.bak-20261120; placed jar
  41,632 B / d60416f3... (D158 shield widening); boot v0.1.4
  enabled lines incl. "shield compensation: seal 5, elite
  factor 0.1064", guild OPEN state line, SEVERE 0; end state
  server ONLINE, whitelist OFF.
- Hybrid RUN-RECORD.md
  (~/workspace/wrv-install-queue/runs/20261120-1335-runner-hybrid/RUN-RECORD.md):
  - Segment 2: TrainingWheels live gate PASS — the staged pulled
    copy of the live jar (39,077 B / 81e76017...) has internal
    plugin.yml version: 0.1.3, exactly the gate value.
  - 18:14–18:16 CST — renames DONE + verified: the old jar now
    present ONLY as .bak-20261120.
  - 18:15:41 CST — Packet B COMPLETE, 12/12 legs exit 0.
  - 18:15–18:21 CST — post-swap verification PASS: placed live
    ExcelTrainingWheels-0.1.4.jar hash d60416f3... EXACT (10/10
    placed files exact across the whole chain).
  - Segment 3: server Started via panel 18:19 CT, Done (23.437s)
    18:20:01, Online 0/10; parent-captured boot lines:
    TrainingWheels 0.1.4 + seal 5 / factor 0.1064; SEVERE 0.
  - Window closed ~18:33 CST: "ExcelTrainingWheels 0.1.4 (seal
    5, elite factor 0.1064)"; guild "5 of 5 seals broken, guild
    OPEN"; end state ONLINE + whitelist OFF; MANIFEST 6 PENDING
    converted to INSTALLED tombstones and INSTALLED.txt markers
    written x6 including this build folder.

## Drive deposits — NOT DONE (owed, later lane)

- Prep only: no Drive upload was made or attempted in this lane.
- Owed at publication: ExcelTrainingWheels-0.1.4.jar to the Build
  files folder and ExcelTrainingWheels-0.1.4-source.zip (+ SOURCE
  doc, per the fleet records pattern) to the Source folder, each
  verified by Drive-reported size + MD5 EXACT and download-back
  MD5 + SHA-256 EXACT, as in the 1.2.1 record. Local MD5/SHA-256
  for both artifacts are in the Key hashes section above.

## Surfaces state (evidence-bounded; unproven = OWED-UNVERIFIED)

Per the INSTALL-PACKET.md same-pass surfaces list (supervisor
chain owns, same pass as install). No surface completion
evidence exists in the build folder, INSTALLED.txt, the hybrid
RUN-RECORD, or W3.md for this version, so nothing below is
claimed done:

- Website pair (index.html + ai-knowledge.json; shield rule text
  changes to the D158 cover-everything rule with the
  environmental exceptions): OWED-UNVERIFIED
- What's New — website section: OWED-UNVERIFIED
- What's New — in-game ExcelWhatsNew changelog (must be verified
  from the live file per the packet): OWED-UNVERIFIED
- Broadcaster tips check: OWED-UNVERIFIED
- In-game menus check: OWED-UNVERIFIED
- Guide knowledge/intents: intentionally NOT updated — Guide
  development is hibernated (Nov 16 ruling), named as such in
  the packet. This is the packet's stated disposition, not an
  omission.

## Proofs still owed (player lane)

From INSTALLED.txt, INSTALL-PACKET.md, and the hybrid
RUN-RECORD carried-out proof list ("TrainingWheels shield
behavior proof"):

1. A Crazed Rabbit hit on a protected account logging a SHIELD
   line at the compensated factor — in em_primis, a Primis
   Crazed Rabbit (or any Primis rabbit boss) hits
   .RylieDSunflower (or a protected test account) and
   latest.log shows a SHIELD line for ENTITY_ATTACK from RABBIT
   at factor 0.1064 (elite true) with the damage reduced to the
   compensated quarter of the unscaled hit.
Tester: fleet proof lane (Iris-run or Jester/Marvin per the
standing split). The proof is discharged only by that log line
being read back, not by the hit alone.

## Sums verification summary

- Build-folder SHA256SUMS: 5/5 OK locally this run (jar, source
  zip, BUILD-RECORD, INSTALL-PACKET, RECEIPTS; re-hashed
  Nov 21, 2026, 00:59 UTC).
- Re-hash vs task/W3 values: jar 41,632 B / d60416f3... MATCH;
  INSTALLED.txt 847 B / bd4bdf31... MATCH; BUILD-RECORD.md
  9,045 B / 433b1d30... MATCH. Mismatches found: NONE.
- Install-side verification (installer evidence, not this
  lane's): Packet B post-swap pull 10/10 EXACT incl. this jar;
  live gate (internal 0.1.3) PASS in Segment 2.
- This folder's SHA256SUMS covers this RECORDS-RECORD.md plus
  the referenced jar, source zip, and build-folder docs by
  absolute build-folder path.

## Notes / blockers

- Blockers for prep: NONE. Package prep complete.
- Still owed elsewhere (not this lane): publication (wrv-plugins
  record PR, OPEN DRAFT + Drive deposits above), surfaces
  close-out (above), and the player proof (above).
