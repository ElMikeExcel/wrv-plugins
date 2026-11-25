# INSTALL PACKET -- ExcelEMFixes 0.14.0 (food-stack-normalize module)

Lane: ~/workspace/emfood-stacksplit-20261124/
Build record: ~/workspace/emfood-stacksplit-20261124/BUILD-RECORD.md
Origin: Jester's report, Nov 24, 2026, 2:59 PM CT (Discord DM;
FEEDBACK-TRACKER row 90) -- some of his Fresh Rats would not
stack with the others. Filed as P-EMFOOD-STACKSPLIT-20261124.
Mike endorsed the fix Nov 24, 2026 ("Yeah that's something
that needs fixing for sure"). This is the EliteMobs-cause half
of row 90; the second, independent cause (ExcelProtect's rid
stamp on stackables) is ExcelProtect 0.1.14, filed separately
as P-PROTECT-RID-STACKSPLIT-20261124.

## What this is

One new toggleable module in the pack: food-stack-normalize
(module #20). Nothing else changes; the 0.13.0 changes ride
along intact (this build is based on the 0.13.0 tree).

The mechanism, verified from EliteMobs 10.9.14 bytecode and
Jester's live playerdata: EliteMobs' Advanced Combat system
ADAPTS edible items while a player's combat runtime is active
-- it backs the item's food data up into
elitemobs:advanced_combat_food_* PDC keys and rewrites the
live food component (same nutrition/saturation, but
always-edible forced on and eating time doubled) -- and
RESTORES the item when combat ends. The restore passes leak:
copies caught mid-flight (dropped, stored, in an ender chest,
saved mid-combat) keep the stamp forever. Minecraft stacks
only identical components, so a stamped Fresh Rat never
merges with a plain one -- Jester held four piles (7/1/2/1).

Direction (evidence-based): normalize DOWN to the resting
form, replicating EliteMobs' own restoreItem step for step.
The stamp's nutrition/saturation are copies of the item's own
resting values, so a resting copy nourishes identically; the
stamped form's only deltas are transient combat conveniences
EM applies and removes itself. EM's restore on a resting item
is a no-op, so the two systems can never fight.

The module normalizes stamped stacks back to the resting form
and merges piles that become identical:
- ground items at spawn (ItemSpawnEvent),
- on pickup (EntityPickupItemEvent, players),
- on join (delayed sweep of inventory + ender chest),
- on container close (chests/barrels/etc. + the player).
Unstamped items are never touched. Items that differ for
other reasons (Protect rid stamps, soulbind owners, wear)
still split by design -- the rid cause is the 0.1.14 fix.

## Artifacts (in this folder's packet/)

- ExcelEMFixes-0.14.0.jar -- 474,197 bytes
  SHA-256 aa466af721f0f7930441973fee87dc7bb14e1d0d04145fed32fd264836b46cf0
  Deterministic double-build byte-identical (build.sh in the
  lane's build-root/).
- deploy-config.yml -- 67,766 bytes
  SHA-256 562e6cd8b24775cc17c08741fda189c7a52177d02a080488e953a2dc754f1053
  The 0.13.0 packet's deploy config with EXACTLY one change:
  the 73-line food-stack-normalize section appended (module
  comment block carries the REASON in plain language).
  Verified by diff against the 0.13.0 packet config: a single
  insertion, nothing else differs.

## Provenance gates (STOP if any fails)

ORDERING: this packet chains on 0.13.0 -- install 0.13.0
first (its own PENDING entry). If 0.13.0 is not live yet,
STOP; do not re-base this packet onto 0.12.0.

- Live plugins/ExcelEMFixes-0.13.0.jar must hash
  eeec91c1ea0496401544e477a2f170d46df53e80ca5c3772366c900298d65fd5
  (461,532 bytes -- the 0.13.0 build).
- Live plugins/ExcelEMFixes/config.yml must hash
  a749aa7d5607c27c8fd0bb177cd0423f06f7d25bec7a27ff8fcfd5ca2892d3ea
  (64,115 bytes -- the 0.13.0 deploy config).
- Live plugins/ExcelEMFixes/ must contain NO clarity-menus.yml
  (positive listing check, spec v2.1 A6). If one has appeared,
  STOP: it overrides the bundled clarity text, and the window
  must merge per the 0.13.0 packet's note as well.
If the jar differs, STOP and re-base. If the config differs,
do NOT place deploy-config.yml: hand-append only the packet
config's food-stack-normalize section to the live config
instead, backing the live file up first.

## Placement

1. Snapshot first (fleet standard).
2. Stop the server (verified empty, fleet standard).
3. Jar: MOVE plugins/ExcelEMFixes-0.13.0.jar aside as
   ExcelEMFixes-0.13.0.jar.bak-<install date>-pre0140
   (a move, never a copy -- two pack jars must never be
   co-resident), then place the packet jar as
   plugins/ExcelEMFixes-0.14.0.jar.
4. Config: back the live config up as
   config.yml.bak-<install date>-pre0140, then place
   deploy-config.yml as plugins/ExcelEMFixes/config.yml
   (or the hand-append above if the gate said so).
5. Start the server.

## Acceptance

- Boot log carries "ExcelEMFixes 0.14.0 ready" with 20
  modules including food-stack-normalize (ready-line version
  literal matches plugin.yml -- spec v2.1 A11 cross-check),
  plus the line "Food Stack Normalize ready".
- `emfixes reload` reports ZERO reload-failed.
- SEVERE count 0 for the boot.
- Exactly-one-ExcelEMFixes-jar census in plugins/ after the
  swap (spec v2.1 A4/A5).

## Proof (pre-install)

- Unit suites: 1,074 passed, 1 failed in the final run -- the
  single failure is the documented pre-existing skill-nudge
  tie-break flake in untouched code (it passed in the run
  before). New FoodStackNormalizeTest: 16/16 (restore-planner
  cases incl. the Fresh Rat default-flag case and the
  conservative incomplete-backup case; merger cases incl.
  Jester's 7/1/2/1 -> 11). One earlier run showed two
  transient "No space left on device" fixture failures under
  /tmp tmpfs pressure; gone on re-run, unrelated to this code.
- Rig proof (Paper 26.1.2 clone, live EliteMobs jar, genuine
  Fresh Rats minted through EM's own CustomItem path and
  stamped by EM's own adaptItem; full detail in
  BUILD-RECORD.md):
  * defect reproduced: mint resting vs EM-stamped copy are
    NOT similar (food 3/1.8 both, but alwaysEat true and
    consumeSeconds 3.2 vs 1.6 on the stamped copy);
  * EM's own restoreItem output is byte-equal to the mint --
    the direction check;
  * join sweep: piles 7/1/2/1 + a stamped ender-chest pair
    converged to one x11 resting stack + resting ender pair,
    all similar to the mint;
  * eating a normalized rat: hunger 5 -> 8 (+3 nutrition),
    saturation 0 -> 1.8 -- identical to the minted food;
  * ground path: a stamped x3 drop was normalized at spawn
    (byte-equal to mint) and merged on pickup, 10 -> 13 in
    one slot;
  * container path: chest holding stamped x2 + resting x3
    converged on close to a single resting x5 (twice);
  * module-off control: clean boot with 19 modules; EM's own
    join recovery un-stamps the main inventory but does NOT
    merge (four piles remain) and never touches the ender
    chest -- the defect persists without the module;
  * refusal: unstamped stacks and a named/enchanted control
    sword were byte-untouched by every sweep.

## Proof owed after install

- Live test (the upstream bar): Jester's real Fresh Rats
  merge in his inventory (his next Primis rat run, or a
  relog with split piles) and a normalized rat eats for the
  same hunger/saturation. His word + a Protect/playerdata
  spot-check close row 90's EM half.
- Upstream: an EliteMobs report naming ExcelEMFixes 0.14.0
  (the adapt/restore leak between combat passes) is owed
  AFTER that live proof, per Mike's Nov 24 testing bar
  (parent lane drafts it for Mike's eyes; this packet does
  not send anything).

## Surfaces (same pass)

See SURFACES.md in this folder: the in-game What's New TEXT
entry (id 2026-11-24-food-stacks-merge) rides as a companion
live-edit placement; the website plugin-row bump
0.13.0 -> 0.14.0 (module count 19 -> 20) is a parent
follow-up; a Discord note to Jester (he reported it) goes
out from the parent seat after install with the proof ask.

## Rollback

Stop the server; remove ExcelEMFixes-0.14.0.jar; move
ExcelEMFixes-0.13.0.jar.bak-<install date>-pre0140 back to
ExcelEMFixes-0.13.0.jar; restore
config.yml.bak-<install date>-pre0140 over config.yml; start.
No data migration either way: the module writes no data
files, and items it already normalized are ordinary resting
items (the exact form EliteMobs' own restore produces), so
rolling back leaves them valid.
