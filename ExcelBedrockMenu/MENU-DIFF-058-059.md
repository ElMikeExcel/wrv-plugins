# MENU-DIFF-058-059 — what this packet changes in the menus

**Base:** the GuildGate packet's prepared `server_menu.yml`
(`~/workspace/excelguildgate/install-prep/server_menu.yml`), which itself
is not live yet. This packet is third in the install chain (GuildGate
packet, then RewardGuard 1.2, then this one), so its `server_menu.yml`
starts from that file, never from the currently live menu.

## server_menu.yml: exactly two items added, nothing else touched

An automated check deep-compares every shared item against the base
file: title, size (27) and all ten existing items are unchanged. The two
additions fill the two empty slots in the middle row, so the row reads
9 commands, 10 spawn, 11 shop, 12 guild, 13 seals, 14 mcmmo, 15 info,
16 autojoin, 17 website.

**1. `commands` (new, slot 9)** — WRV-058:

```yaml
  commands:
    material: KNOWLEDGE_BOOK
    slot: 9
    display_name: '&a&lServer Commands'
    lore:
      - '&7Every command you can use'
    left_click_commands:
      - '[openguimenu] commands_menu'
```

**2. `website` (new, slot 17)** — WRV-059:

```yaml
  website:
    material: COMPASS
    slot: 17
    display_name: '&a&lServer Website'
    lore:
      - '&7Guides, commands and news'
      - '&7Tap for the address'
    left_click_commands:
      - '[json] {"text":"» Server Website: elmikeexcel.github.io/winkreyesvalley","color":"aqua","underlined":true,"clickEvent":{"action":"open_url","value":"https://elmikeexcel.github.io/winkreyesvalley/"},"hoverEvent":{"action":"show_text","value":"Open the server website"}}'
      - '[message] &bhttps://elmikeexcel.github.io/winkreyesvalley/'
```

The `[json]` line is the clickable one (DeluxeMenus 1.14.1 documents the
`[json]` action and its message code has a JSON send path; MiniMessage
shading is in the jar as a second data point). The `[message]` line
prints the raw address, which the vanilla Java client makes clickable
itself, so the button delivers a working link even if the JSON form is
ever rejected. Both lines firing is expected: a styled link line, then
the plain address.

## commands_menu.yml: NEW FILE

`plugins/DeluxeMenus/gui_menus/commands_menu.yml`, opened by the slot-9
button via `[openguimenu] commands_menu`. It lists the same 44-entry
catalogue as `/servercommands` (see COMMANDS-CATALOG.md): 54-slot chest,
51 items (7 group headers + 44 entries), packed layout:

| Slots | Content |
|---|---|
| 0, 1-8 | Getting Around header + 8 entries |
| 9, 10-16 | Money & Jobs header + 7 entries |
| 18, 19-23 | Skills & Levels header + 5 entries |
| 24, 25-29 | Shops & Trading header + 5 entries |
| 30, 31-37 | Claims & Land header + 7 entries |
| 38, 39-42 | Guild & Quests header + 4 entries |
| 43, 44-51 | Server Info header + 8 entries |
| 17, 52, 53 | blank |

Item behaviour mirrors the other surfaces: entries that are safe bare
run `[player] <command>`; display-only entries answer with
`[message] &7Type it in chat: &e<command>`; headers have no click
commands. Every entry's lore carries its one-line description and its
group name.

The file is generated from the catalogue table by
`build/gen_commands_menu.py` and verified against the Java `CATALOG`
literal triple-for-triple by `build/verify_menus.py`, so the Java menu,
the Bedrock forms and this file cannot drift apart silently.

## DeluxeMenus config.yml: one registration added (required)

DeluxeMenus 1.14.1 only loads menus registered in the `gui_menus:`
section of `plugins/DeluxeMenus/config.yml`; a file sitting in
`gui_menus/` unregistered is silently ignored. The live config registers
8 menus (basics_menu, server_menu, spawnshop and its 5 categories),
which matches the live log's "8 menus loaded". The install must add:

```yaml
  commands_menu:
    file: commands_menu.yml
```

under `gui_menus:` (after the `server_menu` entry, same 2-space style).
`server_menu` needs no change: it is already registered live.

This step was caught by the scratch boot test, not by reading: the first
boot loaded only the 3 example menus and `dm list` showed neither
packet file; after adding the registration the same boot loaded 5 menus
and `dm list` listed both `server_menu` and `commands_menu`. Without
this step the Commands button would have opened nothing.

## How to apply at install time

1. Confirm the chain position: the GuildGate marker
   (`excelguildgate/install-prep/INSTALLED.txt`) and the RewardGuard 1.2
   marker (`excelrewardguard/install-prep-1.2/INSTALLED-1.2.txt`) both
   exist before this packet installs.
2. Read the live `server_menu.yml` and confirm it matches the GuildGate
   packet's prepared file (the locked/open guild pair at slot 12, the
   seals item at 13). If it has drifted, apply the two item blocks above
   by edit instead of swapping the file.
3. Copy `commands_menu.yml` into `plugins/DeluxeMenus/gui_menus/` (new
   file; nothing to back up, delete to roll back).
4. Back up `plugins/DeluxeMenus/config.yml` to
   `config.yml.bak-20261016`, then add the `commands_menu` registration
   shown above under `gui_menus:`.
5. Swap the BedrockMenu jar (keep the 1.5.3 jar as the rollback), then
   `dm reload` or let the install restart cover it. Verify the log
   counts one more menu than before the install (live: 8 becomes 9)
   and `dm list` shows `commands_menu`.
