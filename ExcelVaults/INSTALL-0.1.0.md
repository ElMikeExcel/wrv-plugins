# INSTALL: ExcelVaults 0.1.0, NOT DONE

Staged Oct 25 2026, about 5:00 PM CDT. Approved by Mike Oct 25 4:09 PM CDT as a staging build. Prices picked by Mike 4:33 PM CDT.
Nothing here is live. No panel step has been taken. Install needs Mike's go in the moment.

One part only: the jar (needs a restart). No config upload: the jar writes its default config.yml on first start.
The tip and What's New entry are TEXT DRAFTS only (TIP.txt, WHATS-NEW.txt). Compose them onto the live
ExcelTips / ExcelWhatsNew files in a separate step, with their own drift guard.

## Preconditions
1. Player gate: console `list` must read 0 players on TWO fresh readings. If anyone is on, hold.
2. Live versions must match the rig: Paper 26.3, Vault 1.7.x, EssentialsX economy, Geyser + floodgate,
   ExcelGuildGate 0.3.3 (rig), ExcelAuditLog 0.2.0 (rig). If ExcelGuildGate or ExcelAuditLog differ, re-test on the rig first
   (both are reached by reflection).
3. `sha256sum -c SHA256SUMS` in this folder.
4. Drift guard: there must be no ExcelVaults jar and no plugins/ExcelVaults folder on the server yet.

## Backup
Nothing is replaced (new jar). Take the usual panel backup first. Keep at most 2 panel backups: replace the manual one.

## Upload
| Staged file | Live path | sha256 |
|---|---|---|
| ExcelVaults-0.1.0.jar | plugins/ExcelVaults-0.1.0.jar | afdf1ec20b6b24575de7179b6c3d9e3ca80a458d950afeb00b6fbb232a2796bf |
Upload under a temp name, rename into place, then check the sha256 or the exact size (see SHA256SUMS).

## Steps
1. `list` -> 0 players (two reads). Write down `guildgate status` (seal count).
2. Upload the jar, check the sha.
3. Panel Shut Down (never console `stop`), then Start. Or let the nightly restart pick it up.
4. Boot must show (the seal part depends on the live seal count):
   `[ExcelVaults] ExcelVaults enabled: normal 24h, ominous 72h after seal 2 (seals N of 5, ominous waiting|OPEN), reopen-now $750 scaling to min $50 / $3,000 scaling to min $200, 0 cooldowns on 0 vaults, Bedrock forms on, audit on.`
   - "seals ? of 5" means ExcelGuildGate was not found: ominous stays vanilla. Report it.
   - "audit off" means ExcelAuditLog was not found: purchases still go to purchases.csv. Report it.
   - No ERROR lines from ExcelVaults.
5. `vaults reload` must answer with the same summary.
6. `guildgate status` -> same as before.
7. Statuses after this: "live" only. Java proof and Bedrock proof stay separate until a real player confirms each.

## After install
- Nothing changes for anyone until they click a vault they already used. No vault is mass-unlocked.
- Watch `plugins/ExcelVaults/purchases.csv` and `/audit <player> 24 all` for the first paid reopens.
