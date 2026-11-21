# RECORDS RECORD — ExcelEMQuestXP 1.1.0 (PREP — not yet published)

Prepared: Nov 20, 2026, ~6:59 PM CST (America/Chicago) /
2026-11-21T00:59Z, records-prep worker R2 for board supervisor
run 669. PREP ONLY: this pass made no GitHub pushes, no Drive
uploads, and no edits outside
~/workspace/board-r669/records/. Recording work is pre-approved
standard workflow (Mike, Nov 4, 2026); the publication and
deposit legs themselves are still owed (see Publication claim
and Drive sections — both NONE-YET / OWED).

Install of record: ExcelEMQuestXP 1.1.0 INSTALLED Nov 20, 2026,
runner-hybrid window runs/20261120-1335-runner-hybrid, Packet B
20261120-1814-hybrid-b (12/12 legs exit 0; post-swap verification
10/10); panel Start 18:19 CT, Done (23.437s) 18:20:01 CT; window
backup wrv-fullpush-20261120 (Manual, 8.1 GB). This install is
TWO changes in one swap: (1) D159 — player-chosen quest XP
target (/questxp target, /questxp menu, popup; default HIGHEST =
pre-1.1.0 behavior; the /questxp command does NOT change); and
(2) D160 rename ExcelQuestXP -> ExcelEMQuestXP (EliteMobs naming
law, Mike Nov 20, 2026) WITH data-folder migration in the same
swap: live plugins/ExcelQuestXP/ copied to
plugins/ExcelEMQuestXP/, all 4 files SHA-256-identical both
sides (backpay.yml 24274c55..., backpay-paid.yml, grants.log,
quest-xp.yml); source folder left in place; old
ExcelQuestXP-1.0.1.jar renamed to .bak-20261120. Boot:
ExcelEMQuestXP v1.1.0 ready; "Backpay paid ledger loaded: 46";
SEVERE 0. See INSTALLED.txt in the build folder and the hybrid
RUN-RECORD Segment 3 (~18:33 CT).

## Publication claim line

Publication claim: wrv-plugins PR #NONE-YET (duplicate check run 2026-11-21T00:5xZ, result NO MATCH — exit 0 for fragments 'questxp', 'questxp-1.1.0', 'foodheal', 'foodheal-0.1.0' per W3 §2; generic 'questxp' also exit 0 NO MATCH; lane: W3 read-only pr-exists.sh sweep, board run 669)

## ExcelEMQuestXP 1.1.0 record package — PREPARED, NOT PUBLISHED

- PR: NONE-YET — no wrv-plugins PR exists for 1.1.0 (W3 §1c/§2:
  RECORDS-RECORD count 0 and version PR count 0 before this prep;
  duplicate guard below). The publish leg (Homebase native git,
  draft PR, never merged) is OWED and is not part of this prep.
- Intended branch (per house pattern, not yet created):
  record/excelemquestxp-1.1.0-20261120
- Intended package path (not yet created):
  packages/excelemquestxp-1.1.0-20261120/
- Source of record (hash-verified this run, not guessed): build
  folder ~/workspace/questxp-110-build-20261120/ —
  `sha256sum -c SHA256SUMS.txt` returned OK for both artifacts
  this run (jar + source zip); the installed jar hash in
  INSTALLED.txt matches the candidate, and the hybrid RUN-RECORD
  post-swap verification pulled the live jar at cab15744... (10/10
  EXACT across the chain's placed files).
- File inventory of record (build folder): BUILD-RECORD.md
  13,082 B / 2583aac282b692c71d06e3a8a1818977bdc027c98e9719cfdca1a5dbc199f8c6;
  INSTALL-PACKET.md 9,077 B /
  b114a137d53fd4ea613f64178103bfce115dff7f1474c5157ecf0d3b144f53c2;
  INSTALLED.txt 1,262 B /
  5e0dc6ed57d0b8b24024183ea356c01d351912aaf22e19d5d09c9df09d405098;
  SHA256SUMS.txt (jar + source zip, both OK this run);
  tests-110.log 13,547 B /
  e77fab728a91eae688b9279c557a466c2dde9d627663635980deb473945ea39c.
  Jar NOT in the future GitHub package (Drive only,
  hash-referenced), per the 1.2.1 template pattern.
- Source zip contents: 30 files — 12 source .java
  (BackpayLedger, BackpayTable, BedrockForms, BuildInfo,
  ExcelQuestXP, GrantPolicy, QuestXpListener, QuestXpTable,
  SkillGrant, TargetMenu, TargetPolicy, TargetStore), 4 test
  .java (BackpayTestMain, EmApiTestMain, QuestXpTestMain,
  TargetTestMain), plus plugin.yml, build.sh, run-tests.sh,
  backpay.yml, quest-xp.yml.
- Key hashes (all re-hashed by R2 this run; NO mismatches):
  jar 50,603 B /
  cab1574471c50ace591bc12603afbd42272c06aec8c4cc78a3b660ae112b670e /
  MD5 025fb9014e0a51c98b27e921a2130362; source zip 53,486 B /
  279baa56d2a83b9c79027c9deb3a1ba233083a0f5ded412a2de2a5dce34fb9cb /
  MD5 7ea3c1489d84fd6867afcb65804a798a; plugin.yml (from jar)
  1,441 B /
  1988eedc56bb09808746993a0033044c80319fdfe816a533b5d649e23f0f502c
  (name: ExcelEMQuestXP, version: 1.1.0; command /questxp
  unchanged).
- MANIFEST state: the PENDING entry filed in run 655 was
  converted at Segment 3 close-out to the one-line tombstone
  "## INSTALLED — ExcelEMQuestXP 1.1.0 — Nov 20, 2026 — full
  record in MANIFEST-ARCHIVE.md" in MANIFEST.md; the full
  PENDING text is preserved verbatim in MANIFEST-ARCHIVE.md
  under the runner-hybrid chain header.

## Rename + backpay close-out (part of this install's record)

- Rename (D160): plugin.yml name ExcelQuestXP -> ExcelEMQuestXP;
  jar ExcelQuestXP-1.0.1.jar -> ExcelEMQuestXP-1.1.0.jar; Java
  package and main class unchanged
  (com.winkreyes.excelquestxp.ExcelQuestXP); /questxp command,
  permissions semantics unchanged except the 1.1.0 fix that moves
  the command-level admin gate into per-subcommand checks so
  players can use target/menu (new node excelquestxp.target,
  default true). Migration proof: rig phase A re-ran
  /questxp backpay RigA against the migrated ledger -> all
  already paid, 0 paid; live Segment 3 repeated the check (below).
- Backpay CLOSED (0 owed): the pre-bridge backlog was paid in
  full on Nov 20, 2026 under 1.0.1, before this install —
  Marvin (GumGumJetPistol): 13 quests, +1,825 Swords / +603
  Armor, paid 10:33:22 AM CT (PAY-RUN-RECORD.md); Jester
  (Jester_Turtle): 33 quests, +22,060 Wands / +7,344 Armor, paid
  after the counter restage 33 -> 34 (RESTAGE-RECORD.md; the
  34th quest was bridge-paid on the spot at 09:56:40 CT,
  +320/+106, and correctly excluded). All other players: 0 owed.
- Segment 3 corroboration under the renamed plugin: boot line
  "Backpay paid ledger loaded: 46"; console
  /questxp backpay for both players REFUSED-offline by design
  (players offline, nothing paid — the plugin's offline guard,
  not a payment); live plugins/ExcelEMQuestXP/backpay-paid.yml
  pulled and verified: exactly 46 paid records, all timestamped
  2026-11-20 (13 Marvin + 33 Jester) -> 0 owed on primary
  evidence. Backpay is CLOSED; no re-run owed.

## Build / test summary (from BUILD-RECORD.md, hash above)

- Tests: 267/267, exit 0 (tests-110.log). Run A (no EliteMobs
  jar): QuestXp 42/42, Backpay 35/35, Target 34/34 (new: pinned
  routing, stale-pin fallback, Armor split unchanged, input
  resolution incl. armor/garbage refusal, store persistence).
  Run B (pinned EliteMobs 10.9.14): EmApi 45/45, QuestXp 42/42,
  Backpay 35/35, Target 34/34.
- Rig (1.0.1 rig server with the packet migration performed):
  phase A 21/0 (migration pays-0 proof, HIGHEST grant, PINNED
  grant to WANDS, invalid-skill refusal), phase B 12/0 (restart
  persistence of the pin). SEVERE 0 attributable to
  ExcelEMQuestXP in both phases.
- Double-build byte-identical (jar + source zip).

## Drive deposits — OWED (not done in this prep)

- No Drive uploads were made by R2 (prep-only scope). Owed at
  publication: ExcelEMQuestXP-1.1.0.jar to Build files and
  ExcelEMQuestXP-1.1.0-source.zip + SOURCE.md to Source, each
  verified by Drive size + MD5 EXACT and download-back MD5 +
  SHA-256 EXACT, per the 1.2.1 template. Expected local values:
  jar 50,603 B / MD5 025fb9014e0a51c98b27e921a2130362 /
  SHA-256 cab15744...b670e; source zip 53,486 B /
  MD5 7ea3c1489d84fd6867afcb65804a798a /
  SHA-256 279baa56...fb9cb.

## Surfaces state (evidence-proven only; rest OWED-UNVERIFIED)

- Packet requirement (INSTALL-PACKET.md, same-pass surfaces):
  website pair (page + ai-knowledge) gains the /questxp target +
  menu explanation and the ExcelEMQuestXP 1.1.0 rename note;
  What's New on BOTH surfaces (website section + in-game
  ExcelWhatsNew changelog, verified from the live file); tips
  check; menus check (ExcelMenu/ExcelBedrockMenu). Guide leg
  suspended under the Nov 16 Guide hibernation; website is canon.
- Proven by evidence in hand: NONE of the 1.1.0 surfaces are
  proven landed. The only surfaces evidence available to R2 is
  the P-0029 publish set (~/workspace/board-r629/), which covers
  ExcelQuestXP 1.0.1 only, and W3 §3 (its in-game changelog
  candidate still OWED placement, MANIFEST grep "0029" = 0).
- Therefore: website pair = OWED-UNVERIFIED; What's New website
  = OWED-UNVERIFIED; What's New in-game = OWED-UNVERIFIED; tips
  check = OWED-UNVERIFIED; menus check = OWED-UNVERIFIED.

## Proofs owed

- Player-lane (D154 pool, Jester or Marvin): pin a non-highest
  skill via /questxp target, complete a paying quest, grant lands
  in the PINNED skill with target=pinned in the grant message +
  grants.log; /questxp target highest restores auto; Bedrock
  /questxp menu = form, Java /questxp menu = chest GUI.
  (Install-window ledger proof is DONE — see backpay section.)
- Publication leg: wrv-plugins draft PR + Drive deposits (above).
- Surfaces legs (above): all OWED-UNVERIFIED.

## Sums verification summary

- Build-folder SHA256SUMS.txt: 2/2 OK locally this run (R2).
- R2 re-hash this run: jar, source zip, BUILD-RECORD.md,
  INSTALL-PACKET.md, INSTALLED.txt — all EXACT vs W3 §1c and vs
  the task brief (jar cab15744..., INSTALLED 5e0dc6ed...,
  BUILD-RECORD 2583aac2...). Mismatches: NONE.
- Post-swap live verification (hybrid RUN-RECORD): placed live
  jar cab15744... EXACT, 10/10 across the chain.
- This folder's SHA256SUMS: covers this RECORDS-RECORD.md + the
  referenced jar + source zip (hashes as above).

## Notes / blockers

- Blockers for publication: NONE known — duplicate guard is
  clean (NO MATCH); the publish leg simply has not run yet.
- This prep record is the records deliverable for run 669; the
  supervisor's publication worker owns the PR + Drive legs and
  must re-run pr-exists.sh immediately before pushing (G2).
