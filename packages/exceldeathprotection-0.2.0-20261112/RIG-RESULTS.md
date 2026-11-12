# RIG RESULTS — ExcelDeathProtection 0.2.0

Rig: local Paper 26.3 (build tree copied from the ExcelTrainingDummy
1.0.0 rig, `rig-0.2.0/server/`) with ExcelDeathProtection-0.2.0.jar
(SHA-256 verified `ed819bb6…0719` in the rig plugins folder before
boot), Vault 2.0.0, Geyser-Spigot 2.11.3-SNAPSHOT + floodgate
2.2.5-SNAPSHOT co-loaded, and the DpRig driver plugin (NMS fake
player, the house rig pattern; `driver-src/`). Offline mode. Two
boots: `boot.log` (phase 1, behaviour) and `boot2.log` (phase 2,
restart persistence). The driver shut the server down itself at the
end of each phase.

## Boot (both phases)

- "Enabling ExcelDeathProtection v0.2.0" + enable lines:
  `ExcelDeathProtection 0.2.0: basic $15,000.00, premium $30,000.00
  (build EXC-DPROTECT-0.2.0-WRV-20261112-01-d2b348)` and
  `ExcelDeathProtection API: premium-credit seam active (creditPremium)`.
- Done (44.262s phase 1). **SEVERE: 0** in both boots, exercise and
  shutdown. Clean stop: "Disabling ExcelDeathProtection v0.2.0".
- One expected WARN: "Vault economy unavailable; purchases
  disabled." — the rig carries the Vault plugin (the hard `depend`)
  but no economy provider plugin, so the purchase flow is disabled
  by design; the premium-credit seam does not use the economy and
  was fully exercised. Purchase-flow behaviour is carried by the
  0.1.3 unit suite (85 purchase/journal/menu tests, all passing).

## Phase 1 — behaviour exercise, 14/14 PASS (RIGOBS lines in boot.log)

| # | Check | Result |
|---|---|---|
| 1 | Plugin enabled, description version 0.2.0 | PASS |
| 2 | NMS fake player DpRigA joins | PASS |
| 3 | BossTrial probe replica resolves the API class via DeathProtection's classloader (first candidate name) | PASS |
| 4 | Probe replica resolves public static `creditPremium(Player, int)` with an exact `int` second parameter | PASS |
| 5 | `invoke(null, player, 1)` (BossTrial's exact shape) → true; tier NONE→PREMIUM, banked 0 | PASS |
| 6 | 1-arg `invoke(null, player)` while PREMIUM active → true; banks (banked 1) | PASS |
| 7 | Console `/dp credit DpRigA 2` → banked 3 (log line source=admin) | PASS |
| 8 | Console `/dp uncredit DpRigA 1` → banked 2 | PASS |
| 9 | Death 1 (real lethal damage + respawn): protection consumed, diamond sword KEPT (keep-inventory), banked credit re-arms → tier PREMIUM, banked 1 | PASS |
| 10 | Death 2: re-armed protection consumed, last banked credit re-arms → tier PREMIUM, banked 0, sword kept | PASS |
| 11 | Death 3: protection consumed, bank empty → tier NONE after respawn (no phantom re-arm) | PASS |
| 12 | Pre-restart state set by two console credits: tier PREMIUM, banked 1 | PASS |
| 13 | Explicit `saveData()` → the player's `.dat` on disk contains `premium-credits` inside `BukkitValues` | PASS |
| 14 | After a proper disconnect (quit-save path), the `.dat` still contains `premium-credits` | PASS |

Console `/dp status DpRigA`, player `/dp status`, and console
`/dp version` were also dispatched in phase 1 (version answered
with the build line in the log).

## Phase 2 — restart persistence, 3/3 PASS (RIGOBS lines in boot2.log)

| # | Check | Result |
|---|---|---|
| 1 | Plugin enabled on the restart boot | PASS |
| 2 | Same-UUID fake player rejoins | PASS |
| 3 | Tier PREMIUM and banked 1 read back after restart — the PDC bank survived | PASS |

## Rig-driver note (a driver artifact found and fixed, not a plugin defect)

The first phase-2 run read tier NONE / banked 0 after restart.
Investigation proved the plugin side correct — checks 13/14 above
show the bank on disk in the saved `.dat` — and the miss was in
the driver: a hand-constructed NMS `ServerPlayer` placed with
`placeNewPlayer` never gets its `.dat` loaded (the real login flow
loads it beforehand). The driver now calls
`PlayerList.loadPlayerData(NameAndId)` and applies it through
`TagValueInput` before placement, exactly as login does; phase 2
then passed. A real client login was never affected by this.

## Limitations, stated plainly

- No economy provider in the rig: the two-tier purchase flow was
  not re-driven end-to-end here; it is unchanged 0.1.3 code covered
  by its unit suite. A live/Vault-backed purchase proof remains
  install-time player proof, as it was for 0.1.3.
- No BossTrial jar in the rig: the seam was driven through a
  verbatim replica of BossTrial's probe/invoke code (in the driver
  and in the unit suite), not through BossTrial itself. The first
  real repeat/first-break verdict after install is the live proof
  (see the draft MANIFEST entry).
- Chat messages to the fake player are not visible in the console
  log; message wording is in the source and the status/credit text
  paths were dispatched without error.
- No Bedrock client on this rig: status/icon display on Bedrock is
  owed player proof after install, matching 0.1.3's gate pattern.
