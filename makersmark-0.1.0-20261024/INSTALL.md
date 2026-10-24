# ExcelMakersMark 0.1.0: install on the live server

NOT DONE. Needs Mike's go in the moment. Test server only so far.
New plugin (nothing is replaced). Paper 26.x, Java 25. Soft-depends only: EssentialsX, Magic (elmakers),
EliteMobs (MagmaGuy), PlaceholderAPI, Geyser/Floodgate. EliteMobs is NOT touched in any way: no EliteMobs
file, config or folder is opened, uploaded or changed. MakersMark only reads item tags to skip EliteMobs items.

Route: Shockbyte panel only (File Manager + Console). No SFTP, no /reload, no plugin managers.

## A. Check before anything (live hash checks first)
A1. Box: `sha256sum ExcelMakersMark-0.1.0.jar` must equal SHA256SUMS
    (7490006ecc46dc2951a38b76ee1da6f4993c25ae702f4517986d70721e3290ad).
A2. Panel File Manager, live `plugins/`: confirm there is NO `ExcelMakersMark*.jar` and no
    `plugins/ExcelMakersMark/` folder. If one exists, stop here and report (do not overwrite it).
A3. Live `plugins/ExcelMenu/menus/server_menu.yml`: download it, sha256 it on the box and write the hash
    down. If tonight's 2 AM batch is in, it should be 1cdc8c29... (Spellbook slot 20, Auto-sort slot 21).
    Check slots 23 and 24 are free before using MENU-ENTRY.yml (step D). If not, pick other free slots.
A4. Console `list`: pick a time nobody is online (or the 2 AM window).

## B. Restart rule (reason, 1-minute warning, then the 10-second countdown)
B1. Console: `say Server restart in 2 minutes to add the Maker's mark (crafted gear shows who made it). Back in about 10 minutes.`
B2. Console: `restartwarn Maker's mark update` (ExcelRestartCountdown: 1-minute warning, then the final
    10-second countdown, then it stops the server).
B3. Panel shows Offline before touching any file. If Shockbyte starts it by itself, press Stop.

## C. Upload (server stopped)
C1. Backups: nothing is replaced by the jar, so no jar backup is needed. If step D edits server_menu.yml,
    first copy it on the server to `server_menu.yml.bak-<date>`. Never overwrite a backup: list the folder
    first; if the name exists use `-2`, then `-3`.
C2. Upload `ExcelMakersMark-0.1.0.jar` to `plugins/`. Do NOT upload a `plugins/ExcelMakersMark/` folder;
    the plugin writes its own config.yml and players.yml on first start.
C3. Download the uploaded jar back to the box and sha256 it: must match A1. Delete the downloaded copy after.

## D. Optional: compass menu buttons (same stop)
Paste MENU-ENTRY.yml under `items:` in `plugins/ExcelMenu/menus/server_menu.yml` (slots 23 and 24, only if
free per A3). ExcelMenu draws the Bedrock form from the same file. Needs PlaceholderAPI.

## E. Start and check
E1. Start. Console must show:
    `ExcelMakersMark 0.1.0 enabled (signing on, stats on, Magic check on, tips via built-in)`
    and no ExcelMakersMark ERROR or WARN lines.
E2. If step D was done: `/excelmenu reload` is not needed after a full restart; open the compass and check
    the two buttons (Maker's mark, Gear stats) show ON.
E3. Bedrock players rejoin after the restart.
E4. In the 2-5 AM CDT window: set the server back to Offline afterwards for the 5 AM job.

## F. Smoke test (your own account)
- Craft an iron sword: gray line "Crafted by <you>", and the first-use tip with [OK] (Java) or a pop-up
  with "OK, got it!" (Bedrock). The tip shows again on each use until OK. `/mark tipreset <you>` afterwards.
- `/mark` shows the switches (Bedrock: pop-up with two switches and Close).
- A Magic wand and an EliteMobs item get no lines.

## Notes
- The first-use tip is MakersMark's own (repeats until OK). tip.use-exceltips is false on purpose:
  ExcelTips 0.1.0 stops a tip after 3 shows. ExcelTips' "makersmark" entry is api-only, so it never fires
  and there is never a second tip.
- Works with tonight's 2 AM batch (WandBars 0.2.1, BedrockMenu 1.5.18, AutoSort 0.1.17, Trash 0.2.0,
  SealAlerts 0.1.0, Tips 0.1.0, Guide 0.3.0): no command clash (/mark, /makersmark), menu slots differ.
