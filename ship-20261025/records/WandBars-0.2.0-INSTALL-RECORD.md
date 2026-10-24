# ExcelWandBars 0.2.0: wand controls opt-in (Classic default, Spell bars opt-in)

Staged Sat Oct 24 2026 (CDT). NOT installed, NOT pushed. Magic (elmakers)
11.2.5 on Paper 26.3-214, ExcelMagicGate 0.1.0 stages 1-5. EliteMobs
(MagmaGuy) is not touched. No spells.yml is in this batch (another job owns
the staged spells.yml files).

## What it does
* Everyone gets Classic wand controls (Magic chest mode): right click = next
  spell, sneak + right click = spell list, Q = back one spell, left click casts.
* A player can opt in to Spell bars (inventory mode + quick_cast, the
  action-bar overlay controls) with `/wandstyle`, and switch back any time.
* Bedrock: `/wandstyle` opens a two-button Geyser form. The /spellinfo book
  form has a "Wand controls" button at the bottom. Java: clickable chat.
* The choice is saved in plugins/ExcelWandBars/styles.yml and re-applied on
  join, on wand hold / bind / re-issue, on respawn, and after every Magic load.
  ExcelMagicGate copies plugins/ExcelMagicGate/stages/stageN/* over
  plugins/Magic/ and runs `magic reload` at every seal; that fires Magic's
  LoadEvent, and the plugin re-applies each online player's choice 2 s later.
* Sneak bar flip and the "Bar 2/3: ..." line are for Spell bars players only.

## Which files go up depends on what is live (check FIRST)
Hash the live plugins/Magic/wands.yml on the panel:

| live Magic/wands.yml sha256 | means | upload |
|---|---|---|
| 1a9accf26f66fe893e08ec5414bce443042d26c3ea6ccea9e60f4bf877f83de0 | noab chest set (classic) is live | jar only (part A). Do NOT upload part B. |
| e97406d0198f2f65515a1ea112bbc9b7880f394b33f2e1f0e1e62e903ac32e9a | action-bar overlay (bars) is live | jar (part A) AND classic wands set (part B) |
| anything else | unknown | STOP, do not install, ask |

Why part B is needed with the overlay: the overlay rank wands pin spells to
slots (`mend@0`, `none`, `boon@8`, ...). In chest mode Magic 11.2.5
`Wand.cycleSpells` throws a NullPointerException on any empty slot, so
classic right click / Q stop working once the active spell is past the first
gap (seen on the rig). The noab files list spells without gaps.

Expected live hashes for the stage files before part B (overlay):

| file | overlay (expected live) | classic (part B) |
|---|---|---|
| ExcelMagicGate/stages/stage1/wands.yml | e97406d0198f2f65515a1ea112bbc9b7880f394b33f2e1f0e1e62e903ac32e9a | 1a9accf26f66fe893e08ec5414bce443042d26c3ea6ccea9e60f4bf877f83de0 |
| ExcelMagicGate/stages/stage2/wands.yml | 5346d442c93b98c7942eeb1f7820f08cae55ccfb53d593ef5d4a9dc3b1b21286 | 3dc1bf99ce9781614dfb83e6c71200e0a3b195e44fb1d872b254f9b79b54206c |
| ExcelMagicGate/stages/stage3/wands.yml | 5bf3d4bb69ecbd30df221ad105d81cb56e080d25a402d83c31ec6ffd40484bc5 | a8afb0d8d2a5a0450851f5197067db3eabccd050930912b710b0c37c3ec157e2 |
| ExcelMagicGate/stages/stage4/wands.yml | c14f2483f1c6aea4c14bb9d37468fadb503d97ebbc2e904a5ff2588b420f8477 | e5165a7e08e53099fb108d7676fa64f85f4a37ac716ab33de2bf310831176fe1 |
| ExcelMagicGate/stages/stage5/wands.yml | 8bc82e75b0928299b514d654a3fd46123170ef607702e5f986b3e8ebb3ea29f9 | fb5a8896a4fa49ef22a0cd519887b1100527f928066358e561614e5ff6b9d33e |
| Magic/wands.yml | e97406d0198f2f65515a1ea112bbc9b7880f394b33f2e1f0e1e62e903ac32e9a | 1a9accf26f66fe893e08ec5414bce443042d26c3ea6ccea9e60f4bf877f83de0 |

If ANY stage file has a hash in neither column, STOP (someone changed it
since; it would be overwritten). Part B files are byte-identical to the noab
ship (SHIP-20261024-noab.zip). Magic/wands.yml and all of stages 1-5 must go
up together, because the next seal copies the stage file over Magic/.
stage0/wands.yml has no rank wands and is NOT changed. config.yml files are
NOT changed (overlay `vanilla_icons_enabled: true` is harmless in chest mode).
No spells.yml.

## Restart rule
Only with 0 players online if possible, in the 2-5 AM CDT window, otherwise
with notice: say the reason in chat ("Server restart in 1 minute to add a wand
controls switch"), wait 1 minute, then a 10-second countdown (10, 9, ... 1),
then restart. Bedrock players rejoin after any Magic reload.

## Install steps
1. Hash checks above. Decide A only, or A + B.
2. Backups, never overwrite one: for every file replaced, copy it to
   `<file>.bak-20261024`; if that name exists use `.bak-20261024-2`, then
   `-3`, and so on. Check with a directory listing first. Files:
   * plugins/ExcelWandBars-0.1.0.jar -> move out of plugins/ as
     ExcelWandBars-0.1.0.jar.bak-20261024 (a .bak name so it is not loaded).
   * plugins/ExcelWandBars/config.yml -> config.yml.bak-20261024 (kept in place too)
   * (B) plugins/Magic/wands.yml and the five stages/stageN/wands.yml.
3. Part A: upload ExcelWandBars-0.2.0.jar to plugins/. Keep the live
   plugins/ExcelWandBars/config.yml as it is: without a `wand-style:` section
   0.2.0 uses its built-in defaults (switch on, default classic, rank wands =
   inherit base_wand, masons excluded). Rig-tested with the 0.1.0 config file.
   The commented `wand-style:` block in src/ExcelWandBars/config.yml can be
   pasted in later if a setting needs changing (`/wandbars reload`).
4. Part B (overlay live only): upload the six wands.yml files from
   staged-classic-wands/plugins/ to the same paths. Re-hash each on the panel.
5. Restart per the rule above (a restart also loads part B; no separate
   magic load needed).

## Boot checks (console / latest.log)
* `[ExcelWandBars] Enabling ExcelWandBars v0.2.0`
* `[ExcelWandBars] Sneak bar flip ON (debounce 250 ms), spell book ON, ...`
* `[ExcelWandBars] Wand style switch ON (default classic, 0 player choice(s) saved)`
* no `Exception` mentioning ExcelWandBars; `/wandbars` shows
  `ExcelWandBars 0.2.0 ... wand style ON default classic`
* in game: hold a wand, right click = next spell, sneak + right click = spell
  list, Q = back one spell. `/wandbars style <player>` shows
  `classic (default) ... mode=chest ... matches=true`.
* `/wandstyle bars`, right click opens the bar, sneak flips (2+ bar wands),
  `/wandstyle classic` back. Bedrock: `/wandstyle` shows the form.
* (B) after the next seal / `magicgate apply N`, live Magic/wands.yml hash =
  classic stageN column.

## Rollback
* Quick (one player): `/wandbars style <player> clear` (back to default) or
  `/wandbars style <player> unset` (removes the plugin's keys from the held
  wand so its template rules again).
* Plugin: move ExcelWandBars-0.2.0.jar out, put ExcelWandBars-0.1.0.jar back
  from its .bak, restart (rule above).
  Wands that got per-wand overrides keep them until re-issued or
  `wandp <player> configure`; re-issuing a wand clears them.
* Part B: copy each wands.yml .bak-20261024 back (Magic + stages 1-5), restart
  or `magic load`, Bedrock players rejoin.

## Not done here
* ExcelBedrockMenu compass-menu button: note only, see
  BEDROCKMENU-COMPASS-BUTTON-NOTE.md. ExcelMenuJoin not touched.

## Checks (both passed on jar 3bac9054323330dfbcb5c84ed4488121f463a03d41eda5bbb336fbaa872b70a1)
* Check 1a, unit: 114 passed, 0 failed (tests/unit-tests.log), including a
  real Cumulus SimpleForm build against floodgate.jar.
* Check 1b, rig (Paper 26.3-214, Magic 11.2.5, ExcelMagicGate 0.1.0, the
  0.1.0 config.yml kept), classic wands set live:
  main 39/0, full restart 3/0, chest base + student 2-bar wand 20/0,
  stage tour 2 -> 3 -> 4 -> 5 22/0 (live Magic/wands.yml hash equals the
  staged classic stage file after each apply, picked bars and default
  classic both survive each apply, builder wand never touched).
  Logs: tests/rig/run-final-classic-base.log, run-final-stages-rerun.log.
* Overlay wands set live with the same jar: 37/2, the 2 failures are Q in
  classic hitting the Magic cycleSpells NPE on slot gaps. This is why part B
  is required with the overlay (tests/rig/run-final-overlay-base-EXPECTED-Q-FAIL.log).
* Check 2: REVIEW-0.2.0.md (5 fixes made, then everything re-run).
* Not tested: a real Bedrock client through Geyser/Floodgate (form send and
  the press-and-hold / LT mapping), a real controller, the live server.
