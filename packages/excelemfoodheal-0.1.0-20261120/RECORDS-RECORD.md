# RECORDS RECORD — ExcelEMFoodHeal 0.1.0 (PREP — not yet published)

Prepared: Nov 20, 2026, ~6:59 PM CST (America/Chicago) /
2026-11-21T00:59Z, records-prep worker R2 for board supervisor
run 669. PREP ONLY: this pass made no GitHub pushes, no Drive
uploads, and no edits outside
~/workspace/board-r669/records/. Recording work is pre-approved
standard workflow (Mike, Nov 4, 2026); the publication and
deposit legs themselves are still owed (see Publication claim
and Drive sections — both NONE-YET / OWED).

Install of record: ExcelEMFoodHeal 0.1.0 INSTALLED Nov 20, 2026,
runner-hybrid window runs/20261120-1335-runner-hybrid, Packet B
20261120-1814-hybrid-b (12/12 legs exit 0; post-swap verification
10/10); panel Start 18:19 CT, Done (23.437s) 18:20:01 CT; window
backup wrv-fullpush-20261120 (Manual, 8.1 GB). NEW plugin (D162):
no ExcelEMFoodHeal*.jar existed live before the window (gate
passed). Boot: "Enabling ExcelEMFoodHeal v0.1.0"; enabled line
with debug=true; SEVERE 0. See INSTALLED.txt in the build folder
and the hybrid RUN-RECORD Segment 3 (~18:33 CT).

What it does (D162): in EliteMobs worlds only (gate:
EliteMobsWorld.isEliteMobsWorld registry OR config world list,
default em_primis + em_adventurers_guild), eating grants +1% of
max health instantly plus a heal budget totalling
clamp(saturation x 0.75, 1%, 10%) of max health (steak 9.6%,
bread 4.5%), delivered at 0.5%/s while out of combat; the budget
waits during combat (20 s window mirroring ACS), expires 90 s
after the meal, is replaced (never stacked) by re-eating, and
never heals above max. EliteMobs' own flat ACS food heal is
untouched. No commands, no UI in v1; server-side, identical for
Java and Bedrock.

## Publication claim line

Publication claim: wrv-plugins PR #NONE-YET (duplicate check run 2026-11-21T00:5xZ, result NO MATCH — exit 0 for fragments 'foodheal', 'foodheal-0.1.0', 'questxp', 'questxp-1.1.0' per W3 §2; generic 'foodheal' also exit 0 NO MATCH; lane: W3 read-only pr-exists.sh sweep, board run 669)

## ExcelEMFoodHeal 0.1.0 record package — PREPARED, NOT PUBLISHED

- PR: NONE-YET — no wrv-plugins PR exists for 0.1.0 (W3 §1d/§2:
  RECORDS-RECORD count 0 and version PR count 0 before this prep;
  duplicate guard below). The publish leg (Homebase native git,
  draft PR, never merged) is OWED and is not part of this prep.
- Intended branch (per house pattern, not yet created):
  record/excelemfoodheal-0.1.0-20261120
- Intended package path (not yet created):
  packages/excelemfoodheal-0.1.0-20261120/
- Source of record (hash-verified this run, not guessed): build
  folder ~/workspace/emfoodheal-build-20261120/ —
  `sha256sum -c SHA256SUMS` returned OK for jar + source zip
  this run (the file also carries the pinned EliteMobs jar hash,
  OK); the installed jar hash in INSTALLED.txt matches the
  candidate, and the hybrid RUN-RECORD post-swap verification
  pulled the live jar at 8c7f1cec... (10/10 EXACT across the
  chain's placed files).
- File inventory of record (build folder): BUILD-RECORD.md
  11,741 B / da024f820c764d179d8f81eb96b3fda58c4e634dfd8da0765546be193d46638f;
  INSTALL-PACKET.md 5,508 B /
  624442e121de12389ad93f3d2a8125a3462b68f705f0997ee50f61d537da9103
  (lint 5508/118/0); INSTALLED.txt 817 B /
  1f9ce5d941dfe14be6c62803191af2d4eb1cab088da01f1d19dc58e74a9575fd;
  RECEIPTS.md 2,094 B /
  a48fdf3f9ca6bb0212da9e9b5d0ecbe375b09588e5042817827953e126ce704c;
  SHA256SUMS (jar + source zip + pinned EliteMobs jar, all OK
  this run). Jar NOT in the future GitHub package (Drive only,
  hash-referenced), per the 1.2.1 template pattern.
- Source zip contents: 22 files — 4 source .java
  (ExcelEMFoodHeal, FoodTable, HealEngine, WorldGate), 5 test
  .java (Check, FoodTableTest, HealEngineTest, TestMain,
  WorldGateTest), plus plugin.yml, config.yml, build.sh,
  run-tests.sh.
- Key hashes (all re-hashed by R2 this run; NO mismatches):
  jar 15,564 B /
  8c7f1cecb1e6a911fdb6d8c1156d5ca6765dfeca9ef646e393c5bb46c1a59d05 /
  MD5 ca90c886c1b330e00cf9a5ff2a863389; source zip 16,436 B /
  2535d7aa2fcd40e41b21f5c36e5b45b7e5ed542c8aff57774e4fd8099df0a635 /
  MD5 cca2e173dcc51870a690106c4e585dae; plugin.yml (from jar)
  347 B /
  b9f91212a983a5035b60649cea3a1f6330333981aae60d2be72aa5d9b4e770d4
  (name: ExcelEMFoodHeal, version: 0.1.0,
  softdepend: [EliteMobs]).
- Pinned premise (RECEIPTS.md): EliteMobs jar
  EliteMobs-10.9.10.jar (internal 10.9.14) 14,563,211 B /
  4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd
  (audit pull 20261120-062811); the world-gate API
  EliteMobsWorld.isEliteMobsWorld(UUID) was verified on these
  exact bytes via javap.
- MANIFEST state: the PENDING entry filed in run 655 was
  converted at Segment 3 close-out to the one-line tombstone
  "## INSTALLED — ExcelEMFoodHeal 0.1.0 — Nov 20, 2026 — full
  record in MANIFEST-ARCHIVE.md" in MANIFEST.md; the full
  PENDING text is preserved verbatim in MANIFEST-ARCHIVE.md
  under the runner-hybrid chain header.

## Build / test summary (from BUILD-RECORD.md, hash above)

- Unit tests: 137/137 pass (FoodTableTest: saturation values +
  class bands + clamps; HealEngineTest: delivery rate,
  combat pause/resume with no drain, replace-not-stack, 90 s
  expiry incl. mid-pause, full-health wait; WorldGateTest: OR
  truth table).
- Rig (Paper 26.2-123 + real EliteMobs 10.9.14, SEVERE 0): all
  10 driver cases PASS — API, overworld no-op (no FOODHEAL
  line), instant heal (50 -> 51.0 exactly), HoT completion
  (9.60% budget), pause at remaining=7.10% with health frozen,
  resume exactly 20 s after the hit with nothing drained,
  clamp at max (100.0/100.0), replace-not-stack (46.5, inside
  the 45.4-47.0 band). 90 s expiry is unit-proven on the
  injected clock, not re-run in rig real time.
- Double-build byte-identical (both builds 15,564 B, same
  SHA-256; cmp clean).

## Drive deposits — OWED (not done in this prep)

- No Drive uploads were made by R2 (prep-only scope). Owed at
  publication: ExcelEMFoodHeal-0.1.0.jar to Build files and
  ExcelEMFoodHeal-0.1.0-source.zip + SOURCE.md to Source, each
  verified by Drive size + MD5 EXACT and download-back MD5 +
  SHA-256 EXACT, per the 1.2.1 template. Expected local values:
  jar 15,564 B / MD5 ca90c886c1b330e00cf9a5ff2a863389 /
  SHA-256 8c7f1cec...a59d05; source zip 16,436 B /
  MD5 cca2e173dcc51870a690106c4e585dae /
  SHA-256 2535d7aa...0a635.

## Surfaces state (evidence-proven only; rest OWED-UNVERIFIED)

- Packet requirement (INSTALL-PACKET.md, same-pass surfaces):
  What's New line lands on BOTH surfaces (website section +
  in-game ExcelWhatsNew changelog, verified from the live
  file) — drafted text in the packet; website pair (page +
  ai-knowledge) gains the food-healing note; broadcaster tips
  check (packet notes no existing tip scopes food healing in EM
  worlds — confirm); in-game menus check (no menu/command
  changes — confirm none needed). Guide leg suspended under the
  Nov 16 Guide hibernation; website is canon.
- Proven by evidence in hand: NONE of the 0.1.0 surfaces are
  proven landed. No 0.1.0 surfaces publish record was in the
  evidence set given to R2, and W3 §3 shows the related P-0029
  in-game changelog candidate still OWED placement.
- Therefore: website pair = OWED-UNVERIFIED; What's New website
  = OWED-UNVERIFIED; What's New in-game = OWED-UNVERIFIED; tips
  check = OWED-UNVERIFIED; menus check = OWED-UNVERIFIED.

## Proofs owed

- Player-lane (D154 pool, Jester or Marvin; Java + Bedrock):
  eat-and-heal proof in an EM world — a player at partial
  health in Primis eats a steak: log shows a FOODHEAL meal line
  (9.6% budget) delivering over ~17 s out of combat; a hit
  mid-budget pauses delivery (FOODHEAL pause line) and it
  resumes ~20 s after the last combat action; eating in the
  overworld produces no FOODHEAL line.
- OWED CONFIG LEG (depends on the proof above): FoodHeal was
  left with debug=true after install — flip
  plugins/ExcelEMFoodHeal/config.yml debug true -> false AFTER
  the eat-and-heal proof lands, then reload/restart per the
  text-edit exemption (per packet + INSTALLED.txt + hybrid
  RUN-RECORD Segment 3 summary). Until then debug logging stays
  ON deliberately (the proof IS a log read).
- Publication leg: wrv-plugins draft PR + Drive deposits (above).
- Surfaces legs (above): all OWED-UNVERIFIED.

## Sums verification summary

- Build-folder SHA256SUMS: jar + source zip OK locally this run
  (R2); the pinned EliteMobs jar entry also OK.
- R2 re-hash this run: jar, source zip, BUILD-RECORD.md,
  INSTALL-PACKET.md, INSTALLED.txt, RECEIPTS.md — all EXACT vs
  W3 §1d and vs the task brief (jar 8c7f1cec..., INSTALLED
  1f9ce5d9..., BUILD-RECORD da024f82...). Mismatches: NONE.
- Post-swap live verification (hybrid RUN-RECORD): placed live
  jar 8c7f1cec... EXACT, 10/10 across the chain.
- This folder's SHA256SUMS: covers this RECORDS-RECORD.md + the
  referenced jar + source zip (hashes as above).

## Notes / blockers

- Blockers for publication: NONE known — duplicate guard is
  clean (NO MATCH); the publish leg simply has not run yet.
- The debug=true state is NOT a defect: it is the packet's
  deliberate proof posture (TrainingWheels precedent). The flip
  is an owed leg, recorded above — do not flip before the proof.
- This prep record is the records deliverable for run 669; the
  supervisor's publication worker owns the PR + Drive legs and
  must re-run pr-exists.sh immediately before pushing (G2).
