# RIG RESULTS — ExcelChunks 0.1.1 (console admin)

Rig: local Paper 26.2-123 server (the 0.1.0 full-stack rig
tree from `~/workspace/excelchunks/rig-server/`, copied to a
scratch `rig-work/` folder that was deleted after evidence
capture) with ExcelChunks-0.1.1.jar (SHA-256 verified
8d9b47e3…3877 in the rig plugins folder before boot),
Geyser-Spigot 2.11.3 and floodgate 2.2.5 — the same stack the
0.1.0 rig proved against. Offline mode. Full log: `boot.log`
in this folder; the exact console commands: `cmdfile.txt`.
Every command was typed on the server's stdin — the REAL
console sender (`CONSOLE`), exactly the caller this packet
exists for.

## Boot

- "Enabling ExcelChunks v0.1.1" present; the enable line
  keeps its 0.1.0 shape verbatim: `ExcelChunks enabled:
  /chunks view resync ON (item radius 96 blocks, cooldown
  10s)`.
- Done (31.813s). SEVERE: 0 in the whole boot + exercise +
  shutdown.
- Clean stop: "Disabling ExcelChunks v0.1.1" +
  "ExcelChunks disabled."

## Behavior exercise — 6/6 PASS (verbatim lines in boot.log)

| # | Check | Result |
|---|---|---|
| 1 | Boot reaches Done with v0.1.1 enabled, 0.1.0-shaped enable line | PASS |
| 2 | SEVERE count for the whole run | PASS — 0 |
| 3 | `chunks status` from console → all three status lines verbatim: "ExcelChunks status: enabled (v0.1.1)", "View resync: ON - same-spot teleport + dropped-item hide/show cycle; item radius 96 blocks, cooldown 10s per player, hide/show delay 2 ticks.", "Config: no config file - radius and cooldown are built-in constants. Permissions: excelchunks.use (default true) for /chunks; excelchunks.admin (default op) for this status." | PASS |
| 4 | Bare `chunks` from console → the exact 0.1.0 refusal line ("ExcelChunks: /chunks only works in game - it refreshes the view of the player who runs it.") — the self path is unchanged for console | PASS |
| 5 | `plugins` → ExcelChunks (0.1.1), Geyser-Spigot (2.11.3-SNAPSHOT), floodgate (2.2.5-SNAPSHOT) | PASS |
| 6 | Clean stop with "Disabling ExcelChunks v0.1.1" | PASS |

## Runner note (not a jar defect)

The first invocation of this rig (run 011) booted the same
jar cleanly and stopped cleanly, but exercised no commands:
the runner was passed a steps-file path relative to the
wrong directory, so it read no steps. Run 011b (the evidence
above) re-ran with the absolute path. The jar was identical
in both runs.

## Limitations, stated plainly

- The non-admin denial was NOT exercised in this rig: there
  is no player sender in this rig at all. The gate arithmetic
  (console allowed / node-holder allowed / non-admin denied)
  is pinned by the StatusCommandTest harness checks, and the
  node itself (default: op) is pinned there against the
  shipped plugin.yml. A live in-game denial check by a non-op
  family member is a fair post-install proof if Mike wants
  one.
- The resync cure itself (a Bedrock client re-rendering its
  items) is the 0.1.0 player proof still owed live (Kailey
  first, Rylie second); 0.1.1 changes no resync code path —
  at jar level only ExcelChunks.class changed and
  StatusCommand.class was added, every other entry is
  byte-identical to the live 0.1.0 jar.
