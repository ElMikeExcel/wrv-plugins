# Ghast Flying Mount — ExcelEMFixes summon-mount extension (D204)

- Status: BUILD COMPLETE Nov 23, 2026 — desk/mirror work only. NOT installed, NOT filed to MANIFEST (parent files after this record lands). NO player messages, NO player-facing surfaces — Mike's instruction: players must NOT know yet. Secrecy is a hard constraint on this lane.
- Approved: Mike, Nov 23, 2026 ~03:42 CT (D204); carry-and-toggle form confirmed ~03:44 CT.
- Lane: ~/workspace/ghast-mount-20261123/
- Base tree: melee-mitigation lane's verified ExcelEMFixes 0.7.0 tree (~/workspace/melee-mitigation-20261123/build-root/), copied to build-root/ (base untouched). 0.7.0 is the live pack version (installed Nov 23, 2026 20:02 CT).

## Spec implemented (D204)
- A flying ghast mount, the premium tier above the Mount Charm horse ($15,000 list / $11,250 NPC per D202).
- Price: $30,000 list / $22,500 guild-vendor NPC (standing 25% NPC discount, D200). Both Excel Bucks via Vault.
- GATE: purchase requires the player to be halfway through the quests in Primis OR in any single expansion (either path qualifies). Checked at purchase.
- Carry-and-toggle summon item like the horse: right-click toggles summon/despawn, item never consumed.
- Implementation: extend the ExcelEMFixes `summon-mount` module (the horse's home) with a second mount; the ghast charm registers in ExcelItems at the next free ID (horse is ID 5).
- All numbers config knobs. Public-fix-pack discipline: generic, data-driven, zero WRV hardcoding in code; WRV values in deploy config. Module documentation rule: plain-language explanation in the config section AND the README.

## Phases
- [x] Gate census — per-package quest counts + halfway thresholds from EliteMobs quest data (D204's own precondition: numbers land here BEFORE the gate is built) — DONE, census section below.
- [x] Build — module extension (item, service, buy flow + gate, config, README) — DONE Nov 23, 2026: pack 0.7.0 → 0.8.0. GhastItem (ID 9), GhastMountService (runtime-resolved HAPPY_GHAST, reflection harness, CUSTOM spawn), EmQuestGate + pure QuestCensus/QuestGateMath core, GhastSettings, /ghastcharm command, BuyFlow ghast flow over a settle core shared with the horse, CombatMarks shared by both services (one combat state per player across charms). Config sections + README written in both shipped and deploy configs.
- [x] Automated tests — DONE Nov 23, 2026: mount suites 103 passed / 0 failed (53 new checks: GhastItem shape, ID-9 registry trap both directions, census against the real 112-file fixture, gate math 52-refuse / 53-pass); pack suites 935 passed / 0 failed (one pre-existing flake in the untouched base tree — "Jester-shaped: armor named first on the tie", a tracker tie-break check — fires intermittently there too, 2 of 3 runs on the pristine 0.7.0 tree; unrelated to this lane). Deterministic build: two builds byte-identical.
- [x] Mirror/rig load proof (module on + off, SEVERE 0 both) — DONE, rig section below (4 boots).
- [x] Rig buy-flow + gate proof (below halfway refused; at/above can buy; NPC $22,500 vs list $30,000) — DONE, rig section below.
- [x] Rig summon/ride/despawn proof (as far as the rig allows; flight feel = live proof owed, never gates) — DONE, rig section below.
- [x] Final record — jar hash, ExcelItems ID, install packet inputs — below.

## Build output (0.8.0)
- Jar: `build-root/ExcelEMFixes-0.8.0.jar` — 422,898 bytes — SHA-256 `a5cb921da4c4188f8901e21bdea4dadfcfb199191477831c2fcf3f89341c71ef` (deterministic: two consecutive builds of the final tree byte-identical).
- ExcelItems ID taken: **9** (key `ghast_charm`). Lineage: 1–4 ExcelItems builtins, 5 mount_charm, 6 field_bandage, 7 strong_healing_potion, 8 atlas (ExcelAtlas, next-free mint at its Nov 23, 2026 05:37 boot) → 9 was next free. The ID-9 config-assignment trap is unit-proven both directions (GhastRegistryTest): NO `9: ghast_charm` line may be added to ExcelItems' config.yml while ExcelItems is 0.1.0 — it would disable ExcelItems at its own enable.

## Rig proof (mirror stack: Paper 26.2 + EliteMobs full content + ExcelItems 0.1.0 + Vault/EssentialsX; receipts in `build-root/receipts/rig-ghast-*.json`)
Driver: `build-root/rig/gdriver` (GhastRig, fake players on the fleet NMS pattern). Config under test = the DEPLOY config with two rig-only shortenings (combat window 3 s, ghast loss cooldown 3 s); prices and gate at deploy values. Quest completions were seeded as ONE `PlayerData.updatePlayerQuestCooldowns` write per fake player — the same call EM's own completion path ends in — and verified read back through the gate's own read path before any buy phase ran. (First rig run taught why: EM's per-completion `addCooldown` is a read-modify-write that, for a player whose data is not loaded in memory, overwrites the registry blob with a one-entry object on every call — bulk-seeding through it leaves exactly one completion standing. Noted here so no future lane re-learns it.)

- **Boot "on" — 30 passed / 0 failed, SEVERE 0.** Registry: 5=mount_charm, 9=ghast_charm. Module census log line: `ghast quest-gate census — primis=105 quests`. Gate: 52/105 completions → buy+confirm charged nothing, delivered nothing (GATE-refuse); the SAME player then bought the horse at list $15,000 (control — the buy machinery was live; only the gate refused). 53/105 → list purchase charged exactly $30,000 and delivered the charm; repeat buy charged nothing (already owned). Console-armed NPC purchase charged exactly $22,500. Gate-passing player with a $300 balance → no charge, no charm (funds checked before withdrawal). Summon through the real item event: HAPPY_GHAST spawned with the CUSTOM reason (spawn watch: not cancelled), PDC ghast mark + owner set, invulnerable, non-persistent, custom name, **RED_HARNESS read back from the BODY equipment slot**. Ride: owner boards; a non-owner's VehicleEnter is cancelled. Second right-click despawns. Combat: a damage mark barred BOTH charms (shared combat state proven behaviorally); after the window the ghast answered again. In em_primis: the ghast summoned via CUSTOM, not cancelled, while a NATURAL zombie control spawn WAS cancelled by EliteMobs.
- **Boot "ghastoff" (ghast.enabled: false) — 4/0, SEVERE 0.** ID 9 not registered, /ghastcharm does not exist, horse untouched.
- **Boot "modoff" (module disabled) — 4/0, SEVERE 0.** Neither charm registered, neither command exists.
- **Boot "noitems" (ExcelItems jar removed) — 2/0, SEVERE 0.** Module stood down (boot log carries the stand-down warning; neither command exists; pack fully up).

**What the rig could NOT prove** (live proof owed after install, never gates): actual flight feel and rider steering (vanilla client-input flight — a server-side fake player sends no flight input; the rig proved the rideable entity, the harness it requires, and boarding, not the flying itself); Bedrock-client rendering of the happy ghast + harness; the ghast's idle drift/wander behavior over time; performance with a 4-block entity in Primis content.

**One design fact the rig caught and fixed:** the first summon attempt failed closed (by design) because the harness equip probed a `getInventory()` the happy ghast does not have — Paper's HappyGhast API is a bare Vehicle/Animals interface. The harness is vanilla's BODY equipment slot (harness items are body-equippable, like wolf armor); the equip now uses plain `EntityEquipment` API with read-back verification, and the rig proves RED_HARNESS sitting in that slot.

## Install packet inputs (for the parent's filing — this lane files nothing)
1. **Jar swap:** `build-root/ExcelEMFixes-0.8.0.jar` (422,898 bytes, SHA-256 `a5cb921da4c4188f8901e21bdea4dadfcfb199191477831c2fcf3f89341c71ef`) replaces ExcelEMFixes-0.7.0.jar. Standard reversible pack swap; 0.7.0 jar is the rollback.
2. **Config:** apply the `ghast:` subsection of the summon-mount section from `build-root/deploy/config.yml` to the live ExcelEMFixes config. It is the ONLY config change in this lane (prices 30000 / 22500, gate on, fraction 0.5, one package: primis / prefix `primis_`, entity HAPPY_GHAST, harness RED_HARNESS). If the live config lacks the subsection entirely the module still loads with shipped defaults — but the gate then has NO packages and the charm cannot be bought (the enable log says so), so the subsection must land with the jar.
3. **ExcelItems: NO change, and one explicit warning** — do NOT add a `9: ghast_charm` line to ExcelItems' config.yml (0.1.0 trap, unit-proven: it disables ExcelItems at its own enable). ID 9 registers through the API at module enable.
4. **Vendor NPC wiring (not in this lane's files):** the guild mount vendor (shepherd stall) gains a Ghast Charm good at $22,500 whose interaction is a COMMAND interaction running the console form `ghastcharm buy %player_name% npc` — mirroring the Mount Charm's wiring exactly (EM-native shops charge Elite Coins and cannot be used).
5. **ExcelShops listing (not in this lane's files):** a Ghast Charm listing at $30,000 wired to the player-form buy (`ghastcharm buy` as the player), mirroring the Mount Charm listing.
6. **Secrecy (Mike's instruction):** NO What's New, website, or Guide surfaces in this packet — the tier goes in unannounced; surfaces are a separate later decision.
7. **Post-install verification:** enable log must show `ghast quest-gate census — primis=105 quests;` and the ghast tier line (registry id 9, $30000/$22500, gate on); `/ghastcharm` exists; a below-gate account is refused with its real progress quoted (e.g. "52 of 105 … 53 needed" shape); flight feel is the live proof owed (see rig limits).

## Gate census (per-package quest counts + halfway thresholds)
DONE Nov 23, 2026, before any gate code — source: the mirror's live EliteMobs content copy (melee-mitigation lane rig, `plugins/EliteMobs/customquests/`), cross-checked against the pack's own generated tracker catalog (deploy/tracker-catalog.yml, 111 quests).

Quest identity = bare filename (EM's own key: every quest file carries `questLockoutPermission: elitequest.<filename>`). A player's completed set = the quest-cooldown registry (`PlayerData.getPlayerQuestCooldowns` → `elitequest.<file>` permissions) — the pack's already-proven read path (tracker module, EmData: "quests finished at least once").

| Package | How identified | Quests (enabled) | Halfway threshold |
|---|---|---|---|
| **Primis** | files under `customquests/primis/`, all prefixed `primis_` | **105 / 105 enabled** (incl. the 31-quest main chain) | **53 completed** (ceil of 105 × 0.5) |
| Any single expansion | — | **none exist in current content** | — |
| (not a gate package) Adventurer's Guild hub | root files prefixed `ag_` | 5 enabled | — |
| (not a gate package) seasonal | `xmas_quest_0.yml` | 1 enabled | — |
| (not a gate package) test | `test_quest.yml` | 1 enabled; already excluded from the pack's own tracker catalog (112 files vs 111 catalogued) | — |

Findings:
- Primis is the ONLY quest package in the live content. No other expansion ships quests yet, so today the gate's qualifying path is Primis; the "OR any single expansion" path is implemented generically (below) so a future expansion enrolls by config alone.
- The measure is clean and per-package: totals are counted from EM's own quest files at runtime (recursive scan of `customquests/`, `isEnabled: true`, `.bak` skipped), grouped by package filename-prefix rules held in config; completions come from the cooldown registry. Nothing is hardcoded: if Primis gains quests, its total (and threshold) moves on its own.
- Gate rule: for at least one configured package with total > 0, completed ≥ ceil(total × fraction), fraction = config `gate.fraction` (shipped default 0.5 = halfway). WRV deploy config defines one package: id `primis`, prefix `primis_`.

## Open notes
- The two charms are independent: a player may own both, and each toggles its own mount; the shared rules are the combat state (one per player across charms — proven on the rig), the world gate, and the retirement switch. Summoning either is refused while riding anything (the horse's standing rule).
- The ghast keeps vanilla happy-ghast AI when unridden (it hovers/drifts as the vanilla entity does); no plugin steering exists by design — a ridden harnessed happy ghast is flown by the rider's own client controls.
- Gate packages are config-only from here on: when a future expansion ships quests, adding its prefix entry to `ghast.gate.packages` in the deploy config enrolls it — no rebuild, no re-census needed (totals are counted at enable/reload from EM's files).
## Speed fix + 0.7.1 re-base (Nov 23, 2026, evening — post-filing amendment)

Mike, Nov 23, 2026 ~8:58 PM CT: "The last ghast we had as a mount
moved slower than the horse. Double check and make sure it goes
slightly faster than the horses." He was right. The first 0.8.0
build rode at vanilla happy-ghast speed; vanilla is slow. Two
changes landed on the filed build, and the filed jar changed:

### The numbers (double-checked, with method)

- **Vanilla happy ghast, ridden: 3.611 blocks/second.** Measured
  on this lane's mirror rig (Paper 26.2) by stepping the game's
  own travel code (method note below); the same figure is the
  Minecraft Wiki's published measured cruise for a ridden happy
  ghast, reproduced to the third decimal. Attribute basis
  (read on the rig from a spawned entity, and from the Paper
  jar's bytecode): FLYING_SPEED base 0.05.
- **The module's horse: ~12.2 blocks/second.** Its MOVEMENT_SPEED
  is 0.29 (read back from the rig-spawned horse; JUMP_STRENGTH
  0.85 likewise). The ridden-horse conversion from internal
  speed units to blocks/second is 42.16 — an in-game-measured
  factor (minecart-comparison testing recorded on the Minecraft
  Wiki's transportation research; the wiki's own table entry
  0.28125 → 11.86 blocks/s implies the same factor), so
  0.29 × 42.16 ≈ 12.23 blocks/second. The rig could not step
  the horse's ridden travel directly (its travel override
  behaves differently under the client-authoritative gate —
  see method note), so the horse figure is attribute-derived
  from that measured conversion; the ghast figures are direct
  rig measurements.
- **Why the ghast is quadratic:** in this Minecraft version a
  player-ridden vehicle is client-authoritative — the server
  never simulates its travel (bytecode: `Player.isClientAuthoritative()`
  is unconditionally true; `travelRidden` zeroes the delta when
  movement cannot be simulated). The client runs the same travel
  code the rig stepped: the rider-input vector is scaled by
  3.9 × flying-speed AND the travel acceleration by
  flying-speed × 5/3, so cruise speed grows with the SQUARE of
  the attribute: blocks/second ≈ 1444.4 × attribute² (at 0.05 →
  3.611, matching both measurements). Web claims that doubling
  the attribute merely doubles ghast speed are wrong — it
  quadruples it.
- **Chosen value: `flying-speed: 0.097`** in the deploy config's
  `ghast:` subsection → predicted 13.59 blocks/second.
  **Rig-measured on the final jar: 13.590 blocks/second** —
  **+11.2% over the horse**, inside Mike's "slightly faster"
  band (+5–15%) under either horse conversion (nominal 43.17
  would put the horse at 12.52 and the margin at +8.6%).
  The spawned ghast's FLYING_SPEED reads back **0.097** on the
  rig; the horse is unchanged (0.29 / 0.85 read back).

Method note (rig "speed" scenario, receipt
`build-root/receipts/rig-ghast-speed.json`): because the server
will not simulate a player-ridden vehicle, the probe drives the
vehicle's own `travel(getRiddenInput(rider, ZERO))` once per
tick with a forward input held — exactly the calls the client
makes — carrying the travel delta across ticks by hand (the
server's zeroing would otherwise eat the friction carry-over).
Every measured number is produced by the game's own
travel/friction code on this stack; vanilla reproduced the
published 3.611 exactly, before and after the fix. The horse's
ridden branch would not displace under the same stepping
(delta accumulates, position does not), so its figure stays
attribute-derived as stated above. Flight at 0° pitch,
200-tick samples after an 80-tick warm-up.

### What changed

- **Deploy config only for the speed itself** — the knob already
  existed (`GhastMountService.applyFlyingSpeed` sets the
  FLYING_SPEED attribute at summon when `ghast.flying-speed` >
  0); the fix sets the deploy default to **0.097** with the full
  reasoning in the config comments, expands the shipped
  config's comment (shipped value stays 0.0 = vanilla, per the
  pack's shipped-vs-WRV discipline), and documents the knob in
  the README (ghast tier section + settings table). No Java
  code changed for speed.
- **Re-base onto 0.7.1 (parent directive, Nov 23, 2026 9:14 PM
  CT):** ExcelEMFixes 0.7.1 went live at 9:06 PM CT (the
  `emfixes reload` NPE fix, lane
  `~/workspace/emfixes-reload-fix-20261123/`). The fix — one
  branch in `ExcelEMFixes.syncModules`: reload the ENABLED
  module instance instead of the fresh registry instance whose
  ctx is null — was ported verbatim. Verification: after the
  port, this tree's `ExcelEMFixes.java` differs from the 0.7.1
  lane's file ONLY in the VERSION constant ("0.8.0"); a full
  tree diff confirms no other 0.7.1 delta exists. The deploy
  config was also re-aligned to the 0.7.1 live config: outside
  the `ghast:` subsection it now differs from the 0.7.1 deploy
  config (`b1c4b791…`, the live config) by nothing — including
  food-heal `debug: false`, the TP-26 flip that went live with
  0.7.1 (the 0.7.0-based deploy config still carried `true`).

### Final artifacts (supersede the Build output section above)

- Jar: `build-root/ExcelEMFixes-0.8.0.jar` — **423,069 bytes —
  SHA-256 `5c9d6ac15b8eb0e49624a71f84792442fbeb911da8594090f555416788117dd7`**
  (deterministic: two builds byte-identical). History:
  `a5cb921d…` (original 0.8.0, vanilla speed, 0.7.0 base) →
  `aa9fd230…` (speed docs, 0.7.0 base, never filed onward) →
  **`5c9d6ac1…` (speed + 0.7.1 re-base — the filed jar).**
- Deploy config: `build-root/deploy/config.yml` — 55,237 bytes,
  SHA-256 `f15c811980c82d32f20a26cb277d5449eef309a9a365e5cffcf07cf0383e68e8`
  (= 0.7.1 live config + `ghast:` subsection with
  `flying-speed: 0.097`).

### Proof on the final jar

- Suites: mount 103/0; pack 935/0 on the clean run (one
  intervening 934/1 was the documented pre-existing
  skill-nudge/tracker tie-break flake, identical on pristine
  0.7.0 and seen by the 0.7.1 lane as well — not a defect).
- Rig (receipts refreshed in `build-root/receipts/`): ON **30/0**
  SEVERE 0 — every prior ghast proof (gate, buy at both prices,
  summon, harness, combat mark, toggle) still passes;
  ghastoff 4/0, modoff 4/0, ExcelItems-absent 2/0, all SEVERE 0;
  speed scenario 10/1 — the 1 is the horse ridden-step harness
  artifact documented above (both ghast measurements and all
  attribute readbacks pass).
- The 0.7.1 reload fix's own rig proof (4× `emfixes reload`,
  zero failures) stands in its lane record; the code in this
  jar is byte-identical to it apart from the version constant.
- Still not proven (unchanged from the original record; live
  proof owed after install, never gates): flight feel and
  rider steering with a real client, Bedrock rendering of the
  ghast + harness, idle drift.

Status unchanged: **HELD** — not installed, no player surfaces,
no announcement. The parent seat updates the MANIFEST HELD
entry to the new jar hash.
