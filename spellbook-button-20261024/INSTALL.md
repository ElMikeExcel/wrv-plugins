# Install: Spellbook button + wand controls choice + wand tip + Auto-sort button

STAGED ONLY. Nothing here is installed. Do it only when Mike says go.
The box SFTP link to the live server is DOWN (Oct 24, 2026), so use the host's web
panel file manager (Upload / Download / Rename) for every step below. Don't use a script
that needs SFTP.

## What goes up (all in files/plugins/, hashes in SHA256SUMS)
| file | goes to | replaces |
|---|---|---|
| ExcelWandBars-0.2.1.jar | plugins/ | plugins/ExcelWandBars-0.2.0.jar |
| ExcelBedrockMenu-1.5.18.jar | plugins/ | plugins/ExcelBedrockMenu-1.5.17.jar |
| autosort-0.1.17+26.3-paper.jar | plugins/ | nothing (new) |
| ExcelMenu/menus/server_menu.yml | plugins/ExcelMenu/menus/server_menu.yml | the live file (must be 2e86584f..., see check 2) |
| ExcelMenu/menus/teleport_confirm_spawn.yml | plugins/ExcelMenu/menus/teleport_confirm_spawn.yml | the live file (must be 1999a762..., see check 2b). Compass Spawn fix, added Oct 24 2:26 PM CDT |

Do not upload: files/plugins/ExcelWandBars-reference/config-0.2.1.yml (reference only;
the live 0.2.0 config works as is, see check 3). Don't create plugins/autosort/active.txt
(everyone starts with Auto-sort off).

## Checks first (stop if any fails)
1. Live jar names are exactly ExcelWandBars-0.2.0.jar and ExcelBedrockMenu-1.5.17.jar,
   and there is no other autosort jar in plugins/.
2. Download the live plugins/ExcelMenu/menus/server_menu.yml with the panel and hash it on
   the box (`sha256sum`). It must be
   2e86584fb6e5dec059cc7f6fda4be10d01491c368afdc6438f027bc86f5d6373. If it differs,
   STOP. Someone changed the menu since this was built, and the new file would wipe
   that change. Re-merge the two new items (slots 20, 21) into the live file instead.
2b. Download the live plugins/ExcelMenu/menus/teleport_confirm_spawn.yml the same way and
   hash it. It must be
   1999a7624d22f6af7728431c79fc8ebbb0a2a85db79e97e619e99d5855dd16d1.
   If it differs, STOP: someone changed the Spawn confirm since this was built. Make the
   same one-line change by hand instead: in the `'yes'` item, replace the line
   `- '[console] execute in minecraft:overworld run minecraft:tp %player_name% -784 80 -336'`
   with `- '[console] spawn %player_name%'` and keep the `- '[close]'` line after it.
2c. Check that `/spawn` itself goes to Marvin's new spawn on live (stand somewhere else,
   type /spawn). The compass now uses exactly that spot, so if /spawn is wrong the
   compass will be wrong too.
3. Keep the live plugins/ExcelWandBars/config.yml. 0.2.1 was booted on the test server
   with the 0.2.0 config file. It used the built-in defaults: Classic default, sneak
   bars Bedrock only, wand tip on, 15 uses. Paste in the `wand-tip:` block or the
   `bars-bedrock-only` line from the reference file only if a setting needs changing.
4. Optional: on the box, `cd files && sha256sum -c ../SHA256SUMS` (all OK).

## Backups (never overwrite one)
For every file replaced, keep a copy named `<name>.bak-20261024`. If that name already
exists, use `.bak-20261024-2`, then `-3`. Check the folder listing first.
* plugins/ExcelWandBars-0.2.0.jar: rename it to `ExcelWandBars-0.2.0.jar.bak-20261024`
  so the server doesn't load it. The `.bak` name stops it loading. Leave it in plugins/
  or move it out.
* plugins/ExcelBedrockMenu-1.5.17.jar: rename it to `ExcelBedrockMenu-1.5.17.jar.bak-20261024`.
* plugins/ExcelMenu/menus/server_menu.yml: copy it to `server_menu.yml.bak-20261024`.
* plugins/ExcelMenu/menus/teleport_confirm_spawn.yml: copy it to
  `teleport_confirm_spawn.yml.bak-20261024` (or `-2`, `-3` if that name exists).
* plugins/ExcelWandBars/config.yml: copy it to `config.yml.bak-20261024` and leave the
  original in place, unchanged.
Also keep the box copies of the downloaded live server_menu.yml and teleport_confirm_spawn.yml.

## Upload
Upload the five files to the paths in the table, then check sizes and names in the panel.
No two ExcelWandBars or ExcelBedrockMenu jars may be loadable at the same time.

## Restart (needed: three jars). Mike's restart rule
1. Say the reason in chat, for example: `say Server restart in 1 minute to add a Spellbook button, wand controls and auto-sort`.
2. Wait 1 minute.
3. Count down 10 seconds (10, 9, ... 1) in chat, then restart.
Best with 0 players online or in the 2 to 5 AM CDT window. Bedrock players rejoin after.
(Only the two menu .yml files could go in alone with `excelmenu reload`. The compass
Spawn fix on its own needs no restart, but it ships with the batch as Mike asked. But its two
buttons run commands that need the new jars, so put everything in together.)

## Expected boot lines
* `[ExcelMenu] ExcelMenu self-check: loaded 18 menus: ...` with teleport_confirm_spawn in the list
  (same 18 as today).
* `[ExcelWandBars] Enabling ExcelWandBars v0.2.1`
* `[ExcelWandBars] Wand style switch ON (default classic, N player choice(s) saved, sneak bars Bedrock only)`
* `[ExcelWandBars] Wand tip ON (first 15 wand uses until OK, 0 player(s) tracked)`
* `[ExcelBedrockMenu] Enabling ExcelBedrockMenu v1.5.18`
* `[AutoSort] Enabling AutoSort v0.1.17`
* No new ERROR or SEVERE lines for these three plugins. The test server showed only the
  same old messages as before, such as Essentials "unsupported server version" and the
  spawnshop lines.

## After install: proofs (2 minutes)
* Console: `wandbars style <a player>` shows `classic` for anyone who never picked.
  Players who picked bars on 0.2.0 show `saved=bars`. They get Sneak bars on Bedrock and
  Classic on Java.
* Java, compass: slot 20 is Spellbook and opens your spell list (works with the compass
  in hand). Slot 21 is Auto-sort, and a click says "Sorting enabled for: Barrel, Chest,
  Ender Chest, Shulker Box". Click again to turn it off.
* Bedrock, compass: Spellbook, "Wand controls: Classic or Sneak bars" and "Auto-sort my
  chests: on/off" are all there. Wand controls opens the two-button form.
* Bedrock with a 2-3 bar wand on Sneak bars: right click opens the bar, a sneak press
  flips to the next bar, and sneaking with the bar closed does nothing. Then pick Classic
  again: right click goes to the next spell.
* Wand tip: on a wand right click the tip shows. Java [OK] stops it. Bedrock gets one
  pop-up per session.
* Check `plugins/autosort/active.txt`: it is empty or missing until someone turns it on.
* Compass Spawn (Java): stand away from spawn, compass > Spawn > Yes. You land on the same
  spot as typing /spawn (Marvin's new spawn), not the old village. Cancel keeps you in
  place. Console shows `CONSOLE issued server command: /spawn <name>`.
* Compass Spawn (Bedrock): Spawn > Yes lands on the same spot (unchanged code path, it
  already used /spawn).

## Records after install
Big Brain entry, Route Sheet row, and website wording (WEBSITE-WORDING-DRAFT.md, draft
PR only until Mike says merge).
