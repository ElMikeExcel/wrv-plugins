# ExcelMenu definition format (format 1)

One file per menu: `plugins/ExcelMenu/menus/<menu-id>.yml`. The file
name (minus `.yml`) is the menu id every `[openguimenu]` target and
`excelmenu open` call uses. Files are extracted from the jar on
first run; after that the files on disk are the live truth and
`excelmenu reload` re-reads them.

A definition is loaded only if it parses AND validates. Failures
are refused loudly: one SEVERE per problem in the console log, the
menu named in the boot self-check's FAILED list, and `/excelmenu
list` shows it. Nothing about a bad definition is ever silent —
that is the design lesson of 2026-10-20, when two unregistered
DeluxeMenus files made the Java cosmetics store a dead button.

## Shape

```yaml
format: 1                     # required, must be 1
title: '&6&lServer Menu'      # required; & colors + %placeholders%
size: 27                      # chest: multiple of 9, 9..54 (default 54)
type: chest                   # chest | dispenser (dispenser = size 9)
bedrock-render: form          # optional: form (default) | chest — chest
                              # serves Bedrock viewers the chest GUI
                              # instead of a form (0.1.3; see Rendering)
command: shop                 # optional: registers /shop for this menu
update-interval: 1            # seconds; required if any item uses update: true
open-requirement:             # optional gate for opening at all
  requirements:
    - id: permission
      type: has-permission
      permission: some.node
      deny:                     # actions run when the gate fails
        - '[message] &cYou do not have permission to do that!'
items:
  example_item:
    material: COMPASS         # a Bukkit Material name
    slot: 10                  # 0-based, must be inside size
    amount: 1                 # optional (default 1)
    data: 1                   # optional legacy data; applied as item damage where the material supports it
    priority: 1               # optional; items sharing a slot need distinct priorities
    update: true              # optional; re-render on the menu's update-interval
    name: '&a&lSpawn'         # optional display name
    lore:                     # optional, ordered
      - '&7Teleport back to the village'
    view-requirement:         # optional; item shows only if ALL pass
      requirements:
        - id: guild_locked
          type: string-equals
          input: '%guildgate_unlocked%'
          output: 'false'
    clicks:                   # any subset of the six kinds
      left:
        - '[openguimenu] teleport_confirm_spawn'
      right:
        - '[player] shop'
      any:                    # runs when the specific kind has no list
        - '[close]'
    bedrock:                  # optional Bedrock hint
      mode: modal             # the item opens a detail form:
      left-label: 'Rent 1 week - $200'   # button 1 runs the left actions
      right-label: 'Own forever - $15,000' # button 2 runs the right actions
      four-label: 'Rent 4 weeks - $700'  # optional button 3 runs the
                              # shift-left actions (the Bedrock mirror
                              # of the Java shift-left click). Label and
                              # shift-left lines are a pair: either one
                              # without the other is a validation error.
```

## Slots and priorities

Multiple items may share a slot (the guild locked/open pair, the
auto-open on/off pair). For each viewer, the item in a slot is the
one whose view requirements all pass with the LOWEST priority
number; an item without a priority sorts last. Sharing a slot
without distinct priorities is a validation error.

## Requirement types

`has-permission` (option `permission`), `has-money` (option
`amount`, via Vault), `has-item` (options `material`, `amount`),
`string-equals`, `string-equals-ignorecase`, `string-contains`
(options `input`, `output` — `input` contains `output`),
`regex-matches` (options `input`, `regex`), and the numeric
comparisons `gt`, `gte`, `eq`, `lte`, `lt` (options `input`,
`output`, both placeholder-resolved then parsed as numbers).
Inputs are placeholder-resolved for the viewer before comparison.
A requirement that cannot be evaluated counts as not met.

## Click actions

Click strings keep the DeluxeMenus bracket vocabulary, so migrated
menus carried theirs byte-for-byte:

- `[console] <command>` — run as the console
- `[player] <command>` — run as the clicking player
- `[commandevent] <command>` — fire PlayerCommandPreprocessEvent, then run if not cancelled
- `[message] <text>` — send the viewer a message (& colors)
- `[json] <json>` — send a raw JSON chat component
- `[openguimenu] <menu-id>` — open another menu (target must exist; validated)
- `[close]` — close the view
- `[refresh]` — re-render the current view
- `[sound] NAME[;volume[;pitch]]` — play a sound to the viewer
- `[broadcastsound] NAME[;volume[;pitch]]` — play a sound to everyone
- `[connect] <server>` — BungeeCord Connect plugin message

Any action may end with `<delay=<ticks>>` to run that many ticks
later. Unknown tags, unknown keys, unknown click kinds and unknown
requirement types are all load errors.

## Rendering parity

The visible-item set for a viewer is computed once and consumed by
both renderers. Java: a chest/dispenser inventory whose slots hold
the items. Bedrock (through Geyser/Floodgate): a Cumulus SimpleForm
with one button per visible item in slot order — a button tap is a
left click, an item with a `bedrock: modal` block opens its detail
form instead (action buttons for the left / right clicks, the
optional four-label button for the shift-left clicks, and a Back
button that returns to the parent menu), and a display-only item
(no actions) re-sends the form when tapped. If forms are
unavailable the chest GUI serves as fallback (Geyser translates
inventories).

A definition may set `bedrock-render: chest` (0.1.3) to skip the
form for that one menu: Bedrock viewers get the same chest GUI
Java gets, with real item icons. Use it for shelves whose point is
seeing the items (the Safe Blocks shop category — cosmetic blocks
a player cannot judge from a name). It is presentation-only: the
definition, stock, prices and click actions are identical on both
platforms, and the menu's session follows the inventory (close
closes it, refresh re-fills it). Every other value is a load
error; menus without the key keep the form.

## Commands

- `/excelmenu list|open <menu> [player]|reload` — admin surface
  (`excelmenu.admin`, default op).
- `/dm ...` — the same three subcommands under the DeluxeMenus
  name, so existing dispatches from ExcelBedrockMenu
  ("dm open server_menu <player>"), the shop villager and
  ExcelMenuJoin keep working the day DeluxeMenus is removed.
- A definition's `command:` registers that command for the menu;
  claiming `excelmenu` or `dm`, or two menus claiming one command,
  is a validation error.

<!-- ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel) -->
