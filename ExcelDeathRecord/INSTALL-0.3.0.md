# ExcelDeathRecord 0.3.0 (Lost & Found): install

Jar: `ExcelDeathRecord-0.3.0.jar`
SHA-256: `6c7f3f2cdd17cdd9f27223f6f11ad88d9d0fe5f8566675ca3c6fccb4e90aaa50`
Built with `./build.sh` (JDK 25, `--release 21`, Paper API 26.2, VaultAPI 1.7).
Status: tested on the box test server only (Paper 26.3, Geyser + Floodgate,
EssentialsX economy through Vault, EliteMobs 10.9.10 loaded). Not installed live.

Needs: Vault and an economy plugin that registers with Vault (the money Jobs
pays into). Optional: Geyser/Floodgate (Bedrock forms), ExcelTips 0.1.0
(takes over the first-use tip).

## Install (server empty, normal restart rule)
1. Backups first (never overwrite an existing .bak; if the name is taken, add -2):
   - In `plugins/`, rename the current jar, for example
     `ExcelDeathRecord-0.2.0.jar` -> `ExcelDeathRecord-0.2.0.jar.bak-20261024`.
     Only ONE ExcelDeathRecord jar may be loadable.
   - Copy the whole data folder: `plugins/ExcelDeathRecord/` ->
     `plugins/ExcelDeathRecord.bak-20261024-pre030/` (deaths/, burned/,
     config.yml, restores.csv). This is the rollback copy.
2. Upload `ExcelDeathRecord-0.3.0.jar` to `plugins/` and check the SHA-256 above.
3. Restart. The console must show:
   - `ExcelDeathRecord 0.3.0 enabled — death ledger + Burn Ledger watching; restore ready (menu + typed), bedrock-forms=ready, display-zone=America/Chicago, log=restores.csv`
   - `ExcelDeathRecord [Lost & Found]: ON, price $16,000, first one free under $10,000, window 7 days, cooldown 24h, economy=<your economy>, despawn-log-types=11`
   - First boot only: `ExcelDeathRecord: added the 0.3.0 Lost & Found settings to config.yml (old file kept as config.yml.bak-pre-0.3.0).`
   - With ExcelTips installed: `ExcelDeathRecord [Lost & Found]: first-use tip handed to ExcelTips (registered).`
   If it says `economy=none`, Vault has no economy: Lost & Found shows the
   list but refuses to charge ("The money system is offline right now").
4. Check as an op: `/deathrecord lostfound <your name>` prints the status line.
5. Quick live check (optional): a player types `/lostfound` (Java: chest menu,
   Bedrock: form). Nothing else changes for players.

## Settings (plugins/ExcelDeathRecord/config.yml, then restart)
`lost-and-found:` enabled, price (16000), free-first-recovery (true),
free-threshold (10000), free-only-if-no-earlier-recovery (true),
window-days (7), cooldown-hours (24), tell-owner (true), tell-owner-text.
`despawn-log-types:` which plain item types are logged when they despawn.

## Ops
- `/deathrecord lostfound <player>`: status.
- `/deathrecord lostfound <player> reset cooldown|free|tip|all`.
  With ExcelTips installed, also run `/tips reset <player> lostfound` to show the tip again.
- `/deathrecord burned <player>` and `/deathrecord restore burned <n> to <player>`
  work as before. An entry a player brought back shows "restored ... by SELF"
  and an op restore of it is refused unless `force` is added.

## Files it adds
- `plugins/ExcelDeathRecord/lostfound/players/<uuid>.yml` (per-player state)
- `plugins/ExcelDeathRecord/lostfound.csv` (recoveries)
- `plugins/ExcelDeathRecord/config.yml.bak-pre-0.3.0` (one-time config backup)
