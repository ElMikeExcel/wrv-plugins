# ExcelEMFixes

**A plugin box for EliteMobs.** ExcelEMFixes is one plugin that holds a
collection of small, independent modules — each one fixes a defect or a
rough edge in EliteMobs (or adds one small companion feature), each one can
be switched on or off by itself, and each one is configured in its own
clearly separated section of one config file, like its own mini-plugin
living in the box.

Drop the jar in, restart, and turn on only the fixes you need. Everything
is data-driven from EliteMobs' own files wherever a fix touches content,
so new content packs are covered the day they are installed — no update
from us required.

Requires: Paper 1.21+ (built against Paper 26.x), EliteMobs 10.x.
Optional: Geyser-Spigot (the menus render as Bedrock forms automatically;
without Geyser everything works the same for Java players). Jobs — only
the jobs-dummy-gate module uses it, and that module simply idles when
Jobs is not installed. mcMMO — only the mcmmo-xp-connector module uses
it, and that module simply stands down when mcMMO is not installed.

## Install

1. Put `ExcelEMFixes-0.6.0.jar` in your `plugins/` folder and restart.
2. Open `plugins/ExcelEMFixes/config.yml`. Every module has its own
   headed section with an explanation of the defect it fixes and its
   `enabled` switch. All modules default to **on** except where noted.
3. Three modules read data files from `plugins/ExcelEMFixes/`:
   - **Quest Destinations** reads `quest-destinations.yml` — it ships as a
     blank template with the format documented inside. EliteMobs cannot
     tell you where an instanced boss's entrance is (that is the defect),
     so this one table is filled in by you, once per affected quest. The
     plugin logs a warning naming any quest it detects as tracker-blank
     that your table does not cover yet.
   - **Drop Guide** reads `drops-index.yml` and **Progress Tracker**
     reads `tracker-catalog.yml`. Both ship empty; generate yours from
     your own EliteMobs install with the two Python tools in the
     companion project (`generate_index.py`, `generate_tracker_catalog.py`
     — they read your `plugins/EliteMobs` folder and write the two files).
   - **Seal Restore** reads `seal-restore.yml` — ships empty; add a row
     only if you own content whose arena seal is not fully restored (see
     the module notes below).

## The modules

### quest-destinations — Quest Tracker Destinations (default: ON)

**The defect:** for any quest whose target is an instanced boss,
EliteMobs' quest tracker shows no destination at all. The tracker never
loads an instanced boss's spawn locations, so there is nothing to point
at (upstream: MagmaGuy/EliteMobs issue #241). The player is told to kill
something and given no idea where it lives.

**The fix:** `/questwhere` lists the player's active quests that have
this problem and gives each one's real destination — the place name,
coordinates, and how to get there (the wormhole or teleporter NPC at the
instance's entrance, which is the part the player actually needs). A
join hint tells affected players the command exists. Destinations come
from your `quest-destinations.yml` table; the module also scans
EliteMobs' quest and boss files at startup and warns about tracker-blank
quests your table does not cover.

Sub-toggles and commands:
- `modules.quest-destinations.enabled` — the whole module. Off = the
  `/questwhere` and `/questdest` commands do not exist.
- `modules.quest-destinations.join-hint` (default on) — the one-line
  login hint for players on an affected quest.
- `modules.quest-destinations.glow.enabled` (default on) — the glow
  guidance below, separately from the destination text.
- Commands: `/questwhere`, `/questwhere show <quest file>`,
  `/questwhere guide <quest file>` (destination + glow, your own active
  quests), `/questwhere glow <world> <x> <y> <z> [entity name]` (the
  general glow surface other plugins and menus can call).

**Glow guidance:** "Guide me there" lights the way for a short while
(`glow.duration-seconds`, default 90): the destination NPC itself glows
when the destination is a person; a temporary glowing marker stands at
the spot when it is a place. Guidance only — this plugin never teleports
anyone. A small developer API (`excelemfixes.api.GlowApi`) exposes the
same glow to other plugins, and the Drop Guide / Progress Tracker
modules in this box use it for their own "Glow the way in" buttons.

### arcane-bolt — Arcane Bolt Scaling (default: ON)

**The defect:** EliteMobs computes ACS ability damage (Arcane Bolt and
its siblings) from the ability alone — your weapon's damage scaling is
ignored, and your own outgoing-damage buffs (for example Spell Echo) are
never applied to the hit. A geared caster's bolt hits like a fresh
character's.

**The fix:** the module measures each ability hit and adds the missing
part as extra damage: the caster's outgoing-damage buffs, plus optional
weapon scaling (`ability-damage-scale`, default **1.0** = buffs only, no
weapon scaling), capped per hit at a fraction of the target's max health
(`max-extra-fraction-of-max-health`, default 1.0).

### drop-guide — Drop Guide (default: ON)

Not a defect fix — a companion feature that lives in the box. **The
gap:** EliteMobs has no in-game way to ask "what drops that?" — players
alt-tab to a wiki or guess. `/drops` answers it: places → mobs → the
mob's full drop list, read from a generated index of your own EliteMobs
content. Drop *chances* are never shown, by design. Places and mobs get
a **Glow the way in** button when the module can resolve an entrance
from EliteMobs' own data (boss spawns → content packages → wormholes /
teleporter NPCs, all read at runtime).

### progress-tracker — Progress Tracker (default: ON)

The other companion feature. **The gap:** EliteMobs tracks everything
about a player and shows almost none of it. `/progress` (alias
`/tracker`) opens the player's own record: quest completions and
distinct quests, the main chain, dungeon runs and per-boss kills,
combat totals, classes and skills — denominators from your content,
totals from EliteMobs' own player data. Quests whose tracker is broken
(see quest-destinations) get a **guide button** here too: directions in
chat plus the entrance glow.

### food-heal — Food Heal (default: ON)

**The behavior:** in EliteMobs worlds, food barely heals. EliteMobs'
own out-of-combat trickle is a hardcoded 1%/s of max health with no
config, and a steak does almost nothing against an ACS-raised health
pool in the hundreds.

**The fix:** eating food in an EliteMobs world grants a heal budget —
1% of max health instantly, the rest delivered at 0.5%/s while out of
combat — sized by the food: total = clamp(saturation × 0.75, 1%, 10%)
of max health (steak 9.6%, bread 4.5%, carrot 2.7%). Plus a base
out-of-combat bonus of +1%/s with no food needed, so the total base
out-of-combat heal is 2%/s next to EliteMobs' own 1%/s. Budgets pause
in combat (20-second window, mirroring EliteMobs), expire 90 seconds
after the meal, and are replaced — never stacked — by re-eating. Every
number is a knob in the module's config section.

### leash — MMO Leash (default: ON)

**The defect:** EliteMobs' built-in boss leash cannot behave like an
MMO leash. Its soft return starts at *half* the configured radius (a
boss set to leash at 96 starts walking home at 48 — mid-camp-fight),
its returning bosses are damage-invulnerable (so a player can never
re-aggro one on its way home), and it never clears the boss's target.

**The fix:** the module suppresses the native leash (every boss's
in-memory leash radius is set to 0; boss files are never written) and
runs a proper leash itself: within `range` blocks of its spawn point
(default **96**) an elite fights completely normally; pulled beyond
the range it drops aggro, walks home, and heals to full on arrival —
but player damage on the way home re-aggroes it instantly and cancels
the heal.

### seal-restore — Seal Restore (default: ON, no rows = no-op)

**The defect:** EliteMobs content can seal an arena when a boss spawns
(placing blocks over its doors) and restore the blocks from the boss's
onRemove list when it dies — but content exists whose restore list is
*shorter* than its seal list, so part of the arena stays walled up
forever after the fight, every fight.

**The fix:** list the affected boss and the blocks its arena should
end with in `seal-restore.yml`. When that boss dies, any registered
block still wearing its sealed state is restored; blocks changed to
anything else are never touched. Nothing is hardcoded — any boss, any
world, any blocks.

### jobs-dummy-gate — Jobs Dummy Gate (default: ON; needs Jobs)

**The defect:** EliteMobs' Adventurers Guild training dummies are
custom bosses whose entity type is HUSK — and as regional bosses they
respawn on the spot, forever. The Jobs plugin pays its Kill actions
by entity type, so every dummy death pays the killer the full husk
rate in money and job XP, and a player can stand at the guild and
farm a "training" target indefinitely (observed in the wild: three
kills in ten seconds, full pay each). EliteMobs itself paying nothing
for the dummy does not help — Jobs pays on the death event regardless
of EliteMobs' drop flags, and a death event cannot be cancelled.

**The fix:** a veto on Jobs' own payment events
(`JobsPrePaymentEvent` + `JobsExpGainEvent`, zeroed and cancelled),
keyed on EliteMobs' identity for the entity — its boss filename,
read through EliteMobs' own tagger — for the bosses in the module's
`boss-files` list. The shipped default is EliteMobs' own five guild
training dummies (`training_dummy_lv1` / `lv25` / `lv50` / `lv75` /
`lv100`), which ship with EliteMobs' Adventurers Guild content; the
list is a plain config knob, so any EliteMobs boss can be covered.
Identity is never entity type: a real husk anywhere else is not on
the list and keeps paying normally, and class-trial actors are NOT
listed — their kills are real objectives and keep paying. Without
the Jobs plugin there is nothing to veto and the module idles.
Wink Reyes Valley runs this module at the shipped defaults.

### enderman-resistance — Enderman Magic Resistance (default: ON)

**The defect:** an enderman can *never* be damaged by a projectile.
Vanilla refuses every projectile damage source outright — no damage
event, just a teleport away — and every EliteMobs magic attack is
projectile-delivered at the point of contact. Bows and tridents whiff
100% of their hits on endermen; wand and staff bolts land only when
the enderman happens not to blink during the bolt's flight; class
abilities whiff outright. A magic or ranged build cannot fight — or
earn any weapon/class XP — on enderman content (regional bosses,
event bosses, natural elites), while melee is completely unaffected.

**The fix:** endermen get *resistance* instead of immunity. The
module watches projectile hits and magic casts aimed at in-scope
endermen (elite endermen anywhere; natural endermen in EliteMobs
worlds — registry, config list, or the `em_` name prefix by default)
and reconciles each hit so only `damage-fraction` of it lands
(default **0.5**): a hit EliteMobs applied itself is trimmed by
restoring the resisted share; a hit vanilla refused is re-applied
once as a player-sourced hit — the one shape endermen accept — so
EliteMobs' own pipeline values it from the shooter's gear and
credits the shooter in its damager ledger, then trimmed the same
way. Melee is never observed or adjusted, and endermen keep
teleporting and hitting exactly as before. At 0.5 an enderman stays
the most magic-resistant mob in the game — roughly twice the hits
of anything else — but every hit counts and every build can level
on enderman content. Wink Reyes Valley runs this module at the
shipped defaults.

### field-medic — Field Bandage (default: ON; needs ExcelItems)

**The gap:** every meaningful heal in EliteMobs content is
out-of-combat-gated — EliteMobs' own trickle and this pack's
food-heal budgets both pause while a player is fighting — while
the hits that kill arrive *in* combat (elite hits at a median of
~10–12 HP against 62–76 HP player pools; a focus-fire burst can
take a whole pool in ~2 seconds). The only in-combat heal a player
owns is a brewed Instant Health II potion: 8 HP, and sold nowhere.

**The solution:** the **Field Bandage**, a consumable a player
right-clicks mid-fight (one gesture, identical on Java and
Bedrock) for **35% of max health over 5 seconds** — about three
potions' worth, or one to two elite hits: enough to turn a
mistimed fight, never enough to out-heal focus fire. One bandage
at a time, a 60-second cooldown from use, no stacking, and a
refused use consumes nothing. The cooldown survives relogging
(it is mirrored into the player's own data); death clears it.
Every delivery goes through `EntityRegainHealthEvent`, so heal
accounting (ExcelProtect and friends) sees every point — this
pack never heals silently.

The bandage is an *item*: it registers once through
[ExcelItems](#) (permanent registry ID 6, key `field_bandage`)
and this module is its behavior. ExcelItems is a softdepend of
the pack and a hard requirement of this module — without it the
module stays off and logs one line naming the missing dependency.
The registry ID is pinned in code, never in config. Wink Reyes
Valley runs this module at the shipped defaults.

**The second gap:** that brewed potion was sold *nowhere* — a
player who wanted the burst heal had to brew it or go without.

**The solution:** the **Strong Healing Potion**, a real Splash
Potion of Instant Health II (8 HP) that registers through
ExcelItems as permanent registry ID 7 (key
`strong_healing_potion`) and is minted by the registry at
delivery — never a plain effect-less splash potion under a
healing name. The ExcelShops special-products listing sells it
by that registry ID at **$150**; the guild vendor NPC sells it
at **$112.50** (the standing 25% vendor discount). Buying runs
this module's review/confirm flow: ExcelShops never charges for
special products itself — its menu hands the buyer to
`/fieldmedic buy`, which arms a review at the venue price (no
charge, price snapshotted), and `/fieldmedic confirm` charges
Excel Bucks through Vault and delivers. A delivery failure
after payment refunds automatically; a failed refund is logged
SEVERE rather than silently lost. Vault is needed only for the
purchases — without an economy the bandage and both items work
and the commands report purchases unavailable.

**The generic vendor flow (`/guildvendor`):** the same vendor
NPC also sells plain goods at the 25% vendor discount — **16
Ender Pearls for $300** (list $400) and an **Empty Map for
$75** (list $100). EliteMobs' own NPC shops are engine-driven
menus with no per-item price files, so a discounted fixed
listing for a vanilla item cannot be expressed in EliteMobs
content; the NPC's interaction instead runs `guildvendor buy
<good> <player>` into the same review/confirm engine
(`/guildvendor confirm` to pay), the pattern the summon-mount
module proved with its NPC venue. One armed review per player
is shared across both commands, so a player can never hold two
armed prices at once. Wink Reyes Valley runs the buy flows at
the shipped prices above, which are the module's defaults.

### melee-premium — Melee Premium (default: ON)

**The design gap:** in EliteMobs worlds a melee fighter pays for
every hit twice — once in the swing, and again in the contact
damage eaten at point-blank range, which EliteMobs' elite damage
scaling multiplies (on the reference server, natural elites deal
x1.94 and regional bosses x2.0) — while a ranged or magic fighter
kites the same mob for free. EliteMobs' damage multipliers are
weapon-neutral, so nothing compensates the melee fighter for
standing in the fire.

**The fix:** a melee-range risk premium. Direct melee hits by
players in EliteMobs-scope worlds (registry, config list, or the
`em_` name prefix by default) deal `damage-multiplier` more
damage (default **1.15** — melee slightly above magic
single-target, the reference server's standing balance ruling).
For ELITE targets the scaling happens inside EliteMobs' own
damage pipeline — on its `EliteMobDamagedByPlayerEvent`, after
its formula and before it credits the hit — so the damager
ledger, weapon/class XP and loot shares all price the premiumed
number (a side-channel HP top-up would move health while the
ledger kept the un-premiumed figure; a pre-pipeline Bukkit
modifier is overwritten by EliteMobs' formula — both were
measured on the rig and rejected). For NON-elite targets in
scope worlds, where stock vanilla damage applies, the hit's
base damage is scaled the vanilla way so defenses apply to the
stronger hit. Projectiles, magic weapons (wands/staves — a
wand in hand is magic delivery to EliteMobs), sweep echoes and
synthetic/custom damage get no premium, and the overworld is
never touched — including elites that appear there. Wink Reyes
Valley runs this module at the shipped defaults.

### skill-bonus-nudge — Skill Bonus Nudge (default: ON)

**The defect:** EliteMobs' skill system grants real power — the Armor
line alone holds Battle Hardened, Second Wind and Last Stand — but
nothing anywhere tells a player the picks exist or that theirs are
sitting unclaimed. The claim menu is three screens deep inside
EliteMobs' status screen, and EM never points at it. Observed live
(Nov 22, 2026): a player at Armor level 14 / Wands 17 had selected
zero bonuses in every skill while dying to the burst damage the
unclaimed Armor picks exist to answer.

**The fix:** once per session, a few seconds after a player joins,
the module compares their EliteMobs skill levels and selected
bonuses — read through EliteMobs' own public API, the same getters
its Skills page uses, strictly read-only — against EliteMobs' own
bonus catalog (the `skillbonuses` folder: per skill, how many
enabled bonuses each tier holds; tiers unlock at levels 10/25/50/75
and at most 3 bonuses can be active per skill, both EliteMobs' own
rules, both config knobs here). If a pick is genuinely claimable,
the player gets a short message naming the count and the claim path
(`/elitemobs` → Skills). Claiming the bonuses stops the nudge on
its own. The check is deliberately not world-gated: it is about the
player's progression, evaluated once at join, and changes no
gameplay. Wink Reyes Valley runs this module at the shipped
defaults.

### respawn-grace — Respawn Grace (default: ON)

**The problem:** a player who dies in an EliteMobs world drops
everything where they fell, and the run back to recover it goes
through the same mobs that just killed them — players die twice in
minutes on the corpse run, and the spiral, not the death, is what
hurts.

**The fix:** the server's Nether precedent (protection holds until
the player hits something) applied to the death-return — with a
fuse, because an open-ended shield is too strong. Dying in an
EliteMobs world arms a one-use **return ticket** and tags every
dropped stack with the owner's name and a per-death id. When the
player comes back into an EliteMobs world — respawning inside one,
or crossing back into one while the ticket is live — they take
**no damage from any cause** until the fuse burns:

- **Loot fuse** — the first pickup of their *own* tagged
  death-drops starts a `countdown-seconds` countdown (default
  **10**): action-bar numbers plus a tick sound each second and a
  distinct final tick. Still protected while it runs; at zero,
  protection ends with a **"Round start!"** title flourish.
  Other pickups never start it and never restart it.
- **Attack fuse** — dealing damage at any point ends protection
  immediately, cancels any countdown, and fires the same flourish.
  The hit itself lands in full.
- **Cap** — before any loot is recovered, `max-seconds` (default
  **60**) ends protection plainly; it cannot be sat on forever.

Moving does not end it: a corpse run is movement. Leaving the
EliteMobs area ends it. The ticket lapses after
`return-window-seconds` (default **600**), quitting clears ticket
and grace, and a plain world-hop with no death grants nothing —
one death buys exactly one protected return, so there is nothing
to farm. Every text, sound and timing is a config knob. Wink Reyes
Valley runs this module at the shipped defaults.

### summon-mount — Summon Mount (default: ON; needs ExcelItems)

**The gap:** EliteMobs has no mounts of its own — nothing in
its worlds carries a player — so crossing a big EliteMobs
region means ender pearls, one throw at a time. Until
EliteMobs ships the native mounts its developer has
announced, this module is the interim answer; it retires
the day those land, by config switch, without a rebuild.

**The product:** the **Mount Charm** (ExcelItems registry
ID 5, key `mount_charm`), a reusable item: right-click and
your own horse arrives — tamed, saddled, yours alone (no
other player can ride or move it), invulnerable, and never
saved to disk, so it exists only while called. Right-click
again and it goes home. It also leaves on its own when you
die, log out or change worlds, and if it is lost any other
way, a short cooldown (`lost-horse-cooldown-seconds`,
default 60) is the whole cost — never the purchase.
Summoning is refused briefly after combat
(`combat-window-seconds`, default 20), and the charm does
not work inside instanced dungeons or the Adventurers
Guild hub. The module's own buy flow (`/mountcharm buy`,
with an arm-and-confirm step) charges the server's Vault
currency at the config prices, and a vendor NPC can be
wired to the same flow at the discounted `npc-price`.
Purchases check the buyer's balance first and refuse
cleanly if it is short — nobody is ever charged into a
negative balance.
ExcelItems is a softdepend of the pack and a hard
requirement of this module — without it the module stands
down and logs one line naming the missing dependency;
Vault is needed only for purchases. Wink Reyes Valley
runs this module at the shipped defaults.

### mcmmo-xp-connector — mcMMO XP Connector (default: ON; needs mcMMO)

**The defect:** when mcMMO runs beside EliteMobs, mcMMO pays weapon
XP for every point of damage dealt to an elite — and EliteMobs
elites carry health pools hundreds of times larger than vanilla
mobs. One boss fight pays more weapon XP than hours of ordinary
play, and the "optimal" way to level becomes standing in front of
the biggest health pool on the server: elite health size, not
player skill or time, decides who levels. Practice targets make it
worse — a training dummy is an elite with an enormous pool that
cannot fight back, built for testing damage, and it pays like a
raid. The pool size is also content data: any future content pack
can ship a bigger pool and silently re-break the whole curve.

**The fix:** XP from EliteMobs content is settled at the kill,
never per point of damage. Per-damage mcMMO XP from any
EliteMobs-tracked entity is zeroed in every world — for the earner
and for party shares of that gain — and the kill pays a single
award: a flat rate for the elite's class multiplied by its level.
The class comes from EliteMobs' own class-loot rank for bosses
(BOSS 100, MINIBOSS 40, TRASH 10); natural elites, which carry no
loot rank, use the natural-elite rate (20). The numbers are the
design's defaults, not one server's tuning: a level-10 boss pays
100 × 10 = 1,000 XP, a level-100 Warden-class boss pays 10,000,
and a level-1 trash elite pays 10. Pool size, fight length,
healing and damage dealt are all irrelevant — only class and level
count, so no content file can inflate the payout.

The details that keep the award honest:

- **Practice targets pay nothing.** Custom bosses whose EliteMobs
  file defines them as passive (AI off and damage multiplier 0.0 —
  the shape every practice dummy ships in) and bosses whose file
  name is in the training-dummy set (`training_dummy_lv*.yml`,
  `damage_test_dummy.yml`) pay no award and no per-damage XP.
- **A rolling-hour ceiling.** Awards to one player in one skill
  are capped at 60,000 XP per rolling hour (config:
  `hourly-ceiling`), counted after mcMMO's own multipliers and
  perks have composed — the ceiling sees the final number that
  lands in the profile, so bonuses speed a player toward the cap
  but never leak past it. An award that would cross the cap is
  trimmed to fit; the remainder is forfeited. The ledger is the
  module's own data file (`mcmmo-connector-ledger.txt` in the
  plugin folder), so the cap survives restarts.
- **The award lands in the killing blow's weapon skill** — the
  skill mcMMO itself credits for that weapon or shot. A kill by an
  adopted (tamed) pet pays its owner in Taming at the same class
  rates; a kill by a magic CAST summon pays nothing, because a
  cast summon is the caster's weapon, not a companion. Acrobatics
  is untouched.
- **No world gate.** Nothing in this module pays less XP
  because of the world a player stands in: ordinary mcMMO play
  (mining, fishing, gathering, vanilla combat) pays normal rates
  in every world, EliteMobs worlds included, and awards land the
  moment they are earned, wherever the killer is standing. (An
  award is only ever held if the mcMMO pipeline produces no gain
  for it at all — for example a Creative-mode player, or an
  mcMMO build that still gates worlds in its own pipeline; then
  the module holds it and pays it through the normal pipeline
  the moment a gain can land, with multipliers and party sharing
  applying as usual.)
- **The XP watchdog.** The module totals EVERY mcMMO gain per
  player and per skill over rolling minute and hour windows
  (saved to disk, so a restart does not reset them). Crossing a
  flag threshold appends a record — player, skill, window
  total, world, time, live EliteMobs hit tokens — to
  `mcmmo-watchdog-flags.txt` in the plugin folder and logs one
  warning line; a flag never blocks anything. Crossing the
  hourly cap zeroes further gains in that skill until the
  rolling hour falls back under it (a kill award arriving
  while capped is held and paid after roll-off, not lost).
  Every legitimate rate sits far below the shipped thresholds
  — the largest single award is 10,000 XP and kill awards are
  separately capped at 60,000 per hour — so the watchdog only
  ever trips on a gain path nobody foresaw, which is exactly
  what it is for.
- **Party sharing composes.** Awards flow through mcMMO's own
  pipeline, so a kill in a party shares by mcMMO's own rules;
  shares of per-damage gains from elites are zeroed like the
  earner's own gain, so a vetoed gain mints no shares.
- **Off is off.** With the module disabled (or either switch below
  off), mcMMO behaves exactly as it does without the module.

**The knobs** (config section `modules.mcmmo-xp-connector`):
`per-damage-veto` (zero per-damage XP from EliteMobs-tracked
entities, default true), `kill-awards` (pay the kill-settled
awards, default true), `rates.boss` / `rates.miniboss` /
`rates.natural-elite` / `rates.trash` (XP per elite level by
class, defaults 100 / 40 / 20 / 10), `hourly-ceiling` (award XP
cap per player per skill per rolling hour, default 60000; 0 turns
the cap off), `watchdog.enabled` (the XP watchdog as a whole,
default true), `watchdog.flag-minute-xp` (flag one skill gaining
this much in a minute, default 25000), `watchdog.flag-hour-xp`
(same for one hour, default 90000), `watchdog.cap-hour-xp` (zero
further gains in a skill whose rolling hour reaches this,
default 120000), `share-veto-budget` (how many party-share events
one landed hit can veto, default 8), `hit-token-ttl-ticks` (how
long a landed hit's veto marker lives, default 6 ticks), `debug`
(default false). The module keeps small data files in the plugin
folder: the ceiling ledger, the held-awards queue, the watchdog
ledger, and the watchdog flags file. Wink Reyes Valley runs this
module at the shipped defaults.

### quest-beacon — Quest Beacon Guide (default: ON; needs quest-destinations)

**The gap.** EliteMobs' tracker tells a player what to kill, but for the
tracker-blank quests (issue #241) it shows no destination at all, and even
a working tracker gives no always-on visual to follow on a long walk. The
quest-destinations module answers with text directions, coordinates and a
short glow; this module adds the missing travel visual.

**What it does.** While you have a quest selected (tracked) in EliteMobs'
tracker, a tall pillar of light marks that quest's destination — the same
destination the quest-destinations module computes from its table (read
through the pack's destination API, so this module idles when that module
is off or EliteMobs is absent, and a quest gets a beam exactly when the
table covers it — a data row, never a code change). The light is
per-player and purely visual: particles sent only to you. No beacon block
or pyramid is ever placed, nothing persists in the world, and no other
player sees your light. A real beacon beam is rendered by a ticking
beacon block entity, which a per-player fake block does not have, and an
end crystal's beam target renders only on Java — the particle pillar is
the mechanism that behaves identically on Java and Bedrock, so it is the
one shipped. From far away the pillar rides at the edge of your view in
the destination's direction, moving as you move; once the destination is
in range it stands on the destination itself. It ends when you untrack
or finish the quest, change worlds, or arrive. `/questwhere` and the
glow are unchanged — the beacon complements them.

**Commands.** `/questbeacon` (your beam's state), `/questbeacon off|on`
(your own beam, this session).

**Config** (`modules.quest-beacon`): `update-ticks` (refresh cadence,
default 10), `edge-distance` (how far the pillar rides from you, and the
settle range, default 48 — keep it inside your view distance),
`arrival-radius` (arrival ends the beam, default 8), `column-height`
(default 40) and `column-step` (mote spacing, default 2). Wink Reyes
Valley runs this module at the shipped defaults.

## What Wink Reyes Valley runs differently

This pack is developed on the Wink Reyes Valley family server. Where
our deployed values differ from the shipped defaults, both numbers are
stated — here and as comments beside the knob in the shipped config:

| Knob | Shipped default | WRV runs | Why |
|---|---|---|---|
| `arcane-bolt.ability-damage-scale` | 1.0 (buffs only) | **1.5** | WRV casters gear for spell power; 1.5 makes weapon investment matter without trivializing bosses. |
| `food-heal.debug` | false | **true** (temporarily) | Until the first natural live heal case is witnessed in the log; then it flips off. |
| `food-heal.worlds` | empty list | `em_primis`, `em_adventurers_guild` | Belt-and-braces: EliteMobs' own world registry already covers these; the names are listed as a fallback. |
| `leash.debug` | false | **true** (temporarily) | Until the first natural leash return is witnessed in the log; then it flips off. |
| `progress-tracker.kill-records-folder` | empty (no per-boss counts) | `ExcelProtect` | WRV's per-boss kill counts come from our own records plugin's folder; owners without one lose nothing else. |
| `quest-destinations.yml` data | blank template | WRV's 11-row Primis table | Our content's tracker-blank quests; every server fills in its own. |
| `drops-index.yml` / `tracker-catalog.yml` data | empty stubs | Generated from WRV's EliteMobs install | The indexes describe the server's own content. |
| `seal-restore.yml` data | empty | One row: a phase boss whose second doorway was never restored (30 blocks) | The defect that motivated the module. |
| `summon-mount.price` / `npc-price` | 7500 / 5625 | **15000 / 11250** | Luxury pricing: the charm is currently the only mount that works in EliteMobs worlds (Mike's ruling, Nov 23, 2026). |

Every other knob: WRV runs the shipped defaults.

## How it works / house rules

- **Fixes live in the box.** Every EliteMobs workaround we build from
  here on is a module in this plugin — never a one-off edit to
  EliteMobs' own content files — so the whole set installs, toggles and
  updates as one artifact. (One historical exception: a sleeping-boss
  wake fix that lives inside one of EliteMobs' own *power* files,
  because the power system cannot be reached from outside; it is a
  one-block addition documented in our build record, and the same fix
  was sent upstream as MagmaGuy/EliteMobs issue #242.)
- Modules are independent: the core loads each enabled module on its
  own, a disabled module's commands truly do not exist, and one
  module's data files are never read by another.
- Nothing in the shipped jar, code or default config names our server,
  our worlds or our content. Our values live only in our deployed
  config and data files.
- `/emfixes` shows module status; `/emfixes reload` re-reads the
  config (permission `excelemfixes.admin`).

## For developers

`excelemfixes.api.GlowApi` is the stable surface: check
`GlowApi.isAvailable()`, then `glowMarker(player, world, x, y, z, label)`
or `glowEntity(player, world, x, y, z, entityName)` (entity falls back
to a marker when the named entity is not there). It no-ops cleanly when
the quest-destinations module or its glow toggle is off.
