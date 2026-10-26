# ROLLBACK: ExcelBedrockMenu 1.5.22 -> 1.5.21 (and the optional skills_perks.yml)

When: the 1.5.22 boot shows a SEVERE from ExcelBedrockMenu, the compass form fails to open on Bedrock,
a new button misbehaves on a real device, or Mike asks. Rolling back removes My Cosmetics, Ask the Guide
and Maker's mark from the Bedrock compass, brings back the separate mcMMO Stats button and the old
"Magic is sealed" text. Nothing else changes.

## Part A: the jar
1. Player gate as usual (two 0-player reads), whitelist ON with only Mike (enforce-whitelist false,
   never kick), or the house restart rule if Mike says so.
2. Panel Shut Down (never console stop).
3. Rename `plugins/ExcelBedrockMenu-1.5.22.jar` to `ExcelBedrockMenu-1.5.22.jar.off-<date>` and move it
   out of plugins/ (rename, never delete).
4. Put `ExcelBedrockMenu-1.5.21.jar.bak-20261026-parity` back into plugins/ as `ExcelBedrockMenu-1.5.21.jar`.
   Check sha256 `837424dd1a2afb7730e562b5f3e14f9ce4c9c765799cc94d578997eef80f1404` (33,797 B). Box copies:
   batch-next/bedrock-menu-lostfound-trash-20261025/staged/ExcelBedrockMenu-1.5.21.jar and
   base/repro-1.5.21.jar here (byte-identical).
5. Start. Boot line `Enabling ExcelBedrockMenu v1.5.21`, 0 SEVERE, guildgate unchanged. Whitelist back OFF.
6. Nothing else to undo: 1.5.22 writes no files and changes no config.

## Part B: skills_perks.yml (only if it was installed)
1. Copy `skills_perks.yml.bak-20261026-parity` back over `plugins/ExcelMenu/menus/skills_perks.yml`
   (sha256 must be 5df3f0a3...; box copy: java-optional/base/skills_perks.yml.live-5df3f0a3).
2. `excelmenu reload`. No restart.

Records: log the rollback in Big Brain right away, daily log, version map.
