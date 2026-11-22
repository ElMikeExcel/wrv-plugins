# ExcelEMCompanion

One plugin, an open module framework (D186). Mike's direction:
"We'll probably keep adding things to this plugin so make sure
that's open." Each feature is a **module** — a self-contained
package with its own config toggle, commands and menus — and the
core (menu renderer, module lifecycle, dynamic command
registration) never changes when a module is added.

Modules shipping in 0.1.0:

| Module | Toggle | Commands | What it does |
|---|---|---|---|
| Drop Guide | `modules.drop-guide.enabled` | `/drops` | "What does THIS boss / THIS dungeon drop?" — Places → Mobs → full drop list (D185). No drop chances anywhere; class-loot drops state their stats roll at drop. |
| Progress Tracker | `modules.progress-tracker.enabled` | `/progress`, `/tracker` | A player's own progress: quests (completions + distinct), dungeons, per-boss kill counts and per-dungeon clears colored by expertise tier, combat totals, classes & skills. |

Both modules render identical content on Java (chest GUI) and
Bedrock (Cumulus forms via the server's GeyserForms pattern);
every menu and submenu has a Back button, root menus have Close.
Disabling a module in config removes its commands and menus
completely and leaves the other modules untouched (verified on
the mirror rig in all three toggle states).

## How to add a module

1. Create a package `com.winkreyes.excelemcompanion.<yourmodule>`
   with a class implementing `CompanionModule`:
   - `id()` — stable id, e.g. `"party-tools"`. Its config toggle
     is `modules.party-tools.enabled`.
   - `onEnable(ModuleContext)` — register your menu-action handler
     (`ctx.renderer().registerHandler("<prefix>", ...)`) and your
     commands (`ctx.registerCommand(...)`). Commands are
     registered dynamically: do NOT declare them in plugin.yml.
   - `onDisable()` — unregister your handler; commands are
     unregistered by the core automatically.
2. Add ONE line to `ModuleRegistry.all()`.
3. Add the module's config section under `modules:` in
   `src/config.yml` (with `enabled: true`).
4. Build menus as shared `Views` (content buttons + nav buttons)
   and open them through `ctx.renderer()` (`openRoot` for command
   entry points, `open` for forward navigation). Never render
   inventories or forms yourself — the renderer gives you Java +
   Bedrock parity, the Back/Close stack, and detail screens for
   free. Action strings are `<prefix>:...`; the prefixes `core:`
   (back/close/detail/noop) are reserved.
5. Put module data files (generated indexes etc.) in the plugin
   data folder with a bundled fallback in the jar, like the two
   existing modules do.

That's all. No core changes, no changes to existing modules.

## Data files and regeneration

Both data files are generated from the live EliteMobs configs so
they stay true as content changes. A copy placed in the plugin's
data folder overrides the bundled one (refresh without a rebuild:
regenerate, drop the file in, `/companion reload`).

```
# from a fresh pull of the live EliteMobs config tree into livepull/
python3 tools/generate_index.py livepull /tmp/materials.txt src/drops-index.yml GENERATOR-REPORT.md
python3 tools/generate_tracker_catalog.py livepull src/drops-index.yml src/tracker-catalog.yml TRACKER-CATALOG-REPORT.md
```

- `drops-index.yml` — every place (dungeons, adventure regions,
  event/folder content), every mob that drops anything, every drop
  (fixed, class-loot, quest, random). Drop chances are parsed only
  to be discarded: they are never written to the index.
- `tracker-catalog.yml` — quests (denominator derives from the
  configs: 111 player quests), the ordered main chain (31 files),
  the enabled instanced dungeons (14) with their worlds, objective
  bosses and boss files, and every place's bosses with filenames
  (for lockout mapping) and worlds (for ExcelProtect matching).

## Where the tracker numbers come from

- Completions / runs / kills / highest elite / deaths / skill XP /
  class XP: EliteMobs' own data (`PlayerData`, ACS profiles).
  Completions and runs are TOTAL counters — repeats count — and
  the menus label them that way.
- Distinct quests finished: the player's quest-cooldown registry
  (a quest enters it when finished). Distinct dungeons downed:
  the dungeon boss-lockout list. Both are labeled as such.
- Per-boss kills and per-dungeon clears: ExcelProtect's
  `entities-*.jsonl[.gz]` death records — a record whose killer is
  the player is one killing blow; a dungeon clear is a killing
  blow on the dungeon's objective (final) boss in its world.
  Records are matched to catalog bosses by world + cleaned
  nameplate. Where a boss cannot be matched reliably, the menu
  shows Downed (from lockouts) instead of a number — counts are
  never fabricated. ExcelProtect records on this server begin
  2026-11-02, before EliteMobs content opened (Nov 14).
- Expertise colors (D187): the count's color is the mastery —
  ×1 gray Novice, ×2–4 white Familiar, ×5–9 green Experienced,
  ×10–19 blue Veteran, ×20–39 purple Expert, ×40–74 gold Master,
  ×75+ light-blue Mastered (EliteMobs' quality ladder order). The
  legend rides in the bosses and dungeons menus on both platforms.

## Build & test

```
./run-tests.sh   # plain-JVM suites (137 checks)
./build.sh       # deterministic ExcelEMCompanion-0.1.0.jar
```

Mirror rig (Paper 26.2 + real EliteMobs 10.9.10 + live config
tree + real ExcelProtect records): `rig/run-scenarios.py` boots
three scenarios — full proof, drop-guide off, tracker off — and
writes receipts to `receipts/`.
