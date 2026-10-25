# Changelog: ExcelClaimFly

## 0.1.0 (Oct 25 2026), staged, NOT LIVE
New plugin. Build Flight pass for Wink Reyes Valley.

### What players get
- New: **Build Flight Pass** in the Spawn Shop, Travel & Animals (feather, slot 5). $1,000 of in-game money (Vault/EssentialsX, the same money Jobs pays) buys 30 minutes.
- The pass is a LuckPerms **temporary** permission (`excelclaimfly.pass`). LuckPerms keeps the clock, so it runs in real time, keeps counting while you are offline and survives restarts.
- Buying again while a pass is active adds 30 minutes, up to 120 minutes in total. Above that you are refused and nothing is charged.
- Flight works **only inside your own GriefPrevention claim**, in survival or adventure mode. Players you /trust cannot fly in your claim (`trusted-builders-can-fly: false`). No flight in world_the_end.
- Double-tap jump to fly (Java and Bedrock). Double-tap again to stop.
- **Leaving your claim** turns flight off and you land safe: no fall damage on that one landing (at most 30 s). Back in your claim, a hint tells you to double-tap jump again (at most once a minute).
- **Fights:** a hit from a mob, any projectile or a player, or hitting something yourself, pauses flight and lands you safe with no fall damage. The pass timer keeps running. After 10 s with no hits, "Flight is back" and you can double-tap jump again (if you are still in your claim). The combat tag is the plugin's own 10 s clock, not a separate combat-tag plugin. Config flip: `combat.pause-flight`.
- Warnings at 5 min and 1 min left (title + chat). At the end: "Build Flight ended", flight off, safe landing.
- Action bar shows the time left while you fly, and "Flight paused (fight)" with the timer during a fight.
- `/buildfly` (alias `/claimfly`) shows time left and whether you can fly where you are. `/buildfly help` explains it. `/buildfly buy` opens the review screen.
- Purchase review screen with no charge until you tap Buy: a chest screen on Java, a tappable form (floodgate/Cumulus modal) on Bedrock. Players with no claim or not enough money get a plain explanation and are not charged.
- After buying: a how-it-works chat message plus the title "Build Flight on" / "Double-tap jump in your claim".
- Logging off while flying: on the next join you land safe. If the pass ran out while you were away, flight is cleared and you are told.

### Admin
- `/buildfly give <player> <min>`, `end <player>`, `info <player>`, `confirm <player>` (buys without the screen, for tests), `reload` (re-reads config.yml, no restart). Permission `excelclaimfly.admin` (op).
- The shop button runs `[console] buildfly buy %player_name%`.
- `plugins/ExcelClaimFly/purchases.log` journals every BUY and EXTEND (time, price, time left, balance after). state.yml keeps safe-landing state across restarts.
- If the money is taken but the LuckPerms node fails, the money is given back ("Something went wrong, so your $1,000 was given back.").
- All player text lives in config.yml (`messages:`), plain words, no em dashes.
- Depends on Vault, LuckPerms and GriefPrevention. Soft-depends on floodgate (Bedrock forms).

### Bedrock (Geyser 2.12.0, floodgate 2.2.5)
- Geyser turns the server's allow-flight into the client's may_fly ability, so double-tap jump works on Bedrock. Turning it off (hit, leaving the claim, expiry) removes may_fly on the client.
- Bedrock 1.26.40 sends a double-tap as `player_auth_input` with `start_flying`, and the rig test sends exactly that.

### Server-side packet (not in this repo)
- ExcelMenu: spawnshop.yml (Travel & Animals lore says 6 products) and spawnshop_travel_animals.yml (new button; balance moves to slot 6, Back to 7).
- ExcelTips: new `build-flight` tip, 11 s after a player makes a claim (after the Claims tip, past the 10 s gap) and on /buildfly. needs-plugin ExcelClaimFly.
- ExcelWhatsNew: new top entry `2026-10-25-build-flight`.

### Build
- `build.sh`: JDK 25, `--release 21`, fixed jar date, so the build is deterministic. Runs 35 unit tests (FlightRules, CombatClock).
- Jar sha256 a5e6de66a6bb0f981ab4f6f1e4fc5a02391c2306cd78085744d3daf7a838556b (rebuilt twice, identical).

### Tested
Box rig only (Paper 26.3, offline mode): Java 53/0, Bedrock protocol client 17/0, tip + What's New 12/0, unit 35/0. See RIG-TEST-RESULTS.md.

### Not verified
- A real Bedrock device and touch controls (only a protocol client was tested).
- No-fall-damage landing for Bedrock players (the fall damage checks used Java bots).
- The live economy and live LuckPerms storage.
