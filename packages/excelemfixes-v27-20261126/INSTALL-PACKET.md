# INSTALL PACKET — ExcelEMFixes v27 (turn-in destination switch)

Filed: Nov 26, 2026 (build lane, follow-on relay D228,
item P-GUIDANCE-TURNIN-SWITCH-20261126). Design source:
quest-guidance re-audit FIX-LIST B2
(~/workspace/quest-guidance-reaudit-20261126/FIX-LIST.md).
Build record:
~/workspace/quest-guidance-turnin-switch-20261126/BUILD-RECORD.md
Lint: 6,588 B / max line 72 / non-ASCII 39 (em dashes)

## What ships

ExcelEMFixes v26 -> v27. ONE existing module extended:
quest-destinations. No new module; no other plugin
changes. This build is stacked on the v26 (FIX-LIST
B1+B6 fetch-derivation) build tree and CONTAINS all of
v26 — see ORDERING below.

- WHY: guidance followed a quest's objectives but never
  its final step. When the last objective completed, the
  resolved destination stayed at the objective phase's
  place. Specimen: "Storm's end"
  (primis_main_quest_14) kept pointing at the Storm
  Temple instance entry (em_primis 972.6, 125, 532.6),
  414 blocks from Shogun Navita (em_primis 596, 72, 772),
  the NPC the finished quest turns in to. The re-audit
  counted 1 hole today plus the whole turn-in-far class.
- WHAT CHANGES: at the ONE shared destination
  resolution point (QuestDestinationModule
  .resolveDestination, served to the quest beacon via
  QuestDestinationApi and read directly by /questwhere
  and the glow guide), a carried quest whose objectives
  are ALL complete now resolves FIRST to its turn-in
  NPC's spawn: the quest file's turnInNPC, else the
  quest giver (an enabled NPC whose quest list names
  the quest). The switch outranks the destinations
  table row (a row encodes the objective phase, which
  is spent); when the turn-in NPC cannot be resolved,
  no switch happens and the ordinary resolution stands.
  New pure policy TurnInDerivation + EM bridge
  EmTurnInData; spawn resolution reuses the 0.19.0
  EmDialogData NPC-registry read. Objective-phase
  behavior is byte-for-byte the v26 behavior.
- TOGGLE: modules.quest-destinations
  .derive-turn-in-destinations (default on). Off = the
  exact pre-v27 behavior. No quest, NPC or place is
  named in code; no WRV data in shipped defaults.

## ORDERING (read before installing)

v27 is a SUPERSET of v26 (it is built on the v26 tree
and includes the B1 fetch derivation + B6 parity).
- If v26 is still PENDING when this installs: install
  v27 INSTEAD of v26 and mark the v26 entry SUPERSEDED
  by this one (the 0.22.0 duplicate-build precedent:
  never install both, never install v26 after v27 —
  that would be a downgrade).
- If v26 is already INSTALLED: this is a normal
  v26 -> v27 swap; the live-jar gate below then reads
  the v26 jar.

## Live-premise receipts (G1)

NOT YET TAKEN — this packet was prepared by a desk
lane under a no-live-action limit. The filing lane
(board supervisor chain) takes these same-day receipts
BEFORE adding the MANIFEST pointer entry:
- Live jar: plugins/ExcelEMFixes-*.jar — same-day pull
  (size + SHA-256 + timestamp) into this packet's
  RECEIPTS.md. Expected lineage: v25 installed Nov 25,
  2026 (MANIFEST tombstone), or v26 if the B1 entry
  landed first. LINEAGE IS NOT A PREMISE: the gate
  below stops on whatever the pull actually shows if
  it is neither the v25 nor the v26 jar.
- Live config: plugins/ExcelEMFixes/config.yml —
  same-day pull; the deploy config is a FULL config
  (fleet pattern for this pack), so the pull also
  proves no live-only values would be lost; any diff
  beyond the v27 additions must be reconciled into
  deploy-config.yml before the swap.

## Candidate

- ExcelEMFixes-v27.jar — 682,311 B / SHA-256
  6c7726b450f579ea88db5626f960fe71ef543b120cac8776b49447ff5442f00e
  (deterministic build: two consecutive builds
  byte-identical, same hash).
- deploy-config.yml — 101,105 B / SHA-256
  0226f22904b5d928ff49421a1671eaae21aefd6654d667d6d5bc0d504a9602f5
  (full config; the ONLY delta vs the v26 deploy
  config is the derive-turn-in-destinations block).
- Tests: full plain-JVM suites 2,137 passed / 1 failed;
  the 1 failure (SkillNudgeMathTest "Jester-shaped:
  armor named first on the tie") is PRE-EXISTING in
  the v26 base tree — the skillnudge sources and test
  are byte-identical between the v26 snapshot and this
  build, and nothing in v27 touches that module. New
  TurnInDerivationTest: 21/21 PASS, including the
  Navita specimen pinned to the census coordinates.
  Bedrock-abilities bridge suites pass (the pack
  version pin in BridgeStatusTestMain was bumped
  v26 -> v27 with the version).

## Live-jar gate (STOP on mismatch)

At the window, re-pull plugins/ExcelEMFixes-*.jar and
STOP unless it is the v25 jar (per the Nov 25 install
record) or the v26 jar from the B1 packet — record the
pulled size + SHA-256 in RECEIPTS.md either way.
Marker re-checks before Shut Down and before Start
per standing discipline; BACKUP FIRST + cap-at-6;
emptiness `list` = 0/10 twice (weekend protocol if
Mike directs otherwise).

## Scope

Jar swap + full-config placement ONLY. Untouched:
quest-destinations.yml (the data rows are a separate
filing), every other plugin, all player data, all
EliteMobs content files. Rollback: restore the
outgoing jar (kept as .bak per house pattern) + the
config backup; the new toggle defaults on but the
module degrades to pre-v27 behavior with it off, so a
config-only rollback also exists.

## Boot verify

- Boot Done line present; SEVERE 0 attributable.
- "ExcelEMFixes v27 ready" line.
- "Quest Destinations ready —" line.
- Guild state unchanged (OPEN 5 of 5 at last record;
  verify, do not assume).
Marker on install:
~/workspace/excelemfixes-turnin-switch-20261126/packet-v27/INSTALLED.txt

## Same-pass surfaces (owed with the install)

See SURFACES.md in this packet: What's New BOTH
surfaces (website + in-game ExcelWhatsNew changelog),
website plugin row v26/v25 -> v27, Guide knowledge
(quest guidance teaches the turn-in switch), GitHub
release with detailed changelog (D247 plain version
v27; no star — this is a guidance fix, not a headline
release). Broadcaster tips + in-game menus checked:
no change needed (no new commands or menus).

## Proof owed after install

Iris-first per standing testing split: on the live
server, carry primis_main_quest_14 (or stage a copy),
complete its objectives, and confirm /questwhere +
beacon + glow all resolve to Navita at em_primis
596, 72, 772; plus one in-progress quest confirming
its destination does NOT switch early. Player proof
is not a gate (ship-it rule); Jester's next finished
quest is the natural live confirmation — report it,
do not assign it.
