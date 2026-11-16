# ExcelSummons 0.1.13 BUILD — mana +25% (P-SUMMONS-MANA-25)

Worker: board supervisor run 394, worker W7. Built Nov 15, 2026.
Scope of record: ~/workspace/board-r394/summons-mana-scope/SCOPE.md
(28,479 B / 52da51ea…). Decision of record: Mike, Nov 15, 2026,
~12:28 PM CST — adopted +25% draft table (draft of record
~/workspace/board-r392/summons-mana25/TABLE-DRAFT.md, 13,960 B /
024fcc7f…).

**Verdict: BUILT. ExcelSummons 0.1.13 candidate ready for the
supervisor's filing leg (packet draft in packet/ — NOT filed to
MANIFEST, NOT installed, no GitHub push, live server untouched,
0.1.12 tree/packet untouched). Unit shape is jar + config +
Magic modifiers (the 0.1.11 shape, scope flag B) — NOT jar-only
like 0.1.12.**

## Candidate

- Jar: packet/ExcelSummons-0.1.13.jar (also ExcelSummons/
  ExcelSummons-0.1.13.jar + -repro.jar in the build tree)
  — **116,869 B / SHA-256
  97b67906b7aad6b82c548c9ab025618fa5d7c93151331c029eb21cd824ddc1f5**
- Build ID: EXC-SUMMONS-0.1.13-WRV-20261115-01-mana25
  (fingerprint gate PASS: excel-build.txt + BuildInfo fragments)
- Sources zip: packet/ExcelSummons-0.1.13-sources.zip —
  163,820 B / 27eeed3027ad52569421c4ae53ded0b4a1136f8fe764e5872c6d18652dc094a0
  (46 files)
- Candidate config: packet/config.yml — 6,562 B /
  6c449df37666c1d28a3aebc6e04ee55412e84847962b668821bdcbeea2ff4479
- Modifiers replacement: packet/modifiers-replacement-0.1.13.yml
  — 3,751 B / b97cd5d14154efee951210cba848234d7e901160773b183f94cd2b3188f4fabe

## Base gate — PASS (before any edit)

- Base = 0.1.12 tree ~/workspace/board-r365/summons-emitter/
  ExcelSummons/ copied to base-gate/ + ExcelSummons/ in this
  folder (copies verified diff-identical to the source).
- Unmodified base-gate rebuild: **116,690 B /
  a21c584fd9982ec5ce83b652b19b6c9536af31baecdb92251ab42afcf1d81c83
  — byte-identical to the 0.1.12 candidate**, 123/123 tests,
  script double-build byte-identical. Evidence:
  evidence/base-gate-build.log.
- Post-build re-hash of the ORIGINAL 0.1.12 tree (jar, config,
  plugin.yml, local-build.sh, ReservePolicy.java,
  SummonsTest.java) + its packet: all OK — **not mutated**
  (evidence/base-tree-pre-hashes.txt, sha256sum -c clean).

## Tests — 128 found / 128 successful / 0 failed

123 carried from 0.1.12, re-pinned to the +25% table, + 5 new
coherence cases. Evidence: evidence/build-0113-a.log and
build-0113-b.log (both full runs, same counts).

Re-pinned (derived assertions, all at the new values):
PCT/PCT14 fixtures; single/stack holds (archer 25, +bruiser 56,
archer2 50); despawn release (guardian+ghast 88 → 44 → 0);
companions+mount total 64; combat pair 69; tier keys (wolf3 57,
cat2 26); unregister release (82 → 63 → 44 → 0); master 5th-summon
ceiling refusal (applied 75 = archer+bruiser+wolf, familiar
clamp-skipped; ghast 44 refused, message re-pinned); message
format pins (mount 19, familiar 13); sustainability stepwise
(companion field 70 + familiar refused; one elite legal, second
refused; exact-fit trio stepwise to 75); configPins rows;
adopted menagerie + cast archer 25; adopted/cast same-family 32
(wolf 19 + cat 13); menu main page (guardian 44, held 69);
copyable-types + despawn/reserve/refusal emitter literals.

FLIPPED D64 pins (scope §4.2, both asserted as flips in-test):
- 5-wolf pack: was desired wolf5 = 75 exactly → now raw 95,
  desired **wolf3 = 57**, applied 57, all 5 still registered.
- Full elite trio: was applied 70 (two elites) → now raw 126,
  applied **44** (first elite only); priciest pair guardian+ghast
  applied 70 → 44.

Re-derived clamp fixtures at the new values (registry-order
skip-and-continue preserved): sync clamp set guardian/revenant/
bruiser/cat raw 126 → applied 75 via guardian 44 + bruiser 31
(revenant skipped, cat holds nothing, nothing trimmed);
floor-bind clamp (ceiling 90) raw 157 → applied 75 via
guardian 44 + bruiser 31; menu clamp rendering mirrors it.

NEW cases (scope §4.2):
1. mana25SingleHoldsMatchTheAdoptedTable — every family's
   single CAST hold = its adopted percent on raw/applied/
   modifier paths; ADOPTED of each family = 0 on all paths.
2. mana25FourthWolfCastRefusedAtPreCast — FLAG A path 1:
   3 wolves applied 57; check(57,19) = RESERVE_CEILING;
   refusal message "Wolf would hold 19% … already hold 57%".
3. mana25GrandfatheredWolvesHoldNothingAfterReDerivation —
   FLAG A path 2: 5 cast wolves raw 95 → applied 57;
   recordHolds wolf3 true, wolf4/wolf5 false; size stays 5.
4. mana25GrandfatheredElitePairReDerivesToFirstEliteOnly —
   guardian+ghast raw 88 → desired {guardian: 44} only;
   both stay registered; ghast recordHolds false.
5. mana25EmitterReportsTheNewPercents — 0.1.12 regression:
   reserveFields fed from ReservePolicy at the new table
   (hold_percent 44, held_total_percent 69); second-elite
   refusal reason emits reserve_ceiling. All 7 carried
   emitter tests also pass re-pinned (no D64 literal left
   in any emitter assertion).
Floor regression: reserveFloorRespectsToggles… re-pinned to
guardian 44 / archer 25 (floor binds before ceiling under a
shrinking toggle, mechanism unchanged). Exact-fit fields
(archer+bruiser+wolf 75, guardian+bruiser 75,
archer+bruiser+mount 75, archer+archer 50) pinned in
mana25WorkedFieldsMatchTheAdoptedDraft +
reserveCeilingCountsMountAndCompanions. allKeys still
exactly the same 18 keys.

Left as-is deliberately: adminListLineFormatPins — direct
formatter pins (literal percents passed to Nameplate.listLine,
0.1.0–0.1.2 string regression), not table-derived values.

## Changed files (work tree vs base-gate tree, diff -rq)

| File | Size | SHA-256 |
|---|---:|---|
| config.yml (+25% rows + provenance comment; ceiling/floor lines untouched) | 6,562 | 6c449df37666c1d28a3aebc6e04ee55412e84847962b668821bdcbeea2ff4479 |
| plugin.yml (version 0.1.13) | 777 | 8fefedd3805fa5d14d54eb3fb936281d49784fa5ef32ded5406f0bfedd9c5eb2 |
| local-build.sh (version/build-id/tag; PROTECT_API default → this build's deps/ copy) | 5,556 | a090987bfb7296de29d8206fe9c27b7ec65b052d250db617c690484e7cb48770 |
| src/…/BuildInfo.java (0.1.13 / mana25 fragments) | 766 | 33202b2e6ee578ece795062f2dcc0d271e346727b6be3addd662539e0f046d5f |
| src/…/ReservePolicy.java (history-comment append ONLY, no logic edit) | 18,913 | 9946e6acc835981b90893b394a7ce701478b17a9d8374261362df6dae81632d3 |
| tests/…/SummonsTest.java (re-pin + 5 new) | 162,826 | 1b1a8cc64a5c9f1f04c915630376d0ffe0a50ca86ec72b0660b4d0c2536119af |
| DESIGN-0.1.13.md (NEW) | 2,571 | 35bf36a0044b58b3a99e7869e5c7f6ba25dff0db0cad03f5d4f14a4fc5e0507e |

No other tree file differs. Adopted table applied:
archer 25, bruiser 31, guardian 44, revenant 38, ghast 44,
wolf 19, familiar 13, cat 13, mount 19; ceiling 75 / floor 20 /
adopted-origin 0 unchanged.

## Modifiers file (place 3 of 3)

packet/modifiers-replacement-0.1.13.yml re-authored from the
0.1.11 precedent's shape: verified by script — YAML parses,
18/18 blocks, key set identical to the 0.1.11 file, every
mana_max_boost equals the scope §1.2 cumulative table
(archer −0.25/−0.50, bruiser −0.31/−0.62, guardian −0.44,
revenant −0.38, ghast −0.44, wolf −0.19/−0.38/−0.57/−0.76/−0.95,
familiar −0.13/−0.26/−0.39, cat −0.13/−0.26, mount −0.19),
description percents matching (cumulative text 50/62/38/57/
76/95/26/39/26).

## Determinism

- Build-script double-build byte-identical in BOTH full runs.
- Second full run reproduced the candidate exactly:
  cmp evidence/candidate-a.jar == rebuilt jar, both
  97b67906…. Logs: evidence/build-0113-a.log, build-0113-b.log.
- Jar content spot-check: excel-build.txt = the 0.1.13 build
  ID; bundled config.yml carries the +25% rows; plugin.yml
  version 0.1.13; emitter classes present (GameplayEmit/Door/
  Sink); 0 excelprotect entries (gateway compile dep only,
  deps/ExcelProtect-0.1.10.jar 295,195 B / dd78718e…, same
  hash as the run-365 house copy).

## Packet draft (supervisor filing leg — NOT filed)

- packet/INSTALL-PACKET.md — 5,749 B; lint 5749 bytes /
  max line 66 / non-ASCII 0. Live-jar gate pre-filled with
  the 0.1.12 candidate (116,690 B / a21c584f…), STOP if
  0.1.12 has not landed. Jar + config + modifiers install
  steps restored from the 0.1.11 packet (flag B).
- packet/RECEIPTS.md — 2,840 B; lint 2840 / 67 / 0.
  G1 receipt slots BLANK by design (filing-day pulls).
- NO MANIFEST edit, NO install, NO GitHub push by this worker.

## Owed / residuals for the supervisor

- Rig boot (scope §4.3) NOT run in this dispatch — owed
  before filing: scratch rig (Paper 26.2-123 + Magic 11.2.5
  + ExcelProtect 0.1.10 + candidate, jars hash-verified in
  place) proving "Enabling ExcelSummons v0.1.13", the new
  build-id line, "Protect audit door bound", Done, SEVERE 0,
  clean disable, placed config +25% rows, guardian modifier
  spot-check −0.44. Named in packet/RECEIPTS.md.
- Filing waits for the chain: ExcelSummons 0.1.12 must be
  INSTALLED first (its candidate becomes this packet's live
  premise); G1 pulls at filing per scope §4.4.
- Post-install proof owed (packet): Guardian ~44% hold +
  Ghast-alongside refusal (Mike); Master wolf pack 3-hold /
  4th-refused (Jester lane); archer+bruiser+wolf = 75;
  /excelprotect audit reserve_ceiling spot-check; adopted
  pet still 0.
- Same-pass surfaces owed at install (packet names them):
  Guide knowledge, What's New BOTH surfaces, website pair,
  tips + menus checks, old-percent text sweep.
