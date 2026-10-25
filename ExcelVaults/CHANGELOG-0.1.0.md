# ExcelVaults 0.1.0 (new plugin), Oct 25 2026

Status: BUILT and TESTED on the box rig (Paper 26.3). NOT on the test server, NOT live.
Java proof: rig bot only. Bedrock proof: protocol-level bot through Geyser + floodgate only (no real device yet).

## What it does
Trial chamber vaults give each player their reward once, forever, in vanilla. ExcelVaults makes a vault
reopen FOR THAT PLAYER after a real-time cooldown, and lets players pay to reopen one early.

### Reopening
- Per player, per vault. Cooldown starts when the player opens the vault with a key.
  - Normal vault: 24 hours (`normal.cooldown-hours`).
  - Ominous vault: 72 hours (`ominous.cooldown-hours`).
- Real time. It keeps counting while the player is offline and across restarts.
- A key is still needed. Reopening only puts the player back on the vault's "can open" list.
- Uses the Paper 26.3 Vault API only (`Vault#hasRewardedPlayer` / `removeRewardedPlayer`,
  `VaultChangeStateEvent`). No NBT edits.
- Ready vaults are reopened by a sweep every 5 s in loaded chunks. An unloaded vault reopens when its
  area loads, or on the next click. The player gets one chat line when a vault is ready again (`notify-ready`).

### Vaults used before install
- No mass unlock. A vault a player already used before ExcelVaults was installed has no record.
- The first time that player clicks it, its cooldown starts (24h or 72h from that click).

### Ominous vaults and the seals
- Ominous vaults behave exactly like vanilla until 2 GuildGate seals are broken (`ominous.min-seals: 2`).
- Opens before that are recorded quietly. Once the second seal falls, any whose time is already up reopen.
- Pay-to-reopen also waits for the second seal.
- Seal count comes from ExcelGuildGate (`GuildGateAPI.sealsBrokenCount()`, read by reflection). If
  ExcelGuildGate is missing, the gate counts as closed (ominous stays vanilla).

### Clicking a used vault
- Chat: "This vault opens again for you in Xh Ym." plus "Sneak + right-click it to reopen it now for $N."
- Java also gets a clickable "[Reopen now: $N]" button.
- Hints are rate-limited to one per 3 s per player.

### Pay to reopen (money sink)
- Sneak + right-click a locked vault, or `/vaults`, or the chat button.
  - Java: a small confirm screen (green "Reopen for $N", red "Cancel").
  - Bedrock: a Floodgate modal form ("Reopen for $N" / "Cancel"). `/vaults` on Bedrock is a tappable list.
- The prompt shows the time left, the CURRENT price, the player's balance, and (while the price is still
  dropping) "The price drops as the timer runs down (lowest $N)."
- Price (Mike's pick, Oct 25 4:33 PM CDT):
  - Normal: $750 when just locked. Ominous: $3,000 when just locked.
  - Drops in proportion to the time left: half the cooldown left = $375 / $1,500.
  - Rounded to the nearest $5 (`price-step: 5`). Never below $50 normal / $200 ominous (`min-price`).
  - `scale-with-time-left: false` makes it a flat full price.
- Daily cap: 2 paid ominous reopens per player per day (`ominous.reopen-now.daily-limit`, America/Chicago day).
  Normal has no cap (0).
- Safe charging: the price is quoted again at click time and the player pays the lower of shown and
  current. If the vault is gone or the reopen fails, the money is given back.
- Every purchase is written to `plugins/ExcelVaults/purchases.csv`
  (timestamp, player, uuid, platform, type, vault, price, minutes skipped, balance after, result)
  and to ExcelAuditLog as event `vault`.
- Needs Vault + an economy (EssentialsX). Without one, pay-to-reopen says "not available", no charge.

### Commands
| Command | Who | What |
|---|---|---|
| `/vaults` (`/trialvaults`) | everyone | Your vaults waiting to reopen, time left, price. |
| `/vaults help` | everyone | How it works. |
| `/vaults reopen <world> <x> <y> <z>` | everyone | Opens the pay prompt for that vault (used by the chat button). |
| `/vaults status [player]` | op | Cooldowns and paid-today counts. |
| `/vaults reset <player>` | op | Reopens all of that player's recorded vaults (seal gate still applies) and clears their paid-today count. |
| `/vaults reset <player> here` | op | Reopens the vault you look at for that player (ignores the seal gate). |
| `/vaults age <player> <minutes>` | op | Support/test: moves that player's open times back. |
| `/vaults reload` | op | Re-reads config.yml. No restart. |
Permissions: `excelvaults.use` (default true), `excelvaults.admin` (default op).

### Data
- `plugins/ExcelVaults/data.yml`: cooldowns (world;x;y;z -> player -> opened time) and paid-today counts.
- Saved every 5 s when changed, and on shutdown. Written to a temp file, then moved into place.
- An unreadable data.yml is renamed `data.yml.unreadable-<time>` and the plugin starts empty (nothing unlocks).

### Soft dependencies
Vault, floodgate, ExcelGuildGate, ExcelAuditLog. All optional. The plugin runs without any of them,
with the matching feature off.

## Not in this release
- No menu button, no GUI shop entry.
- No per-vault custom cooldowns.
