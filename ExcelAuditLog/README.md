# ExcelAuditLog 0.1.0

Read-only audit log for what CoreProtect does not cover: money (Vault balance deltas polled every 10 s, tagged with the player's last command within 15 s, plus /pay /eco /tpa /spawn /warp /home commands), teleports (PlayerTeleportEvent cause + from/to, skips moves under 2 blocks), ender chest open/close with item diff, XP level changes, and enchanting-table enchants.

Daily CSV: `plugins/ExcelAuditLog/audit-YYYY-MM-DD.csv` (America/Chicago day), `time,player,uuid,platform,event,details`. Writes are queued and flushed async every 5 s.

Command: `/audit <player> [hours 1-720] [type]` (op / excelauditlog.admin), last 20 rows. Types: money, teleport, enderchest, xp, enchant, command.

Boot line: `[AuditLog] READY version=0.1.0 economy=<provider> dir=plugins/ExcelAuditLog`

Build: `./local-build.sh` (JDK 25, --release 21, Paper API + Vault on classpath, JUnit 5 tests).
sha256 0.1.0: 00fafd02404d6a6e2c6d9a74c96322e40fcc9534f834fb5526d2b704155e27b6
