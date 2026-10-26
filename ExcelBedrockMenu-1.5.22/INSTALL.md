# INSTALL: ExcelBedrockMenu 1.5.22 (Bedrock compass matches the Java compass)

Status: STAGED ONLY. Built and tested on the box. NOT installed. Needs Mike's go. One jar swap, ONE
restart. The optional Java skills_perks.yml fix (part B) needs no restart and can be skipped.

## Part A: the jar

### Preconditions (all must hold, or stop)
1. Player gate: console `list` reads 0 on two fresh readings (refresh the panel first). Recheck right
   before the swap. If anyone is on, hold, or use the house restart rule (reason, 1-minute warning,
   10-second countdown) only if Mike said so.
2. **Update whitelist rule (Mike, Oct 26, 12:02 AM CDT):** before touching anything, write down the
   current whitelist state, then turn the whitelist ON with ONLY Mike's own account on it
   (`whitelist on`, `whitelist list` shows only Mike). Keep `enforce-whitelist` false, so nobody is kicked.
   Never kick anyone. It goes back OFF (the original state) after the boot check passes.
3. `guildgate status`: write it down. It must be the same after the boot.
4. **Drift guard (live jar sha).** Exactly one loadable ExcelBedrockMenu jar in plugins/, and it must be
   `plugins/ExcelBedrockMenu-1.5.21.jar`, size 33,797 B, sha256
   `837424dd1a2afb7730e562b5f3e14f9ce4c9c765799cc94d578997eef80f1404`.
   If the name, size or sha differs, **STOP and skip this part**; 1.5.22 must be rebuilt on whatever is
   live. (`.bak-*` / `.off-*` copies do not load and are fine.)
5. ExcelCosmetics 0.1.2, ExcelGuide (/ask) and ExcelMakersMark 0.1.0 (7490006e) are loaded. If one is
   missing, its button only says "isn't available right now"; nothing breaks.
6. Check the staged jar: `cd staged && sha256sum -c ../SHA256SUMS` (the jar line).

### Steps
1. Backup: rename `plugins/ExcelBedrockMenu-1.5.21.jar` to
   `ExcelBedrockMenu-1.5.21.jar.bak-20261026-parity` and move it out of plugins/ (server-root backups/).
   Never overwrite an existing .bak; add -2 if the name exists.
2. Upload `staged/ExcelBedrockMenu-1.5.22.jar` to plugins/ as `ExcelBedrockMenu-1.5.22.jar.uploading`,
   then rename to `ExcelBedrockMenu-1.5.22.jar`. Verify sha256 (or exact size 35,840 B if the panel shows
   no checksum) BEFORE booting:
   `1d860fbb640d188a831b7da93595ce0a08ebcd83018a63fcc31b6201e960e2ab`.
3. Only one ExcelBedrockMenu jar loads from plugins/ now: 1.5.22.
4. ONE restart (panel Restart, or Shut Down then Start). No config changes, no other plugin touched.

### Boot check
* `Enabling ExcelBedrockMenu v1.5.22` and `ExcelBedrockMenu enabled: /menu uses native Bedrock dialogs.`
* 0 SEVERE, no new WARN/ERROR versus the baseline, plugin count unchanged.
* `guildgate status` unchanged.
* Then whitelist back to its written-down state (OFF), and confirm with `whitelist list` / server.properties.

### Player check (owed, real device)
On a Bedrock phone or console, tap the compass:
* 22 buttons. My Cosmetics (green) right after Excel Shop; Ask the Guide (copper) right after What's New;
  Maker's mark and Gear stats (gray) right after Auto-sort. No mcMMO Stats button.
* Ask the Guide: the text box opens, the keyboard works, Send gets a [Valley Guide] answer in chat.
* Maker's mark: the two-switch form opens and a tap flips the switch.
* Skills & Perks: "Magic is awake: 1 of 5 seals broken..." and a working My mcMMO stats button.

## Part B (optional, Java text fix, no restart): skills_perks.yml
1. Drift guard: live `plugins/ExcelMenu/menus/skills_perks.yml` sha256 must be
   `5df3f0a3fbe5b73ca69047c387cc1e293ce3882a2705d4cb199b6aee591e4c72`. If not, STOP and skip part B.
2. Backup: copy it to `skills_perks.yml.bak-20261026-parity` OUTSIDE menus/ (server-root backups/), so
   ExcelMenu does not try to load the backup.
3. Upload `java-optional/files/plugins/ExcelMenu/menus/skills_perks.yml` over it, check sha256
   `f14455174e565c17b8eaa91b12785eb620e27e4537b60c526fbf6e58a1c1c472`.
4. Console `excelmenu reload`. It must list `skills_perks (6 items)` and no new failed definitions.
5. Check: `/excelmenu open skills_perks <Mike>` on Java shows "Magic (Awake)" and "Seals broken: 1 of 5".
   Part B can run in the same whitelist window as part A, or by itself with no restart.

## Records after install
Big Brain entry (same moment), Route Sheet row, daily log, version map (ExcelBedrockMenu 1.5.22,
1d860fbb; skills_perks.yml f1445517 if part B), website change log + plugin row + Guide knowledge
(the compass now has My Cosmetics, Ask the Guide and Maker's mark on Bedrock), wrv-plugins draft PR
comment. A What's New line is optional ("Bedrock compass: Ask the Guide, My Cosmetics and Maker's mark
buttons, same as Java").

Rollback: see ROLLBACK.md.
