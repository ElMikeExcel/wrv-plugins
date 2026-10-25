# Rig test results: ExcelClaimFly 0.1.0

Jar: ExcelClaimFly-0.1.0.jar, sha256 a5e6de66a6bb0f981ab4f6f1e4fc5a02391c2306cd78085744d3daf7a838556b
Rig: box test server /workspace/claimfly-rig (Paper 26.3, offline mode, Geyser 2.12.0 + floodgate 2.2.5, GriefPrevention 16.18.7,
LuckPerms 5.5.87, Vault + EssentialsX economy, ExcelMenu 0.1.5, ExcelTips 0.1.0, ExcelWhatsNew 0.1.0, ViaVersion for Java bots),
loaded with the STAGED menus, tips.yml and changelog.yml from this packet and the default config.yml from the jar.
Clean boot at 3:03 PM CDT Oct 25: "24 tips", "4 news entries (newest 2026-10-25-build-flight)", the ExcelClaimFly enabled line, no ExcelClaimFly errors.

| Suite | Time (CDT, Oct 25) | Result | Log |
|---|---|---|---|
| Unit tests (build.sh) | 3:02 PM | 35 pass, 0 fail | build output |
| Java bots, tests/rig-test.js | 3:03 to 3:07 PM | 53 pass, 0 fail | tests/rig-test.log |
| Tip + What's New, tests/tip-test.js | 3:07 PM | 12 pass, 0 fail | tests/tip-test.log |
| Bedrock protocol client, tests/bedrock-test.js | 3:09 PM | 17 pass, 0 fail (first attempt) | tests/bedrock-test.log |

## Java (53)
- Refusals: no claim means the make-a-claim message and no charge; no money means a refusal and no charge.
- Shop: the feather sits in Travel & Animals slot 5, balance in 6, Back in 7. The Java review screen has a lime "Buy 30 min for $1,000" button, and opening it charges nothing.
- Buy: charged $1,000, how-it-works chat, title + subtitle, a LuckPerms temporary node, allow-flight in the claim.
- Re-buy extends to about 60 min and charges $1,000. Going over the 120 min cap is refused with no charge.
- `/buildfly` status. A double-tap is accepted in the claim. The action bar shows the timer.
- Control: a 7-block fall hurts a player with no safe landing.
- Leaving the claim: message, flight off, the landing takes no fall damage. Safe landing is used up after one landing, so a later fall hurts. Re-entering gives the hint and flight back.
- Mob hit, arrow hit, and attacking a mob: paused with the message, the timer keeps counting on the action bar, the landing takes no fall damage. After 10 s quiet, "Flight is back".
- Flip `combat.pause-flight` off (a hit no longer stops flight), then back on.
- Warnings at 5 min and 1 min. Expiry: title + chat, flight off, the landing takes no fall damage.
- A pass that runs out while offline clears flight on rejoin, and the player is told.
- A trusted builder in the owner's claim cannot fly (owner only).
- Journal: exactly 1 BUY + 1 EXTEND for the run.

## Tip + What's New (12)
- Reloads: 24 tips, 4 entries, newest build-flight. `tips check` has no build-flight warning.
- After a fresh claim: the Claims tip first. The Build Flight tip does NOT fire inside the 10 s gap, then shows both lines (about 11 s).
- `/whatsnew` on Java shows "New: Build Flight pass" first with the approved lines, no em dash.
- `/buildfly` with no pass names the Spawn Shop, Travel & Animals.

## Bedrock (17)
- floodgate sees the client as Bedrock. No claim gives an info form with OK.
- Modal form: "Buy 30 min for $1,000" / "Cancel", plain body with no em dash. Showing the form charges nothing, Buy charges $1,000, Cancel charges nothing.
- How-it-works chat and title arrive. Geyser sends update_abilities with may_fly=true.
- Double-tap (player_auth_input start_flying) reaches the server as flying=true.
- Hit: paused on the server, Geyser removes may_fly, message shown. After 10 s quiet may_fly comes back. Leaving the claim removes may_fly.

## Test fixes made this session
- rig-test.js: the journal check counted every run's lines in the append-only purchases.log (52/1 on rerun). It now counts only this run's lines. Not a plugin bug.
- rig-test.js: the expected wording changed from "Excel Shop" to "Spawn Shop" (the menu title players see).
- tip-test.js (new): the physics-off bot was kicked for "Invalid move player packet". It now drops move packets like rig-test.js.
- bedrock-test.js: switched to the lf-tests copy of bedrock-protocol 3.59.0 (the other copy timed out at RakNet connect). The double-tap is now sent as player_auth_input start_flying (Geyser ignores request_ability on 1.26.40). Reset uses deleteallclaims + `buildfly end`. Login "Checksum mismatch" flakes are retried up to 4 times; none happened this run.
- Older logs are kept in tests/*.run*.log, including the 53/0 run on the previous build 4923302e (same code, older plugin.yml description).

## Not verified
- A real Bedrock phone, tablet or console with touch double-tap.
- No-fall-damage landing for Bedrock players (the fall damage checks were Java only; Bedrock covered may_fly on and off).
- The live economy, live LuckPerms storage, and live server load.
