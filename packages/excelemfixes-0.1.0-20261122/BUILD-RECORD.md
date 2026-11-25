# BUILD RECORD — ExcelEMFixes 0.1.0 (module 1: quest-tracker destinations)

Started: Sun Nov 22, 2026 (CT). Builder: subagent (parent-dispatched).
Task: new plugin ExcelEMFixes — standing home for WRV-side workarounds
to EliteMobs defects. Module 1 fixes the quest-tracker gap: EM quests
15, 18, 19 (and any instanced-boss quest) show no destination in EM's
own tracker because the tracker never loads an instanced boss's spawn
locations. Defect is inside EM compiled code (upstream issue #241,
already posted); EM's tracker cannot be repaired from outside, so our
plugin gives the player the destination itself.

Constraints in force:
- Ship-it standard: unit tests pass; jar loads clean on mirror rig
  (SEVERE 0); functional proof a player on quest 15 gets the correct
  destination from our surface. FILE the packet in MANIFEST PENDING.
  DO NOT install on live.
- ExcelEM naming law; modular structure mirroring ExcelEMCompanion
  (module = package implementing the module interface + one registry
  line; no core changes to add a module).
- Data-driven fix: config mapping quest -> destination covering
  15/18/19 and generalizing to any instanced-boss quest.
- Design law: travel inside EM areas stays stock EliteMobs — this
  fix gives information/compass guidance ONLY. No teleports, no /warps.
- Do NOT edit EliteMobs' own files. Never touch Shockbyte.
- Java + Bedrock parity for any menu surface; Back buttons on menus.

## Log

- 2026-11-22: record opened. MANIFEST PENDING format + Companion
  INSTALL-PACKET format read. Companion build folder surveyed
  (module pattern: CompanionModule / ModuleRegistry / ModuleContext;
  EmData.java is the EM data read path to reuse).

## Investigation (Nov 22, 2026 — live files pulled to livepull/)

Quest files (plugins/EliteMobs/customquests/primis/):
- Q15 primis_main_quest_15.yml "Severance payback!" (lvl 11):
  KILL_CUSTOM primis_pontis_mille_counselor_p1.yml + FETCH_ITEM
  Stone of Far Speech. Turn-in: Shogun Navita, Pontis Mille.
- Q18 primis_main_quest_18.yml "The trial of ice" (lvl 16):
  KILL_CUSTOM primis_ice_village_trial_of_ice_minor_ice_elemental.yml
  + primis_ice_village_fallen_trial_warrior_summon.yml.
- Q19 primis_main_quest_19.yml "Chill out" (lvl 17):
  KILL_CUSTOM primis_ice_village_the_ice_elemental.yml.

Mechanism (primary evidence): all three target bosses DO carry
spawnLocations, but every one points into an instanced world
(em_id_primis_counselor / em_id_primis_trial_of_ice /
em_id_primis_ice_elemental) — per-player instance worlds the tracker
cannot present as a destination in em_primis, so the tracker shows
nothing. Boss files: custombosses/primis/ (pulled). The fallen trial
warrior (Q18 objective 2) is a SUMMON of the minor ice elemental
(ON_HIT 0.66 + ON_COMBAT_ENTER) inside the same lair — same
destination as objective 1.

Instances (content_packages/, INSTANCED_DUNGEON, listedInTeleports
false — wormhole entry only):
- primis_counselor_quarters.yml — objectives include counselor_p1.
- primis_trial_of_ice.yml (LAIR) — objectives: minor ice elemental.
- primis_ice_elemental.yml — objectives: the ice elemental.

Entry wormholes (wormholes/primis/, location1 = where the player
stands in em_primis) — THE destination table for the fix:
- Q15: primis_pontis_advisor.yml -> em_primis 512.5, 42.0, 724.5
  (Pontis Mille). Matches the previously known-correct entry —
  VERIFIED from the live file, not just trusted.
- Q18: primis_ice_village_trial_of_ice.yml -> em_primis 1209.5,
  212, 1399.5 (Ice Village; path starts at Violet's house, follow
  the amethyst blocks to the eastern peak; wormhole at the peak).
- Q19: primis_ice_village_ice_elemental_arena.yml -> em_primis
  975.41, 42.0, 1655.54 (Ice Village; temple at the village summit).

Player-quest read path (reuse of Companion's proven EmData route):
PlayerData.getQuests(uuid) -> CustomQuest.getConfigurationFilename()
gives the active quest filenames. Rig seeding: CustomQuest
.startQuest(filename, player) exists in the EM API (javap-verified
on EliteMobs-10.9.10.jar, the compile-time lib; live is 10.9.14,
softdepend at runtime — same arrangement as ExcelEMCompanion).

## Design (decided)

- Core mirrors ExcelEMCompanion: FixModule interface + ModuleRegistry
  (one line per fix) + ModuleContext + shared Views/MenuRenderer/
  GeyserForms stack (Java chest GUI + Bedrock Cumulus, one content
  model, Back/Close chrome). Core command /emfixes (admin reload).
  Module commands register dynamically — a disabled module's command
  does not exist.
- Module 1: id "quest-destinations", command /questwhere (alias
  /questdest). Opens a menu of the player's OWN active quests that
  EM's tracker leaves blank; each entry's detail screen gives place,
  entry-wormhole coordinates in em_primis and step-by-step
  directions. Information only — no teleports (design law).
- Data-driven: bundled quest-destinations.yml (data-folder override
  wins, /emfixes reload applies) mapping quest file -> destination.
  Generalization rule documented in the file: any quest whose kill
  targets spawn only inside an instanced (em_id_*) world gets an
  entry pointing at its entry wormhole's location1.
- Join hint (config-toggleable, default on): a player who logs in
  with an active tracker-blank quest gets one line pointing at
  /questwhere.

## Rig seed path (solved Nov 22)

First full-scenario run: boot clean, SEVERE 0, module + commands
registered, fake player joined, EM data loaded — but
CustomQuest.startQuest(file, player) returned null for all three
quests. Bytecode shows why: startQuest's String argument is parsed
with UUID.fromString — it looks a quest up among the player's
PENDING (offered) quests by quest ID; it is not a filename entry
point. EM's real registration path (what acceptance does):
  fields = CustomQuestsConfig.getCustomQuests().get(filename)
  quest  = new CustomQuest(player, fields); quest.setAccepted(true)
  PlayerData.addQuest(uuid, quest)
PlayerData.getQuests reads the in-memory PlayerData.quests list
that addQuest appends to. Driver updated to seed this way.

## Scope addition 1 — glow guidance (Mike, Nov 22, ~07:42 CT)

"Use that thing where we made mobs glow" — FOUND: ExcelRaidBuff
0.1.1's stuck-wave RevealTracker (src: ~/workspace/excelraidbuff/
src/ExcelRaidBuff/.../ExcelRaidBuff.java, applyGlow/clearGlow).
The house approach, reused here rather than reinvented:
- glow = a plain PotionEffect(PotionEffectType.GLOWING, duration,
  amplifier 0, no particles/icon) on the entity;
- an in-memory map records exactly which entities THIS feature
  lit — it only ever clears its own glow, never anyone else's;
- clear-all on disable so nothing outlives the plugin.
ExcelEMFixes mirrors that: GlowGuide applies the same effect and
keeps the same tracked-set discipline. For a PLACE destination
(entry wormhole) it spawns a temporary marker armor stand at the
spot (invisible, invulnerable, non-persistent) carrying the same
GLOWING effect, removed at expiry; for an ENTITY destination it
lights the entity itself. Config: modules.quest-destinations.glow
.enabled / .duration-seconds / .search-radius. Guidance only —
still no teleports.

## Scope addition 2 — menu-callable surface (Mike, Nov 22, ~07:46 CT)

Players can re-trigger guidance from ExcelEMCompanion's /progress.
Our side exposes a clean command surface a menu click can invoke
for the player's own quests:
  /questwhere guide <quest file>  — destination text in chat + glow
  /questwhere show  <quest file>  — the directions screen
  /questwhere                     — the menu
FOLLOW-ON (not in this unit): a "guide me" button on the Companion
progress tracker's quest entries that runs this command — a small
ExcelEMCompanion update to ship next; noted in the packet too.

## Scope widening 3 — surface serves ExcelEMCompanion as a whole
(Mike, Nov 22, ~07:49 CT)

Glow guidance is not progress-only: from /drops, a player looking
at a boss or a place can glow that boss/NPC (or a marker at the
place) the same way. ExcelEMFixes therefore exposes a GENERAL
destination + glow surface, designed for both Companion modules:
- /questwhere guide <quest file> — quest destinations (progress).
- /questwhere glow <world> <x> <y> <z> [entity name] — any
  destination: the named entity near the point glows, or a marker
  at the point does (drop guide: a boss by name at its place, or
  the place itself).
- com.winkreyes.excelemfixes.api.GlowApi — a small static facade
  (glowMarker / glowEntity) for plugins that prefer native calls
  over running a command; no-ops cleanly when ExcelEMFixes or its
  glow is unavailable.
FOLLOW-ON UNIT (defined, not built here): an ExcelEMCompanion
update covering BOTH modules — a "guide me" action on progress
quest entries (runs /questwhere guide) and a glow action on
drop-guide boss/place screens (runs /questwhere glow or GlowApi).
Ships as a small Companion update next; this packet documents the
surface it will call.

## Expansion-generic by design (Mike, Nov 22, ~07:49 CT) — how

Standing direction: fixes are prepared for ALL future expansions,
not just Primis. How this build achieves it, and the one limit:

- PLAYER STATE is read from EliteMobs at runtime (PlayerData ->
  the player's own active quests, by configuration filename).
  No quest number, pack name or place exists in code — a grep of
  src/src for "primis" finds no logic (the data file and one
  javadoc example name today's rows; code never does).
- DESTINATIONS are a data file (quest-destinations.yml): quest
  file -> place, entry point, coordinates, directions. Covering
  a future pack's quest = adding a row. A copy in the plugin's
  data folder overrides the bundled one, and /emfixes reload
  applies it — no rebuild, no code change.
- COVERAGE IS SELF-CHECKING: TrackerBlankScan reads EliteMobs'
  own data (every customquests/*.yml + custombosses/*.yml in
  every pack folder) at enable and on reload, applies the
  defect-class rule (KILL_CUSTOM objectives whose bosses spawn
  only in em_id_* instanced worlds or nowhere naturally), and
  logs any tracker-blank quest the table does not cover yet, by
  filename. A new pack's blank quests announce themselves the
  day the pack lands.
- GLOW targets are data/call-time values: rows carry glow-mode
  (marker | entity) + glow-entity-name; the general surface
  (/questwhere glow, GlowApi) takes world + coordinates + name
  from the caller. No destination is baked into the engine.
- THE ONE LIMIT (flagged per the direction): place NAMES and
  step-by-step DIRECTIONS are human prose. EliteMobs' data
  carries filenames and coordinates, not "Pontis Mille, lower
  city by the market" or "follow the amethyst blocks". So a
  future pack's blank quests are DETECTED automatically and
  their entry coordinates are derivable from the pack's own
  wormhole files (location1, the method used for today's rows),
  but the row's words still get written by a human/agent when
  the pack is integrated — a data task of minutes, not a build.


## CLOSE-OUT — fold-ins, verification, filing (Nov 22, 2026, ~08:10–08:47 CT steers applied)

### Module inventory (per the 08:10 standard: defect/behavior, fix/feature, toggle, default, WRV value)

| Module | Defect / behavior (one line) | Fix / feature (one line) | Toggle | Default | WRV runs |
|---|---|---|---|---|---|
| quest-destinations | EM's quest tracker loads no spawn locations for instanced bosses, so those quests show no destination (upstream #241) | /questwhere gives the entry point (wormhole / teleporter NPC), coords + directions, from a data table; startup scan warns about uncovered tracker-blank quests | modules.quest-destinations.enabled (+ join-hint, + glow.enabled separately) | on (hint on, glow on) | shipped defaults + the 11-row Primis data table |
| arcane-bolt | EM computes ACS ability damage with no weapon scaling and never applies the caster's outgoing-damage buffs | measures each ability hit and adds the missing buff/scale as extra damage, capped per hit | modules.arcane-bolt.enabled | on | ability-damage-scale **1.5** (shipped 1.0) — WRV casters gear for spell power |
| drop-guide | (feature) EM has no in-game "what drops that?" | /drops: places → mobs → full drop list from a generated index of the server's own EM content; Glow-the-way-in buttons via runtime entry-point resolution | modules.drop-guide.enabled | on | generated WRV index as data |
| progress-tracker | (feature) EM tracks everything about a player and shows almost none of it | /progress (/tracker): the player's own record from EM's data + generated catalog; guide buttons into quest-destinations for tracker-blank quests | modules.progress-tracker.enabled | on | generated WRV catalog; kill-records-folder = "ExcelProtect" (shipped: empty) |
| food-heal | EM worlds: food barely heals vs ACS health pools; EM's OOC trickle is a hardcoded 1%/s | food grants a saturation-sized heal budget (1% instant + 0.5%/s OOC) plus a +1%/s base OOC bonus (2%/s total) | modules.food-heal.enabled | on | debug ON until first live proof (shipped off); worlds lists em_primis + em_adventurers_guild (shipped: empty, EM registry covers) |
| leash | EM's native leash soft-returns at half radius, makes returning bosses invulnerable, never clears target | native leash suppressed in memory; module runs the D183 leash: normal within range, deaggro + walk home + heal on arrival, damage re-aggroes | modules.leash.enabled | on | range 96 (= default, Mike's pick); debug ON until first live proof (shipped off) |
| seal-restore | EM content whose onRemove restore list is shorter than its onSpawn seal list leaves arena blocks sealed forever | on the listed boss's death, registered blocks still wearing their seal state are restored; data rows only, nothing hardcoded | modules.seal-restore.enabled | on (no rows = no-op) | one row: Camp Warden p4, 30 blocks, generated from EM's own files |
| primis_sleep WakeUpOnDamage | a reset (sleeping) elite that takes damage never re-aggroes (upstream #242) | **STAYS a content fix** — a one-block patch inside EM's own power file using EM's power-event primitives; a plugin equivalent would reimplement the power system against state EM does not expose, and would double-wake. Documented in the README for owners to apply to their own copy | n/a (content) | installed Nov 21 | unchanged by this pack |
| Camp Warden door-seal | (was the D188 content edit, p4 onRemove 15→30) | **CONVERTED to the seal-restore module** per the 08:14 ruling; the packet reverts the content file to stock in the same window so the module is the single fix of record | (seal-restore) | — | — |

Non-qualifying inventory (checked, NOT folded): ExcelEMQuestXP and
ExcelEMWeekendXP are XP-rate features, not defect fixes — stay
standalone. ExcelEMTogether / ExcelEMRaidBuff are gameplay features
(RaidBuff's RevealTracker was the house glow *source*, reused as an
approach, not folded) — stay standalone. The dungeon lockout cut
(720→120) is an EM config value, not a plugin fix — stays config.
ExcelShops' protection behavior is that plugin's own domain.

### Arcane outcome (per the 08:02 steer)

**Integrated as a module in this packet** (it landed before filing):
`excelemfixes.arcane` (ArcaneBoltModule + CompensationMath +
EmInternals), registered in ModuleRegistry, config section merged
with the both-numbers documentation (scale default 1.0 / WRV 1.5).
Its 21 math unit tests are in the pack suite. Its mirror proof was
the Arcane lane's standalone-vehicle proof; the live proof
P-ARCANE-BOLT-LIVE-1 is named in the packet.

### Glow wiring inside the folded modules (08:08 fold-in scope)

- Progress Tracker: Quests Remaining renders a guide button for every
  quest the quest-destinations module covers (action
  `tracker:guide:<file>` → destination text via `/questwhere show` +
  GlowApi glow of the entry). No button when the quest module is off.
- Drop Guide: new `EntryPoints` resolver reads EM's own data at
  runtime (boss spawns → content-package worldName → wormhole
  location1 / teleporter-NPC `em dungeontp`), keyed by the index
  generator's mob-id slug. Place screens and mob screens get a
  "Glow the way in" button when an entry resolves and GlowApi is
  available. On the WRV tree it resolved **151 mobs**.
- Glow engine is the house approach (ExcelRaidBuff RevealTracker):
  plain GLOWING potion effect / marker armor stand, in-memory record
  of exactly what we lit, chunk ticket held for marker lifetime.

### Verification (final jar 777d2c6a…, deterministic ×3 builds)

- Unit tests: **444/444 PASS** (15 suites, incl. ported Companion,
  FoodHeal and Leash suites and the new SealRestoreData suite).
- Mirror rig (Paper 26.2 + EM 10.9.10 + live EM tree), scenario
  "full": **51/51 PASS, SEVERE 0** — receipts/rig-full.json. Includes:
  quest surface end-to-end (seed via EM's real addQuest path, menus,
  marker glow + expiry, entity glow, coverage scan uncovered=[]);
  **Jester tracker reproduction through the folded module: 63
  completions / 61 distinct of 111 / main chain 26 of 31 / 20 runs /
  1,622 kills / highest 25 / 1 death / 8 downed**; /progress + /drops
  open; tracker guide dispatch glows the Advisor wormhole; drops glow
  dispatch glows a resolved entry (151 resolved); seal-restore: a
  real EM boss (permanent regional spawn of test_boss.yml) died and
  the module restored the sealed fixture blocks, leaving a
  non-seal-state block untouched.
- Scenario "modoff" (every module disabled): **3/3 PASS, SEVERE 0** —
  no module commands exist, /emfixes unaffected.
- Leash + FoodHeal behavioral proof on the rig is by code identity
  (byte-moved classes from the standalones whose own rigs proved
  them) + module boot/command checks here; their first-natural-case
  live proofs are named in the packet (P-LEASH-BOSS-AGGRO-181,
  P-JESTER-OOC-HEAL-1), matching how the standalones were filed.

### Distribution bar (08:03/08:05/08:10/08:11 steers)

- Package renamed to `excelemfixes`; shipped config is per-module
  banner-headed sections, each with defect-then-fix comments, its
  toggle and knobs; shipped data files are a blank template
  (quest-destinations), empty stubs (drops-index, tracker-catalog)
  and an empty table (seal-restore). WRV's values live only in
  `deploy/` (deployed config + data files) and are documented in the
  README's "What Wink Reyes Valley runs differently" table and in
  config comments beside each tuned knob.
- Post-refactor grep of shipped code + defaults for WRV strings
  (winkreyes / em_primis / Primis / Jester / ExcelProtect /
  ExcelMenu): **zero hits in logic or player-facing strings**. The
  tracker's ExcelProtect coupling found by that grep was removed:
  per-boss kill records now come from a `kill-records-folder` config
  knob (shipped default empty = no counts; WRV sets "ExcelProtect").
  Remaining "Wink Reyes Valley" mentions are only the required
  both-numbers disclosure comments. `em_id_*` appears as what it is —
  EliteMobs' own instanced-world naming convention.
- README rewritten for outside owners: plugin-box framing, install,
  per-module defect-then-fix, toggles with defaults, both-numbers
  table, GlowApi notes, and the house rule "fixes live in the box"
  (08:14 standing rule).

### Filing

Packet: INSTALL-PACKET.md (this folder). MANIFEST: PENDING entry
"ExcelEMFixes 0.1.0 (Nov 22, 2026)". NOT installed by this lane.

## Mike's approval (Nov 22, 2026, ~08:55 CT)

Mike 👍-approved the build report as presented, including the flagged
judgment call: primis_sleep WakeUpOnDamage STAYS a content fix, not a
module (the one D193 exception, now ratified by Mike). QuestXP,
WeekendXP, Together and RaidBuff staying standalone as features was
approved in the same reaction.
