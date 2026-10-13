# ExcelMenuJoin (MenuJoin)

**Created by us (custom add-on).** This is a documentation label; the runtime plugin name remains `MenuJoin`. Recorded installed version: **1.2.0**; this branch's source version: **1.2.1**. The source/build details below describe this branch, not proof of the installed JAR's behavior. See the [plugin inventory](../PLUGIN-INVENTORY.md).

Argus's compass plugin, recovered from 1.1.0 and extended in 1.2.0 by Steve.
This repair keeps the existing join, respawn, menu-toggle, and compass behavior.

## Requirements and build

- Paper 1.21.4 and Java 21
- DeluxeMenus, with a working `/menu` command
- Optional PlaceholderAPI for `%menujoin_status%`
- Build from this directory with `./build.sh`, or from the repository root with
  `mvn -pl MenuJoin -am verify`.
- Output: `target/MenuJoin-1.2.1.jar`

## Behavior

- On join, waits 60 ticks, gives a missing compass, and runs `/menu` if auto-open
  is still enabled when the delayed task runs.
- `/menujoin` toggles auto-open for that player using persistent player data.
- Right-clicking the marked compass runs `/menu`.
- The compass is removed from death drops. A missing compass is restored one
  tick after respawn. An existing inventory or cursor compass prevents another
  one from being given, including a compass retained by `keepInventory`.
- Dropping the marked compass by hand is cancelled. Ordinary compasses and
  unrelated death drops are unaffected.
- `/compass` replaces a missing compass. If the inventory is full, nothing is
  dropped or overwritten: free a slot and run `/compass` again.
- Existing compasses remain recognized by `menujoin:menu_compass`, unchanged
  from 1.1.0. The auto-open key remains `menujoin:autojoin_disabled`.

## Repairs in 1.2.1

- Full inventories no longer cause repeated `/compass` calls to spawn unlimited
  ground items, or report a successful delivery that never reached inventory.
- Toggling auto-open during the join delay now takes effect before the menu opens.
- PlaceholderAPI reports the loaded plugin's version instead of the stale 1.0.0.
- The plugin descriptor declares the 1.21 API generation actually targeted by
  the build instead of claiming a 1.13 target.

Regression tests use mocked Paper APIs to cover delivery, full-inventory recovery,
existing inventory/cursor compasses, death/drop handling, delayed join and respawn,
toggle persistence, and the PlaceholderAPI contract. A live Paper server is still
needed to validate event interactions with the installed plugin stack.
