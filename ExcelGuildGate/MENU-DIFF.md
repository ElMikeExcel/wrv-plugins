# server_menu.yml — what the packet copy changes vs the current live file

**Packet file:** `server_menu.yml` in this folder.
**Live file:** `plugins/DeluxeMenus/gui_menus/server_menu.yml` on the server
(DeluxeMenus server menu for Java players).

## The change: slot 12 becomes a locked/open pair, plus one new item

The live file has a single `guild` item at slot 12 (added live on Oct 16:
BELL, '&a&lAdventurers Guild', `[player] ag`). The packet copy replaces
that one block with TWO priority variants of the same slot, mirroring the
existing `autojoin_on` / `autojoin_off` pattern in the same file, and adds
one new item at slot 13:

**1. `guild_locked` (priority 1)** — shown while the guild is sealed:

```yaml
  guild_locked:
    material: BELL
    slot: 12
    priority: 1
    display_name: '&8🔒 Adventurers Guild'
    lore:
      - '&7Sealed by the five challenges'
      - '&7Tap to see the Road to the Guild'
    view_requirement:
      requirements:
        guild_locked:
          type: string equals
          input: '%guildgate_unlocked%'
          output: 'false'
    left_click_commands:
      - '[player] seals'
```

**2. `guild_open` (priority 2)** — shown once the guild is open. This is
tonight's live block, unchanged except for the key name and the view
requirement:

```yaml
  guild_open:
    material: BELL
    slot: 12
    priority: 2
    display_name: '&a&lAdventurers Guild'
    lore:
      - '&7Teleport to the EliteMobs'
      - '&7guild hall'
    view_requirement:
      requirements:
        guild_unlocked:
          type: string equals
          input: '%guildgate_unlocked%'
          output: 'true'
    left_click_commands:
      - '[player] ag'
```

**3. `seals` (new, slot 13)** — the quest page, between guild (12) and
mcmmo (14):

```yaml
  seals:
    material: WRITABLE_BOOK
    slot: 13
    display_name: '&a&lRoad to the Guild'
    lore:
      - '&7The five seals quest'
      - '&7See what is left to break'
    left_click_commands:
      - '[player] seals'
```

Nothing else changes: same title, same size (27), same slots for every
other item (spawn 10, shop 11, mcmmo 14, info 15, autojoin 16). Slot 13
was empty, so nothing moves.

## Requirement: the GuildGate PlaceholderAPI expansion

Both variants key off `%guildgate_unlocked%`, which only exists because
ExcelGuildGate registers a PlaceholderAPI expansion (`guildgate`) when it
loads. **Install this menu only together with ExcelGuildGate** (same
packet, same window). If the expansion is missing, the placeholder never
resolves to 'true' or 'false', neither variant's requirement passes, and
slot 12 renders EMPTY. The pair and the plugin are one unit.

Behavior once installed: no file edits ever again. The menu reads the
live placeholder on every open, so the night the fifth seal breaks, slot
12 flips from the 🔒 sealed variant (tap = /seals quest page) to the plain
guild button (tap = /ag) on its own.

## How to apply at install time (drift check first)

1. Read the live file first and confirm it matches the pre-packet state:
   the Oct 15 prepared menu plus the single `guild` block at slot 12
   (BELL, '&a&lAdventurers Guild', `[player] ag`).
2. If it matches: swap in the packet file wholesale, or apply the same
   edit by hand (delete the `guild` block, insert the `guild_locked` +
   `guild_open` pair in its place, insert `seals` after them). Both
   routes land on identical content.
3. If the live file has drifted (anything else differs): do NOT swap the
   file. Apply only the block replacement by edit, preserving whatever
   the live file has become.
4. After applying: `dm reload` from the console. No restart is needed for
   the menu itself; it rides along with the packet's restart window anyway.

## Note on the reconstruction

The packet copy was rebuilt from `~/workspace/excelshops/install-prep/
server_menu.yml` (the prepared Oct 15 menu) plus the guild block exactly
as it was inserted live on Oct 16 (BELL, slot 12, two-line lore, `[player]
ag`). Step 1's drift check exists precisely so any wording drift in that
reconstruction is caught before a swap; the by-edit route in step 2/3 is
always safe regardless.
