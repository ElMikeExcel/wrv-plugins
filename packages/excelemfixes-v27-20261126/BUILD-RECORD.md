# BUILD RECORD — P-GUIDANCE-TURNIN-SWITCH-20261126 (FIX-LIST B2)

Built Nov 26, 2026 by a follow-on relay (D228) worker
lane. Source spec: ~/workspace/quest-guidance-reaudit-20261126/FIX-LIST.md §B2 (+ AUDIT.md / census.csv).

## What was built

The turn-in-phase destination switch in ExcelEMFixes:
when a tracked/carried quest's objectives are ALL
complete, the resolved destination switches to the
turn-in NPC's spawn (the quest file's turnInNPC, else
the quest giver). The switch is made at the ONE shared
resolution point — QuestDestinationModule
.resolveDestination, which the quest beacon reads
through QuestDestinationApi.destinationFor(file,
player) and /questwhere + the glow guide read
directly — so beacon, /questwhere and glow all move
together, per the FIX-LIST's shared-point requirement.
Unlike the 0.19.0 dialog derivation, the switch
outranks a destinations-table row (a row encodes the
objective phase, which is spent once no objective
remains; FIX-LIST A4 explicitly leaves main_quest_14's
turn-in hole open under its row, and B2 is what closes
it). When the turn-in NPC cannot be resolved (unknown,
disabled, no parseable spawn), no switch happens and
the pre-v27 resolution stands — never a guess.

Version: **v27** (D247 plain v-number). Base: the v26
build tree (FIX-LIST B1+B6 fetch derivation, itself
not yet filed at build time — see Filing state), so
v27 is a superset of v26. plugin.yml v26 -> v27.

## Files changed

Build tree: ~/workspace/excelemfixes-turnin-switch-20261126/build-root
(copied from the v26 tree at
~/workspace/excelemfixes-fetch-derivation-20261126/build-root;
post-copy diff vs base = exactly this set)

NEW:
- src/src/excelemfixes/questdest/TurnInDerivation.java
  — pure policy (the 0.19.0 DialogDerivation pattern):
  resolve(enabled, allComplete, questFile, questName,
  spawn) + the table-shaped derived row (glow-mode
  entity, target "Turn in to <NPC>", turn-in
  directions text).
- src/src/excelemfixes/questdest/EmTurnInData.java
  — EM bridge (the EmDialogData/EmFetchData pattern):
  allObjectivesComplete = >=1 objective and every
  objective isObjectiveCompleted(); turn-in NPC =
  CustomQuest.getCustomQuestsConfigFields()
  .getTurnInNPC(), else the giver scan over
  NPCsConfig (enabled NPC whose getQuestFilenames()
  names the quest, sorted-filename deterministic).
  Spawn resolution reuses EmDialogData.spawnFor.
- src/tests/com/winkreyes/excelemfixes/TurnInDerivationTest.java — 21 checks incl. the
  specimen pinned to the census coordinates.
- deploy-config-v27.yml — full deploy config (= new
  src/config.yml, the v26 lane's deploy pattern).

EDITED:
- questdest/QuestDestinationModule.java — new
  deriveTurnIn field + loadSettings read of
  derive-turn-in-destinations; resolveDestination
  consults the turn-in state BEFORE the table row;
  javadoc rewritten to document the phase order.
- api/QuestDestinationApi.java — javadoc: the v27
  switch at the shared facade.
- src/config.yml — derive-turn-in-destinations: true
  block with the full why (quest-destinations
  section). Toggle off = exact pre-v27 behavior.
- src/plugin.yml — version v27. build.sh — jar name
  ExcelEMFixes-v27.jar. run-tests.sh — TurnInDerivation
  added to the compile list. TestMain.java —
  TurnInDerivationTest registered.
- tests bedrockabilities/BridgeStatusTestMain.java — pack version pin bumped v26 -> v27 (the
  standard companion of a version bump).
- README.md — "Turn-in destination switch (v27)"
  section with the creation reason + the new toggle
  bullet (fleet convention: reason in README + config).

No WRV quest/NPC/place is named in code; shipped
defaults carry no WRV data beyond what config already
held. No other module touched.

## Build / test results

- ./build.sh: PASS. ExcelEMFixes-v27.jar — 682,311 B,
  SHA-256 6c7726b450f579ea88db5626f960fe71ef543b120
  cac8776b49447ff5442f00e. Deterministic: two
  consecutive builds byte-identical (same hash); jar
  verified to contain plugin.yml version v27 and the
  new TurnInDerivation / EmTurnInData classes.
- ./run-tests.sh (full plain-JVM suites + bedrock
  bridge runs): TOTAL 2,137 passed / 1 failed.
  - TurnInDerivationTest: 21/21 PASS.
  - The 1 failure — SkillNudgeMathTest
    "Jester-shaped: armor named first on the tie"
    (expected ARMOR, actual WANDS) — is PRE-EXISTING
    in the v26 base tree: the skillnudge sources and
    that test are byte-identical between the base
    snapshot and this build (verified by diff), and
    v27 touches nothing in that module. Flagged for
    the supervisor/B1 lane; not fixed here (out of
    this item's scope).

## Specimen verification (primis_main_quest_14)

Verified logically against the code path + the
re-audit's live-pull content files:
- Quest file (live-pull/customquests/primis/primis_main_quest_14.yml): 5 KILL_CUSTOM objectives;
  turnInNPC: primis_pontis_mille_shogun.yml.
- NPC file (live-pull/npcs/primis/primis_pontis_mille_shogun.yml): name Navita;
  spawnLocation em_primis,595.5,72.0,771.7.
- Code path: all 5 objectives complete ->
  EmTurnInData.allObjectivesComplete true ->
  turnInNPC primis_pontis_mille_shogun.yml ->
  EmDialogData.spawnFor -> NpcSpawn(Navita, em_primis,
  595.5, 72, 771.7) -> TurnInDerivation row at that
  spawn, glow-mode entity on Navita. 595.5/771.7 round
  to 596/772 — destination = em_primis 596, 72, 772,
  exactly the FIX-LIST B2 specimen. The unit test pins
  both the exact spawn and the rounded specimen.
- Phase check: while any objective is outstanding,
  the switch stays silent and the quest keeps its
  objective-phase destination (the A4 table row at
  the Storm Temple wormhole once filed, or EM's own
  tracker) — pinned by the not-all-complete test.

## Filing state — HANDED TO THE BOARD SUPERVISOR CHAIN
(packet prepared; MANIFEST entry NOT added)

Everything install-shaped is staged and ready:
~/workspace/excelemfixes-turnin-switch-20261126/
packet-v27/ (INSTALL-PACKET.md with lint recorded,
candidate jar + deploy-config.yml, SHA256SUMS,
RECEIPTS.md, SURFACES.md). The MANIFEST pointer entry
was NOT added by this lane, for two named reasons:

1. BASE NOT FILED: v27 is stacked on v26 (B1), which
   at build time had no packet, no jar and no MANIFEST
   entry — its lane was still mid-build. Filing v27
   now risks the duplicate/supersede tangle the fleet
   hit with 0.22.0 (Nov 25). The packet carries the
   ORDERING rule for whoever files: v27 supersedes
   v26 if v26 is still pending; never install v26
   after v27.
2. G1 RECEIPTS OWED: the packet template requires
   same-day live-pull receipts for the live-jar gate,
   and this lane's hard limits bar live-server/panel
   actions. RECEIPTS.md names exactly which pulls the
   filing lane must take and append before adding the
   MANIFEST stub.

Supervisor chain to finish filing: (a) confirm the
B1/v26 lane's end state (filed, installed, or still
open); (b) take the two same-day pulls (live
ExcelEMFixes jar + config) into RECEIPTS.md; (c) add
the PENDING pointer stub to
~/workspace/wrv-install-queue/MANIFEST.md naming
packet ~/workspace/excelemfixes-turnin-switch-20261126/packet-v27/INSTALL-PACKET.md, candidate
6c7726b4…, and the ORDERING rule. No install was
performed by this lane; no live server or panel was
touched.

## Surfaces

Code-side surfaces are in the build (README, config
comments). Player-facing surfaces owed WITH the
install pass are listed in packet-v27/SURFACES.md
(What's New both surfaces, website row, Guide,
GitHub v27 release with detailed changelog) — they
belong to the installing pass per the same-pass rule,
and are named there so they cannot be lost.
