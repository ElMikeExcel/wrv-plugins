# ExcelVaults

Trial chamber vaults that reopen. Paper 26.3, Java + Bedrock (Geyser/floodgate).

In vanilla each vault rewards each player once, forever. ExcelVaults reopens a vault for a player after a
real-time cooldown (24h normal, 72h ominous), and lets players pay to reopen it early as a money sink.
A trial key is still needed to open it.

- Paper Vault API only (`org.bukkit.block.Vault`, `VaultChangeStateEvent`). No NBT edits.
- Vaults used before install are not mass-unlocked: the timer starts on the player's first click.
- Ominous vaults stay vanilla until 2 ExcelGuildGate seals are broken (configurable).
- Pay to reopen: sneak + right-click a locked vault, or `/vaults`. Java confirm screen, Bedrock Floodgate form.
  Price drops with the time left: $750 / $3,000 at the start, half at halfway, nearest $5, min $50 / $200.
- Purchases go to `plugins/ExcelVaults/purchases.csv` and ExcelAuditLog (event `vault`).

See CHANGELOG-0.1.0.md for the full feature list, commands and config, INSTALL-0.1.0.md / ROLLBACK-0.1.0.md for
deployment, and RIG-TEST-RESULTS-0.1.0.md for what was tested.

## Build
`./build.sh` (JDK 25, `--release 21`). Needs the Paper 26.3 API jar and its libraries, VaultAPI 1.7 and floodgate API
on the paths set at the top of build.sh. Runs the unit tests, then writes `../ExcelVaults-0.1.0.jar` (deterministic timestamps).

## Rig tests
`rig-tests/` holds the box rig scripts (Java mineflayer bot, Bedrock bedrock-protocol bot, RCON helper).
They expect a local Paper 26.3 rig with EssentialsX, Vault, ExcelGuildGate, ExcelAuditLog, Geyser and floodgate,
and `RIG_RCON_PASSWORD` set in the environment.
