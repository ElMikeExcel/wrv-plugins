# ExcelBedrockMenu 1.5.23 changelog (Oct 26, 2026)

Draft, staged only. Nothing here is on the server until Mike says go.

Base: ExcelBedrockMenu 1.5.22 (draft PR #77, jar sha256 1d860fbb640d188a831b7da93595ce0a08ebcd83018a63fcc31b6201e960e2ab).
1.5.22 is not live yet. If 1.5.22 is installed first, 1.5.23 replaces it. If not, 1.5.23 replaces the live
1.5.21 (837424dd...) and brings all the 1.5.22 changes with it.
The whole change is in `ExcelBedrockMenu-1.5.22-to-1.5.23.diff` (one source file, plugin.yml, build.sh).

Why: Mike's parity rule, Oct 26, 2026, 12:10 AM CDT: "Rule is to always do onto one as you would do the
other. All our plugins should work both bedrock and java." The Java Server Commands pages get /cosmetics,
/lostfound, /trash, /mark and /ask in the same batch (ExcelMenu commands_menu_page2.yml, new row
"Your Items & Help" in slots 9 to 13; /cosmetics was already in Shops & Trading there). This release
puts the same five commands in the Bedrock Server Commands list.

## Added (Bedrock: compass > Server Commands, or /servercommands)
* **/cosmetics** in group 4, Shops & Trading, right after /shop (2nd row of 6). Same place as the Java page.
  Words: "See your colors, effects and sets, and equip them".
* **New group 8, "Your Items & Help"** (last button of the group list). Same name, order and words as the
  Java page 2 row:
  1. /lostfound: "Get back your own stuff lost to lava, fire, the void or a despawn in the last 7 days"
  2. /trash: "Throw items away. /trash undo works for 5 minutes"
  3. /mark: "Choose if things you craft say "Crafted by" with your name"
  4. /ask: "Ask the Guide anything about the server"
* `/servercommands 8` and `/servercommands your` open the new group.
* The seven older groups keep their numbers (`/servercommands 1` to `7` open the same groups as before).
* The chat panel version of /servercommands (Java players typing it) lists the new rows too.

## Changed
* Tapping one of these five rows uses the same guarded action as the matching compass button (`runAction`,
  new set `CATALOG_VIA_ACTION`), not a bare command:
  * /mark opens ExcelMakersMark's own two-switch form (Maker's mark, Gear stats, Close). No typing.
  * /ask opens the "Ask the Guide" text box (same as the compass button). A bare /ask would only print help.
  * /lostfound, /trash, /cosmetics: a missing plugin gives "... isn't available right now. Ask an adult to
    check the server."; no permission gives "You can't use ... right now."
  * Every other row in the list runs exactly as before.
* Compass buttons: unchanged from 1.5.22. Config: unchanged (no new keys).

## What players notice
* **Bedrock:** compass > Server Commands now has an 8th group, "Your Items & Help", with 4 buttons
  (/lostfound, /trash, /mark, /ask) that work without typing. Shops & Trading has /cosmetics.
* **Java:** nothing from this plugin. Java players get the matching row from the ExcelMenu page 2 change.

## Stats (ExcelMenuStats)
Bedrock taps report as form "Server Commands", button "/lostfound" and so on. Add these lines to
`plugins/ExcelMenuStats/bedrock-map.txt` (then `menustats reload`) so they count on the same rows as the Java buttons:
```
Server Commands > Your Items & Help: commands_menu_page2 > hdr_yi
Server Commands > /cosmetics: commands_menu_page2 > st_cosmetics
Server Commands > /lostfound: commands_menu_page2 > yi_lostfound
Server Commands > /trash: commands_menu_page2 > yi_trash
Server Commands > /mark: commands_menu_page2 > yi_mark
Server Commands > /ask: commands_menu_page2 > yi_ask
```

## Tests
* `tests/MenuLogicTest-1.5.23.out`: 244 checks, ALL PASS (1.5.22 checks kept; new: 8 groups, group 8 name,
  `/servercommands 8|your`, 4 rows, /cosmetics after /shop, older group numbers unchanged, catalogue total 50,
  the five rows go through runAction, plain ASCII, no em dashes). Two test fixes: a duplicate `ascii`
  variable renamed to `ascii13`, and the plugin.yml version pin moved to 1.5.23.
* Box rig (Paper 26.3 + Geyser + Floodgate, fake Bedrock player through bedrock-protocol 1.26.40):
  `tests/parity-bed-a.out` 17 passed, 0 failed (Wand controls on the compass, 8 groups, group 8 buttons in
  order, /lostfound answers, /trash opens the bin, /mark opens the Maker's mark form, /ask opens the text box,
  /cosmetics in Shops runs, all six stats rows mapped). `tests/parity-bed-b.out` 4 passed, 0 failed.
* Jar built twice from this source, identical: sha256 3d6ad283b9138e3efc4172c21298f303005fc54fc71e0fbbeb973dc5a31b979c (36,269 B).

## Install and rollback
Restart needed (jar swap). Panel file manager only. Back up the old jar as
`ExcelBedrockMenu-<old version>.jar.bak-20261026`, upload the new one as `.uploading`, rename to
`ExcelBedrockMenu-1.5.23.jar`, check the sha256, then restart (whitelist or 0 players, or the full
restart rule). Rollback: put the .bak jar back under its old name and restart.

## Still to check in game
* A real phone: compass > Server Commands > Your Items & Help > /mark shows the two switches.
* A real phone: /ask in group 8 opens the text box and the Guide answers.
* The ExcelMenuStats panel shows the Bedrock taps on the Java page 2 rows after the map lines are added.

## Known, not changed here
* The bundled `commands_menu.yml` copy in this folder is the old mirror and does not have the new Java row.
* The Bedrock list still has no Fixes & Help group, no /buildmode and no emotes (the Java pages have them).
* The Java-typed /servercommands chat panel still uses em dashes (older text, not touched here).
