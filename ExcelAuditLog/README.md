# ExcelAuditLog 0.1.0

Read-only audit log for what CoreProtect does not cover: money (Vault balance deltas polled every 10 s, tagged with the player's last command within 15 s, plus /pay /eco /tpa /spawn /warp /home commands), teleports (PlayerTeleportEvent cause + from/to, skips moves under 2 blocks), ender chest open/close with item diff, XP level changes, and enchanting-table enchants.

Daily CSV: `plugins/ExcelAuditLog/audit-YYYY-MM-DD.csv` (America/Chicago day), `time,player,uuid,platform,event,details`. Writes are queued and flushed async every 5 s.

Command: `/audit <player> [hours 1-720] [type]` (op / excelauditlog.admin), last 20 rows. Types: money, teleport, enderchest, xp, enchant, command.

Boot line: `[AuditLog] READY version=0.1.0 economy=<provider> dir=plugins/ExcelAuditLog`

Build: `./local-build.sh` (JDK 25, --release 21, Paper API + Vault on classpath, JUnit 5 tests).
sha256 0.1.0: 00fafd02404d6a6e2c6d9a74c96322e40fcc9534f834fb5526d2b704155e27b6

## 0.2.0 — one /audit for every audit tool

`/audit <player> [hours 1-720] [type|all] [page N]` merges these sources, newest first, 10 per page. `/audit types` lists the types and which sources are present.

| type | source (read-only) |
|---|---|
| money, teleport, enderchest, xp, enchant, command | ExcelAuditLog `audit-YYYY-MM-DD.csv` |
| shop | ExcelShops `purchases.csv` |
| menu | ExcelMenuStats `clicks.csv` |
| trash | ExcelTrash `trash.log` (TRASH / UNDO) |
| death, burn | ExcelDeathRecord `deaths/` snapshots and `burned/` Burn Ledger |
| seal | ExcelSealLog console lines in `logs/latest.log` and `logs/*.log.gz` (times read as UTC; set `log-zone` in config to change) |
| block, container, item, chat, command | CoreProtect official API (soft-depend; skipped when CoreProtect is missing or disabled) |

Files are read off the main thread. Missing sources are skipped. Boot line lists what was found:
`[AuditLog] READY version=0.2.0 economy=... dir=plugins/ExcelAuditLog sources=auditlog,shop,...,coreprotect(api 13)`.
