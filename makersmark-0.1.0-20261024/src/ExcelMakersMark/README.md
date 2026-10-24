# ExcelMakersMark 0.1.0

An Excel plugin for Wink Reyes Valley (Paper 26.x, Java 25, Geyser and Floodgate).

Crafted gear says who made it, with one gray lore line: "Crafted by <name>".
Optional counters: weapon kills, armor hits blocked and boss trophies.

## What players see
- Crafted by Alex
- Kills: 12 (Players: 1)          (weapons; the Players part only shows when above 0)
- Slain: Ender Dragon, Wither x2  (weapons)
- Hits blocked: 34                (armor)

## Commands
| Command | Who | What |
|---|---|---|
| /mark on, /mark off | everyone | sign your crafts or not (default on) |
| /mark stats on, /mark stats off | everyone | show and count gear stats or not (default on) |
| /mark ok | everyone | hide the tip for good |
| /mark reload | op | reload config.yml |
| /mark inspect <player> [slot] | op | print an item's mark and counters |
| /mark tipreset <player> | op | show the tip again |

Alias: /makersmark. Permissions: excelmakersmark.use (default true), excelmakersmark.admin (op).

## Placeholders (PlaceholderAPI)
- %makersmark_mark%: ON or OFF
- %makersmark_stats%: ON or OFF

## Which items
Signed and counted: tools, weapons, armor, shields, bows, crossbows, tridents, maces,
spears and elytra. Never: anything stackable, Magic (elmakers) items, EliteMobs items,
shop items (item tag namespaces listed in config.yml, exclude.pdc-namespaces).

## How it stays honest
All data lives in hidden item tags (namespace excelmakersmark). The lore lines are always
redrawn from the tags, and any lore that imitates them is removed at the anvil, smithing
table and grindstone. Renaming or lore editing cannot fake or remove a mark.

## Soft dependencies (all optional)
EssentialsX (nicknames), Magic (wand check), PlaceholderAPI (menu buttons), ExcelTips (tip),
Geyser and Floodgate (Bedrock tip form), EliteMobs and ExcelShops (only through item tags;
nothing in those plugins is changed).

## Build
local-build.sh: JDK 25, javac --release 21 against the Paper API, JUnit 5 unit tests,
deterministic jar (same source gives the same sha256).

## Not shipped
The test rig helper plugin (MMRigAid) and the Magic test wand files used on the test
server are test tools only and are not part of this jar or this install.
