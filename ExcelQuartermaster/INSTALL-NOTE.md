# INSTALL NOTE — ExcelQuartermaster 0.1.0 (for the later gated install)

Status: NOT installed. This note is the pre-written plan for the
empty-server install window, per the standing install rule (reversible,
rollback written in advance, nobody online).

## What ships

- `ExcelQuartermaster-0.1.0.jar` — 16,221 bytes
  SHA-256 b27f8648f3db0116ab1207c4105681fe9cc06d2c9cd143cdacade231471e0d01
- No other files change. The plugin creates
  `plugins/ExcelQuartermaster/` (config.yml + player-tales.txt) on first
  enable. No dependencies; Shopkeepers does not need to be touched.

## Pre-flight checks (before the window)

1. Confirm the live NPC is still the anchor target: a villager named
   "Old Quartermaster" at about (-783, 81, -299) in world "world". If he
   was ever moved or renamed, update config anchor/name FIRST in the jar's
   bundled config or plan a config edit right after first enable.
2. Re-hash the jar in hand and match the SHA-256 above.
3. Gate: `list` shows 0 players (twice, per practice).

## Install steps

1. Upload `ExcelQuartermaster-0.1.0.jar` to `plugins/`.
2. Panel Shut Down, then Start (console `stop` auto-restarts on this
   host; use the panel buttons, as always).
3. Read the boot log:
   - "[ExcelQuartermaster] Enabling ExcelQuartermaster v0.1.0"
   - Ready line: "ExcelQuartermaster enabled: 'Old Quartermaster' will
     tell his tales (11 tales, anchor on: world -783,81,-299 r10,
     cooldown 8s, 0 players with saved progress)."
   - No SEVERE, no ExcelQuartermaster stack traces.
4. Hash-verify the installed jar matches the build (panel file view or a
   re-download), same as every install.

## Player checks (the part scratch could not prove)

Do these on a real client before calling it done; Java first, then a
Bedrock phone check, because the family mostly plays on Bedrock:

1. Right-click (Bedrock: tap) the Old Quartermaster, NOT sneaking:
   - The empty trade window does NOT open.
   - Chat shows the gold header + tale 1 ("Sit a moment...").
2. Click again immediately: nothing happens (cooldown silence), and the
   trade window still does not open.
3. Wait 8+ seconds, click again: tale 2 arrives. Rotation is advancing.
4. Sneak + click: the Shopkeepers editor opens as before (owner/admin),
   untouched by the plugin.
5. A second player clicks: they get tale 1 — rotation is per player.
6. Restart the server once (empty window) and click again: the first
   player continues where they left off (player-tales.txt persisted).

If check 1 shows the trade window opening anyway, the cancellation is
losing the race with Shopkeepers: pull the jar (rollback below), keep the
NPC as-is (he still works exactly as before this plugin), and the fix is
a listener-priority/ordering revisit in a 0.1.1, not a live tweak.

## Rollback

1. Panel Shut Down.
2. Delete `plugins/ExcelQuartermaster-0.1.0.jar`.
3. Optionally delete the `plugins/ExcelQuartermaster/` folder (config +
   player-tales.txt). Nothing else was modified, so removal is the whole
   rollback; the Old Quartermaster NPC is untouched either way.
4. Start; confirm a clean boot log with no ExcelQuartermaster lines.
