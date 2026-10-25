# RIG TEST RESULTS: ExcelVaults 0.1.0

Jar tested: ExcelVaults-0.1.0.jar sha256 afdf1ec20b6b24575de7179b6c3d9e3ca80a458d950afeb00b6fbb232a2796bf (same bytes staged).
Run Oct 25 2026, 4:42 PM to 4:53 PM CDT, on the box rig only.

## Totals
| Suite | Pass | Fail |
|---|---|---|
| Unit tests (TestMain, build.sh) | 91 | 0 |
| Java rig part 1 (rig-test.js) | 54 | 0 |
| Java rig part 2 after restart (rig-test2.js) | 9 | 0 |
| Bedrock rig (bedrock-test.js, through Geyser + floodgate) | 15 | 0 |
| Total | 169 | 0 |

## Rig
- Paper 26.3 (build 214), offline, flat world, port 26261 on the box (loopback only).
- EssentialsX economy via Vault, ExcelGuildGate 0.3.3, ExcelAuditLog 0.2.0, Geyser + floodgate, ViaVersion/ViaBackwards.
- Java bot: mineflayer 1.21.11 through Via. Bedrock bot: bedrock-protocol 3.59.0 (1.26.40) through Geyser.
- Console by RCON. Only the rig's own java process was restarted. No other rig was touched.

## Java checks (part 1)
- PASS fresh vault: bot not rewarded
- PASS unused vault + empty hand: no hint (vanilla)
- PASS open with trial key: vanilla rewards the bot
- PASS open recorded: "reopens in 24h 0m"
- PASS status shows the 24h cooldown
- PASS hint: "This vault opens again for you in 23h 59m"
- PASS hint: pay option $750 (just locked = full price)
- PASS hint: clickable Java button
- PASS hint is rate-limited (3 s)
- PASS sneak + right-click: "Reopen trial vault" screen
- PASS Java confirm: lime "Reopen for $750"
- PASS screen open: no charge yet
- PASS cancel: no charge, vault still locked
- PASS poor: refused with price and balance
- PASS poor: no charge, still locked
- PASS buy: charged $750 (10,000 -> 9,250)
- PASS buy: bot removed from rewarded list (Paper API)
- PASS buy: confirmation message
- PASS buy: console line
- PASS purchases.csv line (type, vault, price, minutes skipped, balance, result)
- PASS ExcelAuditLog CSV has the "vault" purchase line
- PASS /audit VtJava 1 all (as op) shows the vault purchase
- PASS reopened vault opens again with a key
- PASS 12h left: hint says 12h 0m and $375 (half price)
- PASS /vaults reopen opens the same screen at $375
- PASS /vaults reopen buy: charged $375, reopened
- PASS opened again
- PASS 24h passed: sweep re-allowed it without a click
- PASS ready message
- PASS cooldown record cleared after reopen
- PASS /vaults lists the vault with time and price
- PASS pre-install lock set on vault P
- PASS no mass unlock: pre-locked vaults stay locked (sweep ignores unknown)
- PASS first click on a pre-locked vault starts its timer
- PASS pre-locked vault hint: 24h 0m
- PASS pre-locked vault stays locked
- PASS ominous vault opens (vanilla) with an ominous key
- PASS ominous open recorded silently, waits for seal 2
- PASS gated note on click
- PASS gated: no price offered
- PASS gated: sneak does not open a pay screen
- PASS gated: /vaults reopen refused, no charge
- PASS gated: 83h later still locked (seal 0)
- PASS 1 seal: still locked
- PASS seal 2 broken: sweep re-allows the ominous vault (time already up)
- PASS ominous ready message
- PASS ominous just locked: $3,000
- PASS ominous paid reopen 1 of 2: charged $3,000
- PASS ominous paid reopen 2 of 2: charged $3,000
- PASS 3rd ominous paid reopen today refused (limit 2), no charge
- PASS non-op: /vaults reset refused
- PASS admin reset: all recorded vaults reopen (normal, ominous at seal 2, pre-locked)
- PASS admin reset of one player leaves others alone
- PASS restart prep: vault opened again (fresh 24h cooldown)

## Java checks (part 2, after a full rig restart)
- PASS restart: the cooldown was loaded from data.yml
- PASS no temp or unreadable data files left behind
- PASS status after restart still shows about 23h left
- PASS vault still locked for the bot after restart
- PASS hint after restart: about 23h left, $720 (750 x 23/24 to nearest $5)
- PASS /vaults reload answers
- PASS after reload with full-price 1000: $960 (1000 x 23/24)
- PASS reverted config + reload: back to $720
- PASS non-op: /vaults reload refused

## Bedrock checks
- PASS Bedrock player is on the Java server through Geyser
- PASS Bedrock tap with a trial key opens the vault (vanilla rewards the player)
- PASS open recorded (24h cooldown)
- PASS Bedrock hint: time left and $750
- PASS Bedrock sneak + tap: modal form, "Reopen for $750" / "Cancel"
- PASS Bedrock confirm body: time left, price-drops line, balance, no em dash
- PASS form shown: no charge yet
- PASS Bedrock Cancel: no charge, still locked
- PASS /vaults reopen from Bedrock: same modal
- PASS Bedrock tap Reopen: charged $750, vault reopened
- PASS Bedrock: confirmation chat
- PASS purchases.csv: platform bedrock
- PASS opened again
- PASS /vaults on Bedrock: SimpleForm list, button shows 12h 0m | $375
- PASS tap list entry: confirm at $375 (half the cooldown left)

## Geyser and vault visuals
- Geyser sends the Bedrock client vault block updates and vault block entity data (display item, connected players)
  when the vault changes state, including after a paid reopen. So the vault does change on the Bedrock client.
- NOT verified: how it LOOKS on a real Bedrock device (glow / inactive look per player). The bot cannot see.

## Not verified
- Real Bedrock device (form layout on phone/console, sneak + tap feel). Protocol bot only.
- Real Java client look of the confirm screen. Bot only.
- Live economy numbers (no live balances on the box). Prices are Mike's pick.
- Live ExcelGuildGate and ExcelAuditLog versions (rig used 0.3.3 and 0.2.0; both are reached by reflection).
- Real trial chambers (rig used placed vaults in a flat world; the vault block and API are the same).
- `/audit` type filter: ExcelAuditLog 0.2.0 has no "vault" type. The purchase shows under `/audit <player> <hours> all`
  (checked as op in the rig). The money drop also shows under type "money" when the sampler catches it.

## Notes
- Rig only: the "unsupported server version" ERROR is EssentialsX on Paper 26.3 (same on the claimfly rig).
- A "No key layers in MapLike" ERROR seen on one earlier rig boot comes from the flat-world preset, before plugins load.
- Test scripts read the rig RCON password from RIG_RCON_PASSWORD in the repo copy (rig-local test value, not a server secret).
