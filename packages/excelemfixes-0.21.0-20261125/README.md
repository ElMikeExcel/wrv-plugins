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

1. Put `ExcelEMFixes-0.8.0.jar` in your `plugins/` folder and restart.
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

**Dialog destination derivation (0.19.0).** The table above only ever
covered tracker-blank kill quests - but a quest whose current
objective is a DIALOG ("go talk to X") had no destination at all when
it had no table row: on Wink Reyes Valley, 46 of 48 dialog quests had
no table row, so a tracked dialog quest got no quest beacon and
`/questwhere` had nothing to say. The report that surfaced it
(Jester, Nov 25, 2026): a carried quest's only objective was talking
to King Nemian, and there was no beam and no directions to him. The
module now derives the destination from EliteMobs' own data when -
and only when - a quest has no table row: while a player's quest has
an incomplete DIALOG objective, the destination is that objective's
target NPC, at its spawn location from EliteMobs' NPC registry (the
same source the quest-giver-glow module harvests). Because the
current incomplete objective is read each time, the beam follows the
current objective: when a dialog completes, the next incomplete
dialog objective takes over. Table rows keep precedence whenever a
row exists, because rows encode instance entry points EliteMobs'
data cannot express. Everything is derived at runtime, so no quest,
NPC or place is named in code and future content is covered the day
it lands - no hand-maintained rows duplicating data EliteMobs
already holds. The destination API
(`excelemfixes.api.QuestDestinationApi.destinationFor(file, player)`)
serves the player-aware resolution to the quest-beacon module and to
`/questwhere show`/`guide`; the derived destination's glow lights
the NPC itself. An NPC EliteMobs does not track, or one with no
parseable spawn location, derives nothing - never a guess.

Sub-toggles and commands:
- `modules.quest-destinations.enabled` — the whole module. Off = the
  `/questwhere` and `/questdest` commands do not exist.
- `modules.quest-destinations.derive-dialog-destinations` (default
  on) - the dialog derivation above. Off = the exact pre-0.19.0
  behavior: table rows only.
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

**Why it exists (player feedback):** casters reported that Arcane Bolt
and the other class abilities felt weaker with every gear upgrade.
They were right about the gear half: EliteMobs computes ACS ability
damage with the gear adjustment fixed at 1.0, so weapon hits grow
with gear and ability hits do not. EliteMobs' author agrees that is
a gap and is designing gear scaling for abilities (EliteMobs issue
#243).

**The fix:** the module watches EliteMobs' own ability events and,
for each ability hit on an elite, adds the missing scale as extra
damage: `extra = dealt × (ability-damage-scale − 1)`, where *dealt*
is the hit's final damage after everything EliteMobs itself applied
(default **1.0** = the module adds nothing; Wink Reyes Valley runs
**1.5**). The extra damage is a direct health adjustment, capped per
hit at a fraction of the target's max health
(`max-extra-fraction-of-max-health`, default 1.0). Because it is not
event damage, the extra share earns no loot share or threat in
EliteMobs' accounting — kill credit stays with the ability's own
damage. That trade-off is deliberate: dealing it as real damage
would send it back through EliteMobs' weapon formula and timed
modifiers and distort it.

**The 0.11.0 correction:** versions 0.1.0–0.10.0 also re-applied the
caster's timed damage buffs (Spell Echo), based on a decompile
reading that ability damage bypassed EliteMobs' damage events.
That reading was wrong. Measured on EliteMobs 10.9.14 with real
casts, ability damage flows through EliteMobs' damage event and
Spell Echo applies inside it (a 1.154× buff landed as exactly 1.154×
damage, tagged with the buff as its source) — and EliteMobs' author
confirmed the same from his own testing on issue #243. Re-applying
the buff counted it twice, so the buff half is removed (the old
`buff-application-enabled` key is ignored). The trigger also moved:
the old build watched the ability's carrier projectile hit, an
event EliteMobs' modeled-projectile path (FreeMinecraftModels,
which the mage class requires) never raises — on such servers the
old module silently never fired. It now pairs EliteMobs' own
ability events instead.

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

### melee-mitigation — Melee Mitigation (default: ON)

**What it does, in plain language:** a player who is holding a
melee weapon takes less damage. The weapon in the main hand is
the whole test — this is not tied to any class, skill, or player
list. A melee fighter has to stand where the hits land, so
holding a melee weapon takes a flat percentage off the damage
they take (default **15%**). An item in the off hand adds a
small extra slice, split by item: a shield or shield-like
defensive item adds **7%**, a totem adds only **2%** (a totem
already saves your life, negates a death and grants buffs, so
its slice stays small). The off-hand slice pays **only** while
the main hand holds a melee weapon — sword + totem earns the
2%, wand + totem earns nothing, because a wand is not a melee
weapon. While an off-hand slice is active, the off-hand item
itself says so in its lore (`7% mitigation activated` /
`2% mitigation activated`); when it stops being active the line
is gone and the item shows nothing. This module never heals
and never steals life — it only makes incoming hits smaller;
healing stays the healer's job.

**How it works:** the scaling reuses the TrainingWheels shield
shape — damage on the victim axis, multiplied by
(1 − total mitigation) inside the damage event itself at HIGH
priority, after armour has priced the hit, so health, death
and logs all see the one mitigated number. Every number is a
config knob (`base-percent`, `totem-bonus-percent`,
`shield-bonus-percent`, `max-total-percent`). Scope is
EliteMobs worlds (registry, config list, or the `em_` prefix),
with an `apply-in-all-worlds` switch; VOID and SUICIDE damage
are never mitigated. Which items count is data-driven and was
audited against the full EliteMobs custom-items tree: swords,
axes, maces, spears and scythes (EliteMobs builds scythes on
the hoe material family) count, plus a held trident; every
EliteMobs shield is the vanilla SHIELD material and its totems
are TOTEM_OF_UNDYING, and both families are config lists a new
custom item can join without code. One trap the audit caught:
EliteMobs builds its **staves on the WOODEN_SPEAR material**,
so a material-only test would call a staff a spear — the
module therefore asks EliteMobs' own magic-weapon identity
first, and a magic weapon (wand or staff) is never a melee
weapon, whatever its material says. Wink Reyes Valley runs
this module at the shipped defaults.

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
the player gets a short message naming the count and the full
claim path (`/elitemobs` → Skills → click the skill itself to
open its bonus menu → claim the [UNLOCKED] picks — the middle
step is the one players could not find, so the message spells
it out. Reworded in 0.9.0 after Jester's Nov 24, 2026 report:
he had unclaimed bonuses, got the nudge, and could not work
out where this unclaimed skill thing was claimed). Claiming
the bonuses stops the nudge on its own. The check is deliberately not world-gated: it is about the
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

**The premium tier (0.8.0):** the **Ghast Charm**
(ExcelItems registry ID 9, key `ghast_charm`) — a second
carry-and-toggle charm on the same discipline: right-click
calls your own **happy ghast**, a flying mount you ride and
steer with the game's own flight controls (the module does
no steering; a harnessed happy ghast is flown by its rider
in vanilla). It wears a harness — without one the ghast
cannot be ridden, so a harness that cannot be equipped
refuses the summon instead of leaving a grounded mount.
The ghast obeys everything the horse obeys — the combat
gate (combat state is shared between the charms, so a
fight bars both), the blocked worlds, the follow rule,
the loss cooldown (its own clock), the retirement switch —
and it materializes a few blocks above the player, because
a ghast's body is four blocks wide. It needs a current
server: the rideable ghast and its harness exist only in
recent Minecraft versions, and the entity is resolved at
runtime (`ghast.entity-type`), so on an older server
summons refuse cleanly with the reason in the log. The
tier has its own switch (`ghast.enabled`) and its own
command (`/ghastcharm`, same buy/confirm shape, same
two venues — list price in a shop, the vendor price at an
NPC wired to the console form).

**Flight speed:** a vanilla happy ghast is slow — its
flying-speed attribute is 0.05, which cruises at only
~3.6 blocks/second, slower than a sprinting player and
barely a third of this module's horse (movement speed
0.29, ~12.2 blocks/second ridden). The `ghast.flying-speed`
knob overrides the attribute at summon (0 = vanilla).
Flight speed grows with the **square** of the attribute —
it scales both the rider's input and the travel
acceleration — so tune in small steps: 0.10 is roughly
14.4 blocks/second, about four times the vanilla cruise.
Wink Reyes Valley runs **0.097** (~13.6 blocks/second,
about 11% over its horse): the premium tier cruises
slightly faster than the horse, per Mike's ruling of
Nov 23, 2026. The figure was measured on the mirror rig
by stepping the game's own travel code (a ridden vehicle
is client-authoritative, so the server never simulates
it); the same rig reproduced vanilla's published
3.611 blocks/second at 0.05 exactly.

**The quest gate:** the Ghast Charm is earned as well as
bought. Buying requires finishing at least a configured
fraction (default one half) of the quests in **one**
package — halfway through a single adventure, never a
little of several added together. Quest totals are
counted from EliteMobs' own quest files (enabled quests
only) at enable and on reload; completions are read from
EliteMobs' quest-cooldown registry, the record it writes
on every completion. Packages are pure config — a list of
quest-filename prefixes (`ghast.gate.packages`) — so a
new content package enrolls with no rebuild. The gate is
checked when a purchase is armed and again when it is
confirmed, and a refusal quotes the player's real progress
in their best package. If the gate cannot be read at all
(EliteMobs absent or its data unreadable), sales refuse:
an earned item is never sold on a gate nobody could read.
The shipped config defines no packages — quest content is
the server's own — so a stock install logs plainly that
the charm cannot be bought until the owner defines one.
Wink Reyes Valley defines the Primis package (105 enabled
quests when the gate was built, so halfway is 53
completions); no other WRV content package ships quests
yet.

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
ledger, the watchdog flags file, and the EM-world ceiling
ledger. Wink Reyes Valley runs this module at the shipped
defaults.

### EM-world gain ceiling (part 5 of mcmmo-xp-connector)

**The defect:** mcMMO's Dodge pays XP per point of damage dodged —
800 XP per point at the stock rate — and EliteMobs elites deal
EliteMobs-sized damage, so a dodged boss hit pays like a small quest.
On Nov 24, 2026 a player gained 90,646 Acrobatics XP in one rolling
hour in Primis doing nothing but ordinary quests (the XP watchdog
flagged the rate; the play itself was legitimate). The kill-award
ceiling never counted these gains because they are not awards, and
the Nov 23 rebalance deliberately removed the old blanket world
gates, so nothing bounded the source at all.

**The fix (Mike's ruling, Nov 24, 2026 — sized from the server's
own ExcelProtect-era records):** Acrobatics gains earned *in
EliteMobs worlds* count toward their own per-player rolling-hour
ceiling, shipped at **100,000 XP** — about 10% above the heaviest
legitimate hour ever measured here, so ordinary play is never
trimmed, while a deliberate dodge-farm is bounded well under the
watchdog's 120,000 all-worlds hard cap. A crossing gain is trimmed
to the remaining budget rather than lost whole; once the budget is
spent, further in-world gains pay 0 until the hour rolls back
under. Gains in ordinary worlds are never counted and never
touched, gathering/mining/fishing in EliteMobs worlds still pay
normal rates, and kill awards are unaffected (Acrobatics is never
an award skill). The first time the ceiling bites in an hour, a
record lands in `mcmmo-watchdog-flags.txt` alongside the watchdog's
own flags.

**Knobs** (under `modules.mcmmo-xp-connector.em-world-ceiling`):
`enabled`, `skills` (default just ACROBATICS), `hourly-xp`
(default 100000 — Wink Reyes Valley's sizing from its own
measured play; another server should size this from its own
watchdog data, not copy the number), and `extra-world-names` /
`extra-world-prefixes` for a future content pack whose world
name breaks the built-in pattern (every `em_` world and the
Goblin Kingdom world are recognised out of the box).

### quest-beacon — Quest Beacon Guide (default: ON; needs quest-destinations)

**The gap.** EliteMobs' tracker tells a player what to kill, but for the
tracker-blank quests (issue #241) it shows no destination at all, and even
a working tracker gives no always-on visual to follow on a long walk. The
quest-destinations module answers with text directions, coordinates and a
short glow; this module adds the missing travel visual.

**What it does.** While you have a quest selected (tracked) in EliteMobs'
tracker, a tall pillar of light marks that quest's destination — the same
destination the quest-destinations module resolves (read through the
pack's destination API, so this module idles when that module is off
or EliteMobs is absent). A quest gets a beam when the resolution
covers it: a table data row, or - since 0.19.0 - a destination
derived from the tracked quest's current incomplete DIALOG objective
(the target NPC's spawn, from EliteMobs' own NPC data) when the table
has no row for it; the derivation is the quest-destinations module's
`derive-dialog-destinations` toggle, and the beam follows the current
objective as dialogs complete. The light is
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

### quest-giver-glow — Quest-Giver Glow (default: ON)

**Why this exists.** A player asked for it — Jester on Wink Reyes
Valley, Nov 24, 2026: "can you make a menu or something so that i
can see the nearest quest giving npc (its kinda hard to find them
as you have to get close and sometimes just right click them to see
if you have a quest or not)". The server owner's direction: no
menu — quest-giving NPCs glow, per-player, for whoever currently
has a quest from them.

**The problem.** EliteMobs quest NPCs give no sign that they have a
quest for you until you walk up and right-click them. In a big quest
world there is no way to tell, from any distance, which of the many
NPCs around you is worth visiting — players click every NPC they
pass, or miss quests entirely.

**What it does.** Every refresh, the module asks EliteMobs' own
data — through EliteMobs' own accept check, the same one it runs
before offering a quest — which NPCs have a quest this player can
pick up right now, and lights exactly those NPCs up **for that
player only**. The light is a per-viewer entity-metadata packet
carrying the glow flag, not a real glow: the NPC lights up on your
screen and stays plain on everyone else's, so a player with
nothing to pick up sees a completely plain world, and nobody's
available quests are ever advertised to other players. A second
color marks an NPC you are carrying a finished quest back to;
when both are true at one NPC, the turn-in color wins. A third
color (0.17.0) marks the NPC a carried quest's outstanding
DIALOG objective sends you to talk to.

**Why the third state exists.** Jester's Nov 25, 2026 report:
he was carrying "The groves call!", whose only objective is
talking to King Nemian, and Nemian was not glowing — every
quest Nemian offers was chain-locked for him (no accept
light), and the turn-in light for that very quest can only
switch on after the talk it was meant to guide him to. A
glow/beacon audit the same night counted 71 dialog objectives
across 48 of 105 Primis quests, pointing at 64 NPCs; 29 of
those NPCs gave no quests and took no turn-ins, so they sat
outside this module's NPC world entirely and could never glow
for anyone. The objective state closes that hole: while a
player carries a quest with an incomplete DIALOG objective,
that objective's target NPC lights for that player only, in
the objective color. The NPC world now extends to dialog
targets of loaded quests — they are ordinary EliteMobs NPCs
and resolve through the same spawn-event / registry-harvest /
anchor-adoption machinery as givers and turn-ins; an NPC
EliteMobs itself does not track stays unlit, the same
truthful boundary as the other states. Precedence at one
NPC: turn-in green wins, then objective, then accept gold
(the carried quest is the player's declared intent). The
state is toggleable on its own (`objective-enabled`) and its
color is its own knob (`objective-color`). Quests you
already hold, quests you finished for good, and quests still in
their repeat lockout never light anything. Availability is never
re-derived here: accept permissions (including the chain gates
quest packs hang on earlier completions) and lockouts are
whatever EliteMobs itself says they are. Everything is read from
EliteMobs' loaded registries at runtime — NPCs, their quest
lists, turn-in links — so the module knows no quest, NPC or place
by name and works for any EliteMobs content, expansions included.
EliteMobs' dynamic quest givers (NPCs that invent offers on the
spot when talked to) hold no quest list in EliteMobs' data, so
there is nothing truthful to light for them; they never glow
here. Which body is which NPC is learned from EliteMobs' own
NPC spawn records — an NPC body is a plain villager with no
name of its own in the world data, and its floating nameplate
is a per-player visual that does not exist server-side, so the
spawn record is the only truthful identification there is. (An
NPC already standing when the module is enabled is adopted
from the post its data names, where EliteMobs quest NPCs
stand.) If the packet bridge cannot resolve on a future server
build, the module idles quietly — no glow, no errors.

**Commands.** None — it is a background fix.

**Config** (`modules.quest-giver-glow`): `refresh-ticks` (how
often lights are re-checked and refreshed, default 40 = 2
seconds), `view-radius` (how close to a quest NPC you must be
before it can light, default 64 — keep it inside your view
distance), `anchor-radius` (how far from its data home spot
the body on an already-spawned NPC's post may stand and still
be adopted as that NPC, default 8), `accept-color` (default
gold) and `turn-in-color` (default green), `objective-enabled`
(the dialog-target state, default true) and `objective-color`
(default aqua). Wink Reyes Valley
runs this module at the shipped defaults.

### clarity-menus — EliteMobs, Made Clear (default: ON)

Not a defect fix — a companion feature that lives in the box.
**Why it exists:** on Nov 24, 2026, an experienced MMO player on
our server (Jester) reported he could not find the "unclaimed
skill" bonuses our skill-bonus-nudge told him about — he had 12
earned picks waiting, and no EliteMobs screen ever states the
claiming step. A clarity audit of every EliteMobs menu followed
(findings F1–F9: the Skills page reads as a stats list and calls
the same thing "passive skills" while the claim screen calls
them just "skills"; the quest screen's "Accepted!" status line
secretly starts quest-abandoning; both shops' reroll button is
an unlabeled ad head; bonus types are named and never
explained; and a tail of wrong-verb cancel buttons). The server
owner's direction was to fix everything, remaking the menus
ourselves if that is what it takes.

**What it does:** `/emguide` opens "EliteMobs, Made Clear" — a
plain-language front door with four pages. **Skills & Skill
Bonuses** spells out the claim path step by step (Claim
button → pick the skill → click each [UNLOCKED] bonus) and
explains the five bonus types in plain words. **Quests** covers accept / track / turn in, carries the
"Accepted!" warning with the safe way to check a quest, and
routes to `/questwhere` for tracker-blank quests. **Shops &
Elite Coins** explains Elite Coins, the set shop vs the rolling
shop, the reroll button, and selling. **Gear Tools** explains
repair, enchant, elite scrolls and unbind in one line each.
Every page's route buttons close the menu and open the matching
EliteMobs screen for the player, so the explanation is always
one tap from the real thing.

**0.13.0 — the Claim button (the reason this version exists):**
on Nov 24, 2026, the same player reported the rest of the
story: EliteMobs' normal `/em` menu is a dialog whose Skills
page only lists levels and XP — nothing on it can be clicked,
and the skill-bonus claim screens exist only in EliteMobs'
alt (chest) menu style. So the guide's old "Open the
EliteMobs menu" route (bare `/em`) and the skill-bonus
nudge's wording both landed players on the one page that
cannot claim anything. The Skills page now carries a
**Claim skill bonuses** button that opens EliteMobs' own
skill-bonus screens directly, through EliteMobs' public menu
opener (a route kind of its own — `em-menu` — since no
command can reach those screens; if a future EliteMobs moves
the opener, the button falls back to naming the
`/elitemobs alt` path). The nudge's text and click target
were reworded the same way. EliteMobs' view-only dialog
Skills page is EliteMobs' own gap, not something wording can
fix; the wider menu-replacement design lives in the server's
feasibility record (em-menu-replacement, Nov 24, 2026).

Sub-toggles and commands:
- `modules.clarity-menus.enabled` — the whole module. Off = the
  `/emguide` command does not exist.
- Commands: `/emguide`, `/emguide <page>` (page: home, skills,
  quests, shops, gear).

All wording lives in `clarity-menus.yml` (bundled; a copy in
the plugin data folder overrides it), so the text can be
reworded without a rebuild. Menus render identically on Java
and Bedrock through the pack's shared renderer; every page
carries a Back button and the home page carries Close.

### food-stack-normalize — Food Stack Normalize (default: ON)

**The defect (player report, Nov 24, 2026):** a player reported
that some of his items would not stack with each other — his
Fresh Rats sat in four separate piles (7 / 1 / 2 / 1) that
refused to merge. The cause is EliteMobs' Advanced Combat food
handling. While a player is in advanced combat, EliteMobs
rewrites every food item they carry: it backs the item's real
food values up into its own `advanced_combat_food` data keys
and swaps in a combat form (always edible, double the
consumption time). It restores each item when combat ends —
the copies its own passes touch. A copy that slips past those
passes (stored in a chest mid-combat, saved into a logout,
picked up at the wrong moment) keeps the combat form forever,
and because Minecraft stacks only identical items, it never
merges with resting copies of the same food again. Every
edible EliteMobs drop has the same exposure (Fresh Rat, Rat
Meat, …), and so does ordinary food carried through a fight.

**The fix:** this module finishes the restore EliteMobs
started. Any stack still carrying the combat-food stamp is
returned to its resting form — the same transformation
EliteMobs' own restore performs, using the backed-up values
EliteMobs itself stored (an item with no food of its own goes
back to having none, so its material's vanilla values apply; an
item with food of its own gets its exact component back,
including its own always-edible flag; the consumption time
returns to the backed-up original). The normalization runs
when a stamped stack appears in the world, when a player picks
it up, shortly after a player joins (inventory and ender
chest — EliteMobs' own join pass never looks in the ender
chest), and when a container is closed (chests are where
stamped copies hide longest). After a sweep, identical copies
in the swept inventory are merged into full stacks, so
already-split piles converge on the spot.

**Why this direction is safe:** the stamp's nutrition and
saturation are copies of the item's own resting values, so a
normalized copy nourishes exactly as much as a stamped one;
the combat form's only real deltas — always-edible and a
slower eat — are EliteMobs' to apply during combat, and
EliteMobs re-applies them to the whole inventory at every
combat start regardless. Normalizing everything UP to the
combat form instead would export always-edible, slow-eating
food to every chest and ground drop on the server, and
EliteMobs would still strip it at each combat end. The
resting form is the fixed point both systems agree on, and
EliteMobs' restore on an already-resting item is a no-op, so
the two can never fight. Stacks with no stamp are never
touched, and with the module off, stamped stacks simply stay
as EliteMobs left them.

Sub-toggles (all default ON):
- `modules.food-stack-normalize.normalize-on-pickup` — normalize at pickup.
- `modules.food-stack-normalize.normalize-ground-items` — normalize item drops as they appear.
- `modules.food-stack-normalize.sweep-on-join` (+ `join-sweep-delay-ticks`) — the join sweep.
- `modules.food-stack-normalize.sweep-on-inventory-close` — the container-close sweep.
- `modules.food-stack-normalize.merge-stacks` — merge identical copies after a sweep.

One honest limit: this module removes the *food-stamp* cause
of split stacks. A stack can also be split by other data
differences (another plugin's per-stack tracking stamp, a
different soulbind owner, wear) — those copies still will not
merge, by design: they are not the same item.

### skills-menu — Skills Menu (default: ON)

**Why this module exists (player report, Nov 24, 2026):**
Jester reported that EliteMobs' normal menus never let him
switch a skill bonus on. He was right: the dialog menu's
Skills page (`menus/player_status_screen.yml`) defines only
header/level/XP text formats — it is a view-only list with no
activation path at all. The one working screen is EliteMobs'
"alt" chest-style menu (`/elitemobs alt` toggles the style;
its Skills screen's click handler does the switching) — a
container GUI that Bedrock players get as a clunky emulated
inventory. Mike's direction the same day: build the skills
screen ourselves, the way our other menus work — one menu
definition that renders as the appropriate format per
platform. (The configuration shortcut — forcing EliteMobs'
`onlyUseBedrockMenus` on — was considered and declined: it
changes every EliteMobs menu for every player to work
around one broken page.)

**What it does:** `/skillmenu` (alias `/skills`) opens "My
Skills & Skill Bonuses" — every skill with the player's
level, XP and active-bonus count; tapping a skill opens its
bonus screen: every bonus with its unlock tier (10 / 25 / 50
/ 75), locked vs unlocked by the player's level, and its
active state (carried in the button name itself, so Bedrock
form users — who have no hover lore — can read it). Tapping
a bonus switches it on or off. The screen is also one tap
from `/emguide`'s Skills page ("My skill bonuses").

**The switching is EliteMobs' own.** The toggle performs
the exact sequence EliteMobs' own menu click handler
performs (bytecode-read from the live jar): derive the
level from total XP (`SkillXPCalculator.levelFromTotalXP`
over `PlayerData.getSkillXP`); refuse with EliteMobs' own
requires-level message when the level is short; when the
bonus is active, `PlayerSkillSelection.removeActiveSkill`
+ EliteMobs' deactivated message + `SkillBonus.onDeactivate`;
when all 3 slots (EliteMobs' `MAX_ACTIVE_SKILLS`) are used,
EliteMobs' max-active message; otherwise
`PlayerSkillSelection.addActiveSkill` + EliteMobs' activated
message + `SkillBonus.onActivate`. Selection state lives in
EliteMobs' own store (it persists through
`PlayerSkillSelection`'s database save), so EliteMobs' own
screens and this one always agree. Nothing is forged, no
EliteMobs file is edited, and there are no tuning knobs —
levels, tiers, the cap and the messages are read from
EliteMobs live.

**Dual format, one definition:** the screens are built as
the pack's shared view model and rendered by the shared
renderer — a chest GUI on Java, a native Cumulus SimpleForm
on Bedrock (the ExcelMenu / ExcelBedrockMenu pattern, via
the pack's reflection bridge to Geyser — no new dependency).
Every screen follows the pack's menu law: the overview is
the root (Close), every per-skill screen carries Back, and
after a toggle the screen refreshes in place (on Bedrock a
form closes on selection, so the refresh is also what keeps
the player in the flow; EliteMobs' own screen reopens itself
after a toggle too).

**Fallback:** if EliteMobs is absent, or a future EliteMobs
moves the bridged classes, opening the menu degrades to
EliteMobs' own chest claim screens (the clarity-menus
route), and failing that to a printed manual path — never
an error in a player's face. With the module off, nothing
changes: `/skillmenu` does not exist and EliteMobs' screens
are untouched.

Sub-toggles: none — the module is a screen, not a rule.
- `modules.skills-menu.enabled` — the whole module. Off =
  `/skillmenu` and `/skills` do not exist.

### quest-chain-clarity — Quest Chain Clarity (default: ON)

When a player clicks an EliteMobs quest NPC whose quest sits
behind earlier quests, EliteMobs answers with a single generic
red line — "This NPC has some quest(s) you can't accept yet!
Finish the quests that come before them first." — and names
none of them. On Nov 24, 2026 a player clicked Alanus Secundas
at the Pontis Groves lighthouse, got exactly that line, and had
no way to learn in game that the road there runs through King
Nemian's quests in the Groves; he had to ask a human. This
module adds the missing sentence: one line per locked chain
naming the NEXT quest the player can actually go and do — the
deepest unfinished link of the chain — with the NPC who gives
it and the place that quest's own text names when the content
provides one ("Locked: finish 'Population Control!' first —
King Nemian gives it."). EliteMobs has no quest-refusal event
(its api/ events cover accepts and completions only), so the
module observes the same click EliteMobs does, at MONITOR
priority, and replicates only the accept-permission segment
of EliteMobs' own accept check from its loaded quest data —
a player's progress is read from the same completion markers
EliteMobs' check reads. EliteMobs' own message is never
altered or blocked, and quests refused for any other reason
(already finished, already carried) earn no line. Everything
is computed from whatever content is loaded; the module knows
no quest, NPC or place by name. With the module off, nothing
changes at all.

**Completion signposting (0.18.0).** The click line covers a
player who goes looking; it does nothing for a player who
finishes a quest and unlocks the next one somewhere else.
The Nov 25, 2026 quest-guidance audit counted 14 such chain
starts in Primis whose prerequisite ends 151–531 blocks from
the new quest's giver, with nothing in the completion text
pointing onward. So the module also listens for EliteMobs'
own quest-completion event and, once the completion's end
permissions have landed, tells the player which quests that
completion just unlocked — same index, same style: "New
quest unlocked: 'Population Control!' — King Nemian gives
it." Only quests the completion directly unblocked are
named; a quest still missing another link stays silent
until its turn, and a quest already finished once or
already being carried is not news.

Sub-toggles:
- `modules.quest-chain-clarity.enabled` — the whole module.
- `modules.quest-chain-clarity.cooldown-seconds` — how long
  before the same NPC explains itself to the same player
  again (default 30).
- `modules.quest-chain-clarity.max-lines` — the most
  explanation lines one click can produce, and the most
  unlock lines one completion can produce (default 3).
- `modules.quest-chain-clarity.signposting-enabled` — the
  completion unlock messages (default true).
- `modules.quest-chain-clarity.debug` — log each explanation
  as it is sent.

### caster-xp-sharing — Class XP Sharing (default: ON)

EliteMobs' Advanced Combat System gives every class form
two "foundation skills" and sets the class level to the
LOWER of the two. Each track earns XP only from its own
weapon — a wand main earns zero staff XP — so the class
pauses at the lagging skill's level, and EliteMobs
discards (does not bank) class XP earned past the cap. On
Nov 25, 2026 a player reported exactly that: his
Spellcaster class "paused at spellcaster 12 because
staves is the limiting requirement — the staff and wand
exp dont share nor do they level equally", after fighting
a level-25 elite at an effective level of 10; his WANDS XP
was 240,760 (level 18) against STAVES 41,791 (level 12).
The system is alpha with a fixed baseline, so EliteMobs
exposes no setting for any of it. The server owner's
ruling (D242): let the PLAYER choose how the two tracks
share XP, and close the existing gap once for players
already caught by it. That shipped in 0.20.0 for the
Spellcaster pair (WANDS + STAVES), under this module id
and the display name "Caster XP Sharing".

The owner then asked what else of the same shape had
been missed, and a hidden-mechanics audit of EliteMobs
(jar bytecode + live data, Nov 25, 2026) found the min()
rule is universal: all 76 class forms in EliteMobs'
built-in catalog advance on the lower of two foundation
skills, and the pair CHANGES from form to form in every
tree except Spellcaster — Paladin's root pair is ARMOR +
SWORDS, Berserker's is AXES + MACES, Cleric's is MACES +
TRIDENTS. A second player was already capped by it: a
Paladin could not pass level 10 because his ARMOR (level
10) trailed his SWORDS (level 13). The owner's second
ruling (D243): extend the toggle to ALL classes. Since
0.21.0 the module covers every form: the pair is resolved
from the player's selected form against the audit's
probed catalog, the catch-up fires once per class TREE,
the skills-menu row sits on every skill page, and the
display name is "Class XP Sharing". The module id, its
config section and its data file keep the shipped
`caster-xp-sharing` names, and 0.20.0's stored records
are migrated on load (below).

Each player picks one of three modes — STOCK (EliteMobs
default, no intervention), SHARED (every gain on either
of their form's two foundation skills is split across
both tracks; the total is preserved), or CARRY (gains on
the leading track also grant the same XP into the
lagging track, so the class minimum rises with the
player's main weapon) — from a "Class XP Sharing" row on
every page of the skills menu (see skills-menu above),
or with `/classxp` (alias `/casterxp`). The mode is one
global per-player setting; the pair it applies to
follows the player's selected form. EliteMobs' class
formula is never edited: the module listens to
EliteMobs' own skill-XP-gain event (fired before the
gain is applied, with the final amount) and grants the
offsetting XP through EliteMobs' own public `PlayerData`
add call — the same call EliteMobs' own XP handler
makes, which fires no event, so the grants cannot
recurse and every EliteMobs screen agrees with the
result. The first time a player picks a non-STOCK mode
with a form of a class tree selected, a one-time
catch-up for that tree raises the lagging track of their
current form's pair to the leading track's XP (198,969
XP in the reported case); the grant is recorded per
player per tree in the module's `caster-xp-sharing.yml`
data file — when, which form, which track, how much —
and can never fire twice for that tree; if the grant
cannot land it stays owed rather than being marked
spent. Mode changes take effect immediately, no relog.
With the module off, or a player in STOCK, EliteMobs
behaves exactly as shipped.

Migration from 0.20.0: stored modes carry over as-is.
0.20.0 recorded its single (Spellcaster) catch-up in
flat fields; on load, a completed 0.20.0 catch-up
becomes the spellcaster tree's record, so it can never
fire again, and an unfired one stays owed. Saving
mirrors the spellcaster record back into the legacy
flat fields, so a rollback to 0.20.0 keeps honoring it.

Sub-toggles:
- `modules.caster-xp-sharing.enabled` — the whole module.
  Off = `/classxp` does not exist, the skills-menu row is
  gone, and no gain is ever touched.
- `modules.caster-xp-sharing.default-mode` — the mode a
  player who has never chosen gets (default STOCK).
- `modules.caster-xp-sharing.catch-up-enabled` — the
  one-time per-tree gap-closing grant on first non-STOCK
  selection (default true).
- `modules.caster-xp-sharing.mode-set-message` /
  `catch-up-message` — the two player-facing messages.
- `modules.caster-xp-sharing.debug` — log each shared or
  carried gain and each mode change.

## What Wink Reyes Valley runs differently

This pack is developed on the Wink Reyes Valley family server. Where
our deployed values differ from the shipped defaults, both numbers are
stated — here and as comments beside the knob in the shipped config:

| Knob | Shipped default | WRV runs | Why |
|---|---|---|---|
| `arcane-bolt.ability-damage-scale` | 1.0 (module adds nothing) | **1.5** | WRV casters gear for spell power; 1.5 makes weapon investment matter without trivializing bosses. |
| `food-heal.debug` | false | **true** (temporarily) | Until the first natural live heal case is witnessed in the log; then it flips off. |
| `food-heal.worlds` | empty list | `em_primis`, `em_adventurers_guild` | Belt-and-braces: EliteMobs' own world registry already covers these; the names are listed as a fallback. |
| `leash.debug` | false | **true** (temporarily) | Until the first natural leash return is witnessed in the log; then it flips off. |
| `progress-tracker.kill-records-folder` | empty (no per-boss counts) | `ExcelProtect` | WRV's per-boss kill counts come from our own records plugin's folder; owners without one lose nothing else. |
| `quest-destinations.yml` data | blank template | WRV's 11-row Primis table | Our content's tracker-blank quests; every server fills in its own. |
| `drops-index.yml` / `tracker-catalog.yml` data | empty stubs | Generated from WRV's EliteMobs install | The indexes describe the server's own content. |
| `seal-restore.yml` data | empty | One row: a phase boss whose second doorway was never restored (30 blocks) | The defect that motivated the module. |
| `summon-mount.price` / `npc-price` | 7500 / 5625 | **15000 / 11250** | Luxury pricing: the charm is currently the only mount that works in EliteMobs worlds (Mike's ruling, Nov 23, 2026). |
| `summon-mount.ghast.price` / `npc-price` | 30000 / 22500 | **30000 / 22500** (same) | The flying tier's set prices (D204, Nov 23, 2026): premium above the horse, with the standing 25% vendor-NPC discount. |
| `summon-mount.ghast.gate.packages` | *(none defined)* | **primis (prefix `primis_`)** | WRV's only quest package today (105 enabled quests, halfway = 53). Shipped empty because quest content is the server's own. |
| `summon-mount.ghast.flying-speed` | 0 (vanilla 0.05, ~3.6 blocks/s) | **0.097** (~13.6 blocks/s) | Mike's ruling (Nov 23, 2026): the flying tier cruises slightly faster than the module's horse (~12.2 blocks/s at movement speed 0.29). Flight speed grows with the square of the attribute — see the ghast tier section above. Rig-measured. |

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
