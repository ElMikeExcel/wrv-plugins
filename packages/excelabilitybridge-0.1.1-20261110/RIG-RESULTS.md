# RIG RESULTS — ExcelAbilityBridge 0.1.1 (console admin)

Rig: local Paper 26.2-123 server (the 0.1.0 scratch rig tree
from `~/workspace/excelabilitybridge/scratch/server/`, copied
to a scratch `rig-work/` folder that was deleted after
evidence capture) with ExcelAbilityBridge-0.1.1.jar (SHA-256
verified 62076c15…e55 in the rig plugins folder before boot),
the pinned EliteMobs 10.9.8, Geyser-Spigot (the live geyser
jar) and floodgate — the bridge's real runtime neighbours.
Offline mode, fresh world. Full log: `boot.log` in this
folder; the exact console commands: `cmdfile.txt`. Every
command was typed on the server's stdin — the REAL console
sender (`CONSOLE`), exactly the caller this packet exists
for. This is run 2 of two; run 1 is summarised at the end.

## Boot (run 2)

- "Enabling ExcelAbilityBridge v0.1.1" present; the enable
  takes the normal healthy shape: "EliteMobs class system
  (ACS) is still starting - Bedrock ability bridge standing
  by; it comes online automatically when ACS is up."
- At 04:16:18, ~17 seconds after Done: "EliteMobs class
  system (ACS) is up - Bedrock ability bridge online:
  /abilities" — the 0.1.0 watch transition, unchanged.
- Done (36.096s). SEVERE: 0 in the whole boot + exercise +
  shutdown.
- Clean stop: "Disabling ExcelAbilityBridge v0.1.1".

## Behavior exercise — 7/7 PASS (verbatim lines in boot.log)

| # | Check | Result |
|---|---|---|
| 1 | Boot reaches Done with v0.1.1 enabled beside EliteMobs 10.9.8 + Geyser + floodgate | PASS |
| 2 | SEVERE count for the whole run | PASS — 0 |
| 3 | The ACS watch transition fires ("bridge online") exactly as in 0.1.0 | PASS |
| 4 | `abilities status` from console → line 1 verbatim: "ExcelAbilityBridge status: enabled (v0.1.1)" | PASS |
| 5 | `abilities status` → EliteMobs link line verbatim: "EliteMobs link: UP - EliteMobs 10.9.8 enabled, Advanced Combat System READY." (the status reads the gateway's real moduleReady state, after the transition) | PASS |
| 6 | `abilities status` → Geyser + config lines verbatim: "Geyser link: Geyser-Spigot enabled, floodgate enabled - Bedrock form transport available." and "Config: debug-log-fires OFF. Permissions: excelabilitybridge.admin (default op) for this status; /abilities itself stays open to every player." | PASS |
| 7 | Bare `abilities` from console → the exact 0.1.0 line ("The ability bridge is for players in game.") — the player path unchanged for console; `plugins` lists EliteMobs (10.9.8), ExcelAbilityBridge (0.1.1), Geyser-Spigot, floodgate; clean stop | PASS |

## Run 1 — the STANDING BY proof (also a pass, kept as evidence in prose)

Run 1 used the identical jar and stack but sent `abilities
status` ~8 seconds after Done, before ACS finished
initializing. The status answered honestly:
"EliteMobs link: STANDING BY - EliteMobs 10.9.8 enabled,
Advanced Combat System not ready yet (the bridge comes
online automatically when ACS is up)." — the surface reports
the link state it observes, never a canned UP. SEVERE was 0
in run 1 as well. Run 1's log was replaced by run 2's in this
folder; both runs' status lines are quoted here verbatim.

## Limitations, stated plainly

- The non-admin denial was NOT exercised in this rig: there
  is no player sender in this rig. The gate arithmetic
  (console allowed / node-holder allowed / non-admin denied)
  is pinned by the StatusCommandTestMain harness checks, and
  the node itself (default: op) is pinned there against the
  shipped plugin.yml. A live in-game denial check by a
  non-op family member is a fair post-install proof if Mike
  wants one.
- No ability was fired in this rig: 0.1.1 changes no fire
  path (at jar level only AbilitiesCommand.class and
  ExcelAbilityBridge.class changed and StatusCommand.class
  was added; the gateway, form, and fire classes are
  byte-identical to the live 0.1.0 jar), and the 0.1.0 rig
  already proved all three slots firing through the
  production call path. The DOWN status shape (EliteMobs
  absent) is unreachable in a normal boot — the plugin
  disables itself in that state by 0.1.0 design — so that
  line is harness-pinned only, and says so in its own text.
