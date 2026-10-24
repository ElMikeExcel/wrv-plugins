# ExcelMakersMark 0.1.0: rollback

1. Restart rule: Console `say <reason>`, then `restartwarn <reason>` (1-minute warning, then the 10-second
   countdown, stops the server). Panel must show Offline. No /reload.
2. In live `plugins/`: rename `ExcelMakersMark-0.1.0.jar` to `ExcelMakersMark-0.1.0.jar.off-<date>`
   (a name that does not load, so it can come back). Never overwrite an existing name; add -2, -3.
   Keep `plugins/ExcelMakersMark/` (players.yml with each player's on/off and tip choice).
3. If the compass buttons were added: restore `server_menu.yml.bak-<date>` (rename the current file to
   `server_menu.yml.off-<date>` first; never delete a backup).
4. Start. Console: no ExcelMakersMark lines. Bedrock players rejoin.

Effect (rehearsed on the test server, twice): gear keeps its "Crafted by", "Kills", "Slain" and "Hits
blocked" lines as plain lore text; nothing is lost or broken. /mark is gone and new crafts are not signed.
Putting the jar back restores everything: the hidden tags were kept, so marks, counters and trophies
continue where they stopped.
