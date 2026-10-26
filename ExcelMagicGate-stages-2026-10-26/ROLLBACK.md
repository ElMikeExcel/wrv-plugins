# ROLLBACK: all magic by Seal 4 packet

## Before any seal past 1 has applied (the normal case)
Part A (stage files): for each uploaded file, rename `<name>` to `<name>.rejected-20261026`, copy `<name>.bak-20261026` back to `<name>`, read back the base sha (INSTALL.md "Expected live"). No reload, no restart: the files are only read when a seal applies. `magicgate status` stays "Applied Magic stage: 1 of 5".
Part B (configs): same file restore, then it takes effect on the next restart (same restart rules: whitelist rule, 1-minute + 10-second countdown if anyone is online, panel Shut Down). Until that restart the new text stays loaded, which is harmless before Seal 4.

## If a seal already applied with the new files
- Restoring the stage files does not change what is live. ExcelMagicGate stages only move up, so it will not re-apply the same stage by itself.
- ExcelMagicGate keeps the previous live Magic set in `plugins/ExcelMagicGate/backups/<date-time>-from-stageN/` on every apply (rig: `20261026-143152-from-stage1`). To go back to the old list for the CURRENT seal: restore the old stageN files from .bak first, then copy those six files into plugins/Magic/ (with .bak copies of the current ones), and run `magic load`. No restart.
- Grants: a rollback does not remove LuckPerms nodes that were already set. To pull one: `lp group default permission unset magic.cast.<spell>`. Only needed for spells that must close again; casting also needs the spell on the wand, so the file restore alone already takes them off wands.
- Rerun the per-wand override step afterwards (Big Brain rule).
