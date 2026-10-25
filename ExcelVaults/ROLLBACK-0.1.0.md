# ROLLBACK: ExcelVaults 0.1.0

Fast, and safe at any time (needs a restart).
1. `list` -> 0 players if possible.
2. Rename `plugins/ExcelVaults-0.1.0.jar` to `plugins/ExcelVaults-0.1.0.jar.bak-<date>` (do not delete).
3. Panel Shut Down, then Start.
4. Boot: no `[ExcelVaults]` lines.

What stays the same after rollback:
- Vaults go back to vanilla: each vault's own "already rewarded" list is the vanilla one. Players who were
  reopened (by time or by paying) and have not opened again stay able to open it once more. Players
  inside a cooldown stay locked like vanilla.
- `plugins/ExcelVaults/` (data.yml, purchases.csv, config.yml) is harmless. Keep it: reinstalling picks the timers up again.
- Money spent on reopens is not refunded automatically. purchases.csv has every purchase if Mike wants refunds (`eco give`).
