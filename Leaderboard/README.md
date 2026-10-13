# ExcelLeaderboard

Runtime identity: `ExcelLeaderboard`; legacy provider alias: `Leaderboard`.
The source module directory, commands, permissions, main class, authors and licenses
are unchanged. See [rename/migration scope](../EXCEL-PLUGIN-RENAMES.md).
**Source builds are not approved rename-only replacements for the installed JARs.**

Version: **1.0.0**. `/leaderboard` runs the native Essentials `baltop` and mcMMO `mctop` commands as the player, retaining their output and permission checks. If either command is unavailable, the player receives a short fallback message. Console use is rejected.

## Build and test

From the repository root, with JDK 21:

```sh
./mvnw -pl Leaderboard -am verify
```

Output: `Leaderboard/target/ExcelLeaderboard-1.0.0.jar`. No external plugin APIs are needed at compile time beyond Paper.

## Source recovery

The readable source and manifest were restored from historical [`leaderboard/` at `113611b4c753d43617d9daad0c35027db0d6c13b`](https://github.com/ElMikeExcel/wrv-plugins/tree/113611b4c753d43617d9daad0c35027db0d6c13b/leaderboard). Inspection confirms the same command flow and messages as the reference decompile. There are no production behavior changes in this restoration.

`decompiled/` remains untouched as recovery evidence and is excluded from compilation. `RECOVERY.md` preserves the previous README, whose statement that original source was missing was superseded by the historical Git recovery. Neither source comparison nor unit tests establish byte-identical equality with a deployed JAR.
