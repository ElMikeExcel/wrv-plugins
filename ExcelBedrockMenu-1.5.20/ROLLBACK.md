# ROLLBACK: ExcelBedrockMenu 1.5.20 -> 1.5.19-whatsnew

When: the 1.5.20 boot shows a SEVERE from ExcelBedrockMenu, the compass form fails to open on Bedrock,
or Mike asks.

1. Player gate as usual (two 0-player reads), or the house restart rule if Mike says so.
2. Panel Shut Down (never console stop).
3. Rename `plugins/ExcelBedrockMenu-1.5.20.jar` to `ExcelBedrockMenu-1.5.20.jar.off-<date>` and move it
   out of plugins/ (rename, never delete).
4. Put `ExcelBedrockMenu-1.5.19-whatsnew.jar.bak-20261025-lftrash` back into plugins/ as
   `ExcelBedrockMenu-1.5.19-whatsnew.jar`. Check sha256
   `178b03b7c3d202f08cfecf2471a1a9aa9b4cbd6f4397c048988b22be27cc2175`. A box copy is also at
   batch-next/whatsnew/ExcelBedrockMenu-1.5.19-whatsnew.jar (and base/repro-1.5.19-whatsnew.jar here,
   byte-identical).
5. Start. Boot line `Enabling ExcelBedrockMenu v1.5.19-whatsnew`, 0 SEVERE, guildgate unchanged.
6. Nothing else to undo: 1.5.20 writes no files and changes no config. Records: log the rollback.
