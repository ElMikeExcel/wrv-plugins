# BUILD RECORD — ExcelEMFixes 0.21.0 (class-xp-sharing generalization)

Built: Nov 25–26, 2026 (build lane, 3rd dispatch).
Ruling: D243 (Mike, Nov 25, 2026, 6:14 PM CT) — extend
the 0.20.0 spellcaster XP-sharing toggle to ALL classes.
Extends: D242 / ExcelEMFixes 0.20.0 (caster-xp-sharing),
installed live Nov 25, 2026, 17:57 CT.
Working tree: this folder's build-root/ (a copy of the
0.20.0 build tree placed by the parent seat under
Mike's approval, Nov 25, 2026, 6:19 PM CT).

## What changed (0.20.0 -> 0.21.0)

The caster-xp-sharing module is generalized from the
single WANDS/STAVES pair to every EliteMobs class form.
The module id stays `caster-xp-sharing` (the id it
shipped under in 0.20.0) so its config section and its
data file carry over untouched; its display name is
now "Class XP Sharing".

- FORM-AWARE PAIRS: at XP-gain time the module
  resolves the player's ACTIVE form through EM's
  AdvancedCombatSystem profile (activeFormId, which
  accounts for the run-locked selection; selectedFormId
  as fallback), then looks the form up in an embedded
  pair table generated from the hidden-mechanics
  audit's probed 76-form catalog
  (reference/catalog-full.txt — the pair source of
  truth). Pairs change per form (audit correction
  F2): Berserker root = AXES+MACES, Cleric root =
  MACES+TRIDENTS, Paladin root = ARMOR+SWORDS; only
  Spellcaster is constant (STAVES+WANDS). A form the
  table does not know resolves to no pair and the
  module stands down for that gain.
- SAME SEMANTICS: STOCK untouched; SHARED = XP-neutral
  split of the pending gain (gained track keeps
  ceil(g/2), the pair's other track is granted
  floor(g/2)); CARRY = a gain on the leading track
  grants the full gain into the lagging track. Same
  EM-API route as 0.20.0 (EliteSkillXpGainEvent at
  MONITOR + PlayerData.addSkillXP for grants); no DB
  writes; grants are suppressed from re-entering the
  listener (no recursion).
- ONE GLOBAL MODE per player (unchanged). Surface:
  a "Class XP Sharing" row now appears on EVERY skill
  page of /skillmenu (0.20.0: Wands/Staves pages only),
  plus commands: /classxp (new, primary) with /casterxp
  kept as a working alias.
- CATCH-UP PER CLASS TREE: the first non-STOCK
  selection in a class tree not yet caught up raises
  the lagging track of the CURRENT FORM's pair to the
  leading track's current total. Trees are the six
  root lineages: adventurer, paladin, berserker,
  ranger, cleric, spellcaster. 0.20.0 discipline kept:
  grant first, re-read through the API and verify the
  lagging total landed, only then write the done-flag;
  a failed verify leaves the tree owed. Every catch-up
  writes a full audit record (timestamp, form, track,
  amount, both pair skills, both before-totals).

## Migration (hard requirement) — approach: IN CODE

Live 0.20.0 data (plugins/ExcelEMFixes/
caster-xp-sharing.yml) holds per-player modes and
flat spellcaster catch-up fields. 0.21.0's state
loader migrates in memory at load:

- mode carries over unchanged;
- a row with catch-up-done: true synthesizes the
  per-tree `catch-ups.spellcaster` record from the
  flat fields (at / track / amount / before-totals
  preserved) — a catch-up fired under 0.20.0 can
  NEVER refire;
- a row with catch-up-done: false gains no record —
  a catch-up still owed stays owed and fires on the
  player's first non-STOCK pick under 0.21.0.

On every save, the spellcaster per-tree record is
also mirrored back into the legacy flat 0.20.0 fields
(mapped by skill name, either pair order), so a
rollback to the 0.20.0 jar keeps honoring the same
records — no player can be caught up twice in either
direction. No packet-side data step is needed; the
installer changes no data files.

## Config

Packet deploy-config.yml = the LIVE 0.20.0 config
(80,048 B / 40f3ed44…) with only the caster-xp-sharing
section's comments rewritten (reason updated: D242 +
audit universality + Marvin's Paladin case + D243)
and its two player-facing message strings updated
("Class XP sharing set to…", "…one-time fix for this
class tree…"). Diff-verified: the entire diff sits
inside that one section (62 lines added, 36 removed);
every functional key and value in the file is
unchanged. Shipped defaults = WRV values (default-mode
STOCK, catch-up on); zero WRV hardcoding.

## Tests

- Full suite: **1,843 passed / 0 failed** on clean
  runs (0.20.0 baseline 1,259; +584 new/changed
  checks). One intermittent single failure can appear:
  the documented pre-existing SkillNudgeMathTest
  tie-break flake ("armor named first on the tie",
  Map.of salted iteration order), reproduced on the
  pristine 0.20.0 tree as well — unrelated to this
  module.
- New coverage: pair resolution for ALL 76 catalog
  forms (pair + tree per form) against
  reference/catalog-full.txt; the named F2 corrections;
  unknown/null form stand-down; the Marvin-shaped
  Paladin case (catch-up of ARMOR 24,853 vs SWORDS
  54,766 -> lagging ARMOR, amount 29,913); per-tree
  catch-up independence; migration (fired 0.20.0 row
  consumed with amount/track/before-totals preserved
  and stable across save+reload; owed row keeps its
  mode and stays owed); the legacy-mirror before-total
  mapping in both pair orders (added after the rig
  caught the STAVES-first gap — see Rig notes).

## Rig proof (local rig, Nov 25–26, 2026)

Rig: this folder's rig/ — Paper 26.1.2-74 + EliteMobs
10.9.14 (the live internal version) + the built
0.21.0 jar; mineflayer bots; all XP reads from EM's
own player_data.db after its flush. Test boss:
custombosses/classxp_test_boss.yml (zombie, level 12,
no AI, zero damage — EM pays a native ~+61 ARMOR on
every elite kill regardless of weapon, measured in
STOCK; assertions account for it).

- PALADIN (pair ARMOR+SWORDS), staged ARMOR Lv 10 /
  SWORDS Lv 13: STOCK sword kill +185 SWORDS / +61
  ARMOR (native only, twice). /classxp shared fired
  the catch-up: ARMOR 17,394 -> 52,083 = SWORDS total
  exactly (grant 34,689; audit record written);
  re-toggle fired nothing (once-only). SHARED kill:
  +123 ARMOR / +123 SWORDS, combined 246 = the native
  combined award (XP-neutral). CARRY kill: +246 /
  +246, gap constant at 0. PASS.
- BERSERKER (pair AXES+MACES), staged AXES Lv 12
  (37,023) / MACES Lv 5 (849): /classxp carry fired
  the catch-up: MACES 849 -> 37,023 exactly (grant
  36,174; berserker tree record). CARRY axe kill:
  +185 / +185, gap 0. SHARED: +93 / +92 = 185 native
  (XP-neutral). STOCK: AXES +185, MACES +0 exactly.
  PASS.
- SPELLCASTER REGRESSION — owed (bot "JesterOwed",
  pre-placed 0.20.0-format state row: mode STOCK,
  catch-up-done false; staged WANDS Lv 18 = 198,972 /
  STAVES Lv 12 = 37,023): /classxp shared fired the
  owed catch-up exactly once: STAVES -> 198,972
  (grant 161,949; spellcaster record + legacy mirror
  written). Re-toggle to CARRY: no refire, totals
  unchanged. PASS.
- SPELLCASTER REGRESSION — fired/migration (bot
  "JesterDone", pre-placed 0.20.0-format row: mode
  SHARED, catch-up-done true, track STAVES, amount
  162,150, wands-before 199,173, staves-before
  37,023; staged fresh WANDS 198,972 / STAVES 37,023):
  /classxp carry set the mode and fired NOTHING —
  STAVES stayed 37,023; the saved row preserves the
  original 162,150 record in both the per-tree map
  and the legacy fields. PASS — a 0.20.0-fired
  catch-up does not refire under 0.21.0.
- MENU: /skillmenu -> Swords page (a non-caster page)
  carries the "Class XP Sharing" row -> opens the
  module view (three mode buttons, current mode
  marked) -> clicking "Shared pool" set the mode
  (chat confirmation + state file mode SHARED).
  PASS. Bedrock parity rides the same Views/
  MenuRenderer surface as every module since 0.15.0.
- BOOT: "ExcelEMFixes 0.21.0 ready" with all 23
  modules incl. "Class XP Sharing ready — per-player
  foundation-skill sharing toggle for every EliteMobs
  class form (default mode STOCK, catch-up on)";
  SEVERE 0; pre-placed state rows loaded (record
  count reported at boot).

RIG NOTES: (1) EM's branch test-set command validates
the whole ancestor chain, so staging raised the
adventurer pair (ARMOR+SWORDS) before selecting
paladin/berserker/spellcaster; spellcaster selected
fine via the admin test-set despite EM's
FreeMinecraftModels warning. (2) The first rig save
exposed the legacy mirror writing -1 before-totals
for STAVES-first pair records; fixed in
CasterXpState.syncLegacyMirror (map by skill name,
either order), covered by two new unit checks, and
re-proven live in the JesterOwed re-toggle (saved
mirror: wands-before 198,972, staves-before 37,023).

VERDICT: PASS on every leg.

## Artifacts

- ExcelEMFixes-0.21.0.jar — 552,041 B / SHA-256
  4ecf2709ee41f08b2517184621c2b349ec2dbb21aaca5834d3b36e407fd5700a
  Deterministic: two consecutive builds byte-identical
  (verified again after the mirror fix).
- deploy-config-0.21.0.yml — 81,482 B / SHA-256
  949ff6d2423938eb93158cac667ae15a7f9186e4567bbab0a6298cd88342dc0f
  (ships in the packet as deploy-config.yml)
- Packet: packet-0.21.0/ (jar + deploy-config +
  INSTALL-PACKET.md + RECEIPTS.md + SURFACES.md +
  SHA256SUMS, sha256sum -c clean), filed as a PENDING
  entry in ~/workspace/wrv-install-queue/MANIFEST.md
  chaining on 0.20.0.

## Not done here (by design)

No install (the failsafe installs at the first
verified-empty moment), no player messages, nothing
upstream. Marvin's report-back rides the lane's D228
follow-on drop, gated on install + his catch-up
verified in EM player data if he toggles. Jester's
report-back is already wired from the 0.20.0 lane and
is not duplicated.
