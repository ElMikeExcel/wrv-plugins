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
without Geyser everything works the same for Java players).

## Install

1. Put `ExcelEMFixes-0.1.0.jar` in your `plugins/` folder and restart.
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
