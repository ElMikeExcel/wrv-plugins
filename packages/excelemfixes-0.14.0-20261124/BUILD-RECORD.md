# BUILD RECORD — ExcelEMFixes 0.14.0 (food-stack-normalize)

Built Nov 24, 2026. Lane: ~/workspace/emfood-stacksplit-20261124/
Filed as P-EMFOOD-STACKSPLIT-20261124 (Jester's Fresh Rat
report, Nov 24, 2026, 2:59 PM CT; FEEDBACK-TRACKER row 90).
Mike endorsed the fix the same day. Status: BUILT + RIG-PROVEN
+ FILED (packet in packet/, MANIFEST PENDING entry added).
NOT installed — the failsafe owns installs, and this packet
chains on 0.13.0 being live first.

## The defect, from source

EliteMobs 10.9.14 (bytecode-verified, AdvancedFoodItems):
- When a player's Advanced Combat runtime activates,
  preparePlayerInventory adapts every edible item:
  adaptItem backs the food data up into PDC keys
  (namespace elitemobs): advanced_combat_food (BYTE marker),
  advanced_combat_food_default (BYTE: 1 = the item had NO
  food component of its own), advanced_combat_food_nutrition
  (INTEGER), advanced_combat_food_saturation (FLOAT),
  advanced_combat_food_always_edible (BYTE),
  advanced_combat_food_consumption_seconds (FLOAT, original
  seconds) — then rewrites the live food component with the
  same nutrition/saturation but canAlwaysEat forced TRUE and
  the consumption time DOUBLED.
- effectiveFood = the item's own food component if it has
  one, else EM's vanilla table by material. Fresh Rat
  (primis_pontis_mille_fresh_rat.yml: material RABBIT, no
  food fields in the file) takes the table values: nutrition
  3, saturation 1.8, original consumption 1.6 s → adapted
  3.2 s — exactly what Jester's stamped copy carries.
- restoreItem (run at combat deactivation and by
  AdvancedCombatStateRecovery on join/quit/drop paths)
  removes all six keys and rebuilds the resting form:
  default flag → food component removed (the material's own
  defaults apply); otherwise the component is rebuilt from
  the backup values, and the original consumption seconds
  are re-applied through the NMS adapter.
- The leaks between those passes are the bug: a copy that is
  dropped, stored, ender-chested, or saved while stamped
  keeps the stamp after combat ends. Minecraft stacks only
  identical components, so stamped and plain copies of the
  same food never merge (Jester: four piles, 7/1/2/1).

## Direction decision (evidence, not preference)

Normalize DOWN to the resting form, replicating restoreItem
step for step:
- The stamp's nutrition/saturation are copies of the item's
  own resting values (verified in the rig: mint and stamped
  copies both read food 3/1.8; eating a normalized copy
  restored exactly +3 hunger / 1.8 saturation).
- The stamped form's only deltas are transient combat
  conveniences (always-edible, doubled eat time) that EM
  applies on activation and strips at every combat end
  anyway; stamping every copy instead would fight EM's
  restore at every combat boundary and would change how the
  food behaves outside combat (always edible, slow eating).
- EM's restore on a resting item is a no-op (no marker), so
  the module and EM can never fight over an item.

## The module (package excelemfixes.foodstack)

- StampView: reads the six PDC keys into a record.
- RestorePlanner (pure, unit-tested): turns a StampView into
  a restore plan replicating restoreItem's decisions,
  including the conservative incomplete-backup case (marker
  present but backup values missing → leave the food data
  exactly as it is; only the marker family is cleared when
  the plan is safe).
- StackMerger (pure, unit-tested): merge arithmetic over
  similarity groups; fills earlier slots to max stack,
  remainder flows forward.
- FoodStackNormalizeModule (id food-stack-normalize):
  ItemSpawnEvent (ground items), EntityPickupItemEvent
  (players), PlayerJoinEvent (sweep after 40 ticks: storage
  + ender chest), InventoryCloseEvent (container types +
  player sweep). After a sweep, newly identical stacks merge.
  Unstamped stacks are never rewritten.
- Application mirrors EM's exact primitives — a lesson the
  rig taught: the first build cleared the food component via
  the data-component API's unset call, which records an
  explicit component REMOVAL (the item came back with
  food=none — inedible). The shipped build uses the classic
  meta calls EM's restore uses (setFood(null) / rebuild via
  getFood→mutate→setFood, with a data-API fallback), and the
  consumable component is rewritten with the backed-up
  consumption seconds, never unset.
- Config: modules.food-stack-normalize with a full REASON
  comment block (Jester's Nov 24 report) and knobs
  normalize-on-pickup, normalize-ground-items, sweep-on-join,
  join-sweep-delay-ticks (40), sweep-on-inventory-close,
  merge-stacks, debug — all default on/40/false. README
  carries the module section with its reason and its honest
  limit (other data differences — Protect rid, soulbind
  owner, wear — still split stacks by design).

## Build

- Base tree: the 0.13.0 tree (em-menu-replacement-20261124),
  0.13.0 changes intact; version 0.14.0 in plugin.yml,
  VERSION literal, and build output name.
- Jar: ExcelEMFixes-0.14.0.jar, 474,197 B, SHA-256
  aa466af721f0f7930441973fee87dc7bb14e1d0d04145fed32fd264836b46cf0,
  deterministic double-build byte-identical.
- Deploy config: the 0.13.0 packet config + exactly the
  appended 73-line module section (diff-verified single
  insertion): 67,766 B, SHA-256
  562e6cd8b24775cc17c08741fda189c7a52177d02a080488e953a2dc754f1053.
- Suites: final run 1,074 passed / 1 failed — the failure is
  the documented pre-existing skill-nudge tie-break flake in
  untouched code (passed in adjacent runs). New
  FoodStackNormalizeTest 16/16. (One mid-session run showed
  two transient "No space left on device" fixture failures
  under /tmp tmpfs pressure; gone on re-run.)

## Rig proof (Paper 26.1.2 clone, live EliteMobs jar)

Harness: FoodStackProbe plugin + Mineflayer bot (EchoBot) in
this lane's rig/ and harness/ folders. Specimens were genuine
Fresh Rats minted through EM's own CustomItem generation and
stamped by EM's own reflective adaptItem; the comparison
standard was EM's own reflective restoreItem.

1. Defect reproduced: mint = resting (food 3/1.8000001,
   consumeSeconds 1.6); stamped copies = food 3/1.8,
   alwaysEat=true, consumeSeconds=3.2, NOT similar to mint.
   EM's restoreItem output = byte-equal + similar to mint.
2. Join sweep (module on): placed piles 7 unstamped / 1
   stamped / 2 stamped / 1 unstamped + stamped x2 in the
   ender chest → after relog: one x11 resting stack
   (similar to mint) + resting ender pair. (Two sweeps can
   fire per join — the delayed join sweep and the close-event
   sweep; the ender chest's contents finish loading between
   them, and the second sweep catches what the first could
   not see yet. End state converged in every pass.)
3. Eating: setfood 5 → ate one normalized rat → foodLevel
   8 (+3), saturation 1.8. Remaining stack byte-equal to
   mint.
4. Ground/pickup: stamped x3 dropped → the entity's stack
   was already resting + byte-equal to mint at spawn; on
   pickup it merged into the existing pile, 10 → 13, one
   slot.
5. Container close: chest with stamped x2 + resting x3 →
   after the bot opened and closed it: single resting x5.
   Reproduced twice. (Anomaly, recorded honestly: the
   module's container-close DEBUG line never printed in the
   rig log although the convergence is its work — only this
   module merges stacks; EM never does. Join/pickup/ground
   debug lines printed normally. The contents read-back is
   the direct evidence; the missing line is a rig logging
   anomaly, possibly the same /tmp/disk pressure that ate
   two test fixtures.)
6. Module-off control: boot clean, 19 modules, SEVERE 0.
   After relog, EM's own join recovery had un-stamped the
   main-inventory piles but left them UNMERGED in four slots
   (7/1/2/1) and never touched the ender chest (still
   stamped) — the defect persists without the module, and
   the module's merge + ender/container coverage is exactly
   the delta.
7. Refusal: unstamped stacks were never rewritten; a named,
   enchanted control sword's bytes were unchanged across
   every sweep.

## Filing

- packet/INSTALL-PACKET.md + jar + deploy-config.yml +
  SURFACES.md (What's New id 2026-11-24-food-stacks-merge;
  website row 0.13.0 → 0.14.0, module count 19 → 20).
- MANIFEST PENDING entry added Nov 24, 2026 (chains on
  0.13.0's gates).
- Proof owed after install: Jester's live Fresh Rats merge
  (closes row 90's EM half); the upstream EliteMobs report
  naming 0.14.0 waits for that live proof (Mike's testing
  bar).
