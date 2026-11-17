# ExcelZombieCharge - SPEC

Status: 0.1.0 built; rig proof per BUILD-RECORD.md. NOT
installed on the live server. Stage only, per the commission.

## What this is: a rename, nothing more

ExcelZombieCharge 0.1.0 is **ExcelProvoke 0.1.0 renamed** -
identity changes only, behavior code untouched. Mike picked the
new name Oct 21, 2026 (11:50 PM CDT) under his plugin naming
rule (stated the same night): names are discussed with him
before they are fixed, and a plugin's name must describe what
it actually does. "ExcelProvoke" failed that test - it reads
like a taunt ability (EliteMobs literally has a Provoke power)
instead of describing the mechanic. The mechanic in plain
words: **hit it from range, it charges you** - hence
ExcelZombieCharge.

Identity surface changed: plugin.yml name + main class, jar
name, data folder (`plugins/ExcelZombieCharge/`), package and
class names, log prefix, the admin permission node
(`excelzombiecharge.admin`), and the attribute-modifier key
(`excelprovoke:speed` -> `excelzombiecharge:speed`; the
migration duty that creates is below). Deliberately NOT
changed: the admin command stays `/provoke` (it is the existing
admin command name; renaming it would buy nothing and cost
muscle memory - noted in the build record), and every behavior
value is the approved one (+0.5 multiplier, 60 ticks, the
seven-type victim list, debug off), so the renamed plugin
regenerating its config defaults in the new folder is lossless:
the live `plugins/ExcelProvoke/` folder holds only those same
defaults.

### The one migration duty: the legacy key

Bukkit attribute modifiers are part of the entity's persistent
attribute map - they are saved with the mob's data and survive
restarts. ExcelProvoke is LIVE, so a mob saved mid-charge when
the swap restart stops the server can come back carrying a
modifier under `excelprovoke:speed`, and the old plugin will
not be there to remove it. ExcelZombieCharge therefore strips
BOTH keys wherever it strips its own:

- every removal in ChargeManager (apply refresh, strip, sweep
  expiry, clear-on-disable) removes both keys;
- the stale-strip (EntitiesLoad + boot pass) treats an
  untracked modifier under EITHER key as stale, and logs legacy
  removals distinctly ("stripped a LEGACY ExcelProvoke charge
  modifier").

A legacy modifier can never be a live charge of this plugin
(charges are applied only under the new key, and a live charge
always has a tracker entry), so removing it alongside our own
is always safe. The strip set is pinned as pure data in
`core/ChargeKeys` and proved by a plain-JVM test
(ChargeKeysTest: the legacy key IS in the strip set) and by the
rig (a zombie planted with the legacy modifier in boot 1, with
no charge plugin present, reports it gone in boot 2 with only
the new jar installed).

## Why this exists

Mike's Oct 21, 2026 seal-0 bow playtest: every mob felt easier
than expected on the ranged lane. The mechanism is plain - a
zombie walks at 0.23 (the slowest chase in its family) and a bow
user can plink it from 30 blocks while it merely wades closer.
Melee hits were never the problem; kiting was. Mike approved
the mechanic the same night: **the anti-kiting charge** - a
ranged hit from a player makes the mob charge at +50% speed for
3 seconds. Standing your ground stays calm (melee never triggers
it); sniping is what provokes the charge. The design source,
verified from the Magic 11.2.5 jar and the EliteMobs 10.9.10
movement code before anything was built, is
`~/workspace/provoke-speed/FINDINGS.md`; this plugin implements
its recommended recipe.

## Behavior (the whole contract)

When a LivingEntity whose type is in the victim set takes
damage, the plugin acts only if the hit classifies as one of
exactly two shapes:

1. **Projectile:** the damager is a Projectile whose shooter is
   a Player (arrow, crossbow bolt, thrown trident; Magic's own
   arrow spells are real arrows and count here by design).
2. **Magic signature:** cause MAGIC, damage type in the magic
   family (MAGIC or INDIRECT_MAGIC), damager
   is a ThrownPotion whose item carries NO instant-damage effect
   (Magic's reusable blank scratch potion), and the damage
   source's causing entity is a Player. (FINDINGS Q1 said the
   type would be MAGIC; the original build rig's live Missile
   hit reported INDIRECT_MAGIC - Magic hurts through NMS
   `DamageSources.indirectMagic` - so the family check accepts
   both. The blank check remains the separator; see the
   predecessor's BUILD-RECORD in ~/workspace/excelprovoke/.)

Then it applies one transient AttributeModifier to the victim's
MOVEMENT_SPEED under the fixed key `excelzombiecharge:speed`,
Operation MULTIPLY_SCALAR_1, amount = `speed-multiplier`
(default 0.5, so a plain zombie runs 0.23 -> ~0.345). Apply is
remove-then-add: a further qualifying hit REFRESHES the charge
(expiry = that hit + `duration-ticks`, default 60) and can never
stack it. The base value is never written.

Nothing else ever happens. Specifically:

- A real harming potion - a witch's or a player's own - carries
  an instant-damage effect in its item and NEVER qualifies
  (that blank-vs-loaded check is the entire Magic/witch
  separator, per FINDINGS).
- Other MAGIC-cause sources (AreaEffectCloud, EvokerFangs,
  Guardian, dragon breath) present non-potion damagers and
  never qualify.
- Melee, sweep, explosions, falls and every environmental cause
  never qualify. Spells configured `magic_damage: false` present
  as ENTITY_ATTACK and intentionally do not qualify (stock set:
  dash, powerstone, sweep - no primary damage spells).
- Projectiles from non-players (skeletons, dispensers) never
  qualify.
- Non-victim types never qualify, whatever hit them.

Deliberate non-goals: no targeting changes (the mob's own AI -
vanilla or EliteMobs - picks the target; damage already sets
it), no damage changes, no particles, no sounds.

## Victim set (config `entity-types`, these defaults)

ZOMBIE, HUSK, DROWNED, ZOMBIE_VILLAGER, ZOMBIFIED_PIGLIN,
PIGLIN, PIGLIN_BRUTE. **The hoglin is deliberately NOT in the
set** (fast already, built differently).

## Cleanup (a charge must never outlive its window)

Bukkit attribute modifiers live in the entity's persistent
attribute map - they survive chunk unload and restart - so the
3-second charge is fenced on every side:

- a 10-tick sweep removes the modifier at expiry and drops
  tracking for entities that died or left;
- EntityDeathEvent and EntityRemoveEvent strip immediately;
- EntitiesUnloadEvent strips the modifier BEFORE the chunk
  saves, so nothing charged is ever written to disk;
- EntitiesLoadEvent strips any modifier under our key OR the
  legacy ExcelProvoke key that has no live tracker entry (a
  live charge always has one, so an untracked modifier is
  stale by definition) - the backstop for a crash mid-charge
  and for the rename migration, together with the same pass at
  boot;
- onDisable removes every reachable modifier and clears the
  book.

## EliteMobs interaction

None at the API level: no dependency, no softdepend, no
reflective seam. The attribute route is the point - FINDINGS Q2
proved both EliteMobs movement modes derive their step from the
entity's live MOVEMENT_SPEED attribute value (native: vanilla
navigation reads it directly; basic_melee.lua: pursuit factor
and the x0.75 fallback multiply the attribute, not a hardcoded
base), so the charge speeds elites exactly as it speeds vanilla
mobs. It coexists with EM's own movement_speed power as a
separate channel: that power grants a Speed potion EFFECT; both
apply and multiply together when both are present. EM may
rewrite base values at elite spawn; a modifier is not a base
write and is not stripped, and worst case the next qualifying
hit re-applies it, because the charge is refresh-driven.

## Config (`plugins/ExcelZombieCharge/config.yml`)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch; false = nothing ever charges. |
| `speed-multiplier` | `0.5` | The MULTIPLY_SCALAR_1 amount: 0.5 = +50%. Clamped [0, 4]; non-finite falls back to 0.5. |
| `duration-ticks` | `60` | Charge lifetime after the last qualifying hit. Clamped [1, 7200]. |
| `entity-types` | the seven above | The victim set, normalized (trim/uppercase/dedupe). Absent = defaults; explicitly empty = nothing charges. Tokens matching no EntityType warn at load. |
| `debug-log` | `false` | Every damage event on a victim-set mob logs the extracted event shape + verdict, plus charge/expiry lines. |

`/provoke status|reload` (permission `excelzombiecharge.admin`,
default op). Reload re-reads the file and swaps an immutable
settings snapshot atomically; a broken file keeps the previous
snapshot. Live charges keep the terms they were applied under;
new settings govern the next qualifying hit.

## Hook point

EntityDamageByEntityEvent at MONITOR, cancelled events skipped:
by MONITOR the event has fully resolved, so the extracted
fields are the truth of the hit, and the plugin - an observer
that only ever writes an attribute on the victim - never needs
to mutate the event itself.

## Tests and proof (before "ready")

- Plain-JVM suites (house Check harness): the full
  classification table (every cause/damager combination incl.
  every FINDINGS exclusion, with the branch-order pins: victim
  membership first, ThrownPotion always down the signature
  branch), the charge book's refresh/expiry semantics on a
  scripted tick source (no wall clock), settings defaults and
  clamps, the plugin.yml no-dependency guard with the identity
  fields pinned, and ChargeKeysTest pinning the modifier strip
  set (current key + LEGACY key).
- Deterministic build: consecutive `build.sh` runs
  byte-identical; a clean-room rebuild from the source zip
  byte-identical; both identical to the jar the rig tested
  (three-way match, SHA-256 in the build record).
- Rename rig (full stack: Paper 26.2 build 123 + EliteMobs
  10.9.10 live-mirror config + Magic 11.2.5 staged jar), two
  boots: boot 1 plants a persistent zombie carrying the LEGACY
  `excelprovoke:speed` modifier with NO charge plugin present
  and stops (the modifier serializes with its data); boot 2,
  with only ExcelZombieCharge installed, must show the legacy
  modifier stripped, the enable line clean (SEVERE 0),
  `/provoke status` answering, and the arrow spot proof (a
  fresh zombie reads 0.23 -> 0.345 with exactly one modifier
  under the NEW key). The predecessor's full seven-scenario rig
  suite is NOT re-run: the behavior code is byte-for-byte the
  renamed predecessor's, whose suite passed against the
  identical logic (see ~/workspace/excelprovoke/BUILD-RECORD.md);
  what is new here is identity and the legacy strip, and those
  are exactly what this rig proves. Numbers in BUILD-RECORD.md.

## Install (when Mike calls a window)

Empty-server window: remove `ExcelProvoke-0.1.0.jar` and the
stale `plugins/ExcelProvoke/` folder, drop
`ExcelZombieCharge-0.1.0.jar` into `plugins/`, restart, confirm
the ExcelZombieCharge enable line. The new folder regenerates
the same approved defaults, so no config migration is needed;
any mob still carrying a legacy modifier is stripped at boot by
the pass above. Rollback = remove the new jar, restore
`ExcelProvoke-0.1.0.jar` (and its folder), restart. Post-install
proofs owed (one at a time, per house rule): one Bedrock and
one Java ranged hit on a zombie during natural play, watched in
the debug log if temporarily enabled - or judged by feel on the
next play session, Mike's call.
