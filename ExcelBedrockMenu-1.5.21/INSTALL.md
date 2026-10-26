# INSTALL: ExcelBedrockMenu 1.5.21 (Lost & Found + Trash Bin + group text colors on the Bedrock compass)

Status: STAGED ONLY. Built and tested on the box. Not installed. Needs Mike's go (or the standing
empty-server rule once he adds it to the install routine). One jar swap, ONE restart.

## Preconditions (all must hold, or skip this part)
1. Player gate: console `list` reads 0 on two fresh readings (refresh the panel first). Recheck right
   before the swap. If anyone is on, hold, or use the house restart rule (reason, 1-minute warning,
   10-second countdown) only if Mike said so.
2. `guildgate status`: write it down. It must be the same after the boot.
3. **Drift guard (live jar sha).** Exactly one loadable ExcelBedrockMenu jar in plugins/, and it must be
   `plugins/ExcelBedrockMenu-1.5.19-whatsnew.jar`, size 32781 B, sha256
   `178b03b7c3d202f08cfecf2471a1a9aa9b4cbd6f4397c048988b22be27cc2175`.
   If the name, size or sha differs (for example the EliteMobs follow-up 1.5.19 cb8bf9ec went in, or
   any other EBM build), **STOP and skip this part**. Rebuild 1.5.21 on top of whatever is live.
   (`.off-*` / `.bak-*` copies like ExcelBedrockMenu-1.5.18.jar.off-20261025-asap do not load and are fine.)
4. ExcelDeathRecord 0.3.0 (6c7f3f2c...) and ExcelTrash 0.2.0 (a7c00ba7...) are loaded (they are live
   since Oct 24). If one is missing, its button only says "isn't available right now", nothing breaks.
5. Check the staged jar: `cd staged && sha256sum -c ../SHA256SUMS`.

## Steps
1. Backup: rename `plugins/ExcelBedrockMenu-1.5.19-whatsnew.jar` to
   `ExcelBedrockMenu-1.5.19-whatsnew.jar.bak-20261025-lftrash` and move it out of plugins/ (server-root
   backups/). Never overwrite an existing .bak; add -2 if the name exists.
2. Upload `staged/ExcelBedrockMenu-1.5.21.jar` to plugins/ as `ExcelBedrockMenu-1.5.21.jar.uploading`,
   then rename to `ExcelBedrockMenu-1.5.21.jar`. Verify the sha256 (or exact size 33,797 B if the panel
   shows no checksum) BEFORE booting:
   `837424dd1a2afb7730e562b5f3e14f9ce4c9c765799cc94d578997eef80f1404`.
3. Only one ExcelBedrockMenu jar loads from plugins/ now: 1.5.21.
3b. 1.5.20 was never installed; it is superseded by 1.5.21 (staged/superseded/, do not upload).
4. Start (panel). No config changes, no menu file changes, no other plugin touched.

## Boot check
* `Enabling ExcelBedrockMenu v1.5.21` and `ExcelBedrockMenu enabled: /menu uses native Bedrock dialogs.`
* 0 SEVERE, no new WARN/ERROR versus the baseline, plugin count unchanged.
* `guildgate status` unchanged.
* Bedrock players rejoin once (normal after a restart).

## Player check (owed, real device)
* On a Bedrock phone or console: tap the compass. The list shows Lost & Found then Trash Bin right
  after Leaderboard. Lost & Found opens the price form. Trash Bin opens the bin.
* Button text is colored by group: Travel dark blue, Me purple, Shops green, Help copper/orange,
  Settings dark gray. Check Excel Shop (green) and the Help buttons (copper) are readable on the
  gray button. If not, it is cosmetic only; rollback is optional.

## Records after install
Big Brain entry, Route Sheet row, daily log, version map (ExcelBedrockMenu 1.5.21, 837424dd), website
change log + plugin row (ExcelBedrockMenu 1.5.21) + Guide knowledge, wrv-plugins draft PR comment.
A What's New line is optional ("Bedrock compass: new Lost & Found and Trash Bin buttons").

Rollback: see ROLLBACK.md.
