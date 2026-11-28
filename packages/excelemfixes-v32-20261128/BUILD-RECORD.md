# RECORD — Test-mode move-out (ExcelEMFixes v32 + ExcelTestMode v1)

Lane: MOVE-OUT BUILD LANE, Fri Nov 27, 2026, ~11:38 PM CT onward.
Trigger: Mike's ruling, Nov 27, 2026, 11:36 PM CT — "Test mode
probably shouldn't be part of Excel em fixes I don't think."
ExcelEMFixes is the EliteMobs fix pack; test mode fixes nothing,
it is a fleet testing tool. Execute the move-out: the code leaves
the pack and stands alone.

## What was built

### ExcelTestMode v1 (standalone plugin) — build-testmode/

The v31 test-mode module carried out whole: TestModeMath ported
verbatim (package exceltestmode), the module's logic re-hosted in
a plain JavaPlugin (Listener + CommandExecutor). Behavior
identical: victim-axis EntityDamageEvent at HIGHEST pricing the
event's FINAL damage, floor clamp (floor-hp 2.0), hits at/below
the floor land as 0-damage events (never a cancel), no
resistance/absorption, outgoing damage untouched, VOID/SUICIDE
excluded. Command surface identical: /testmode <player> on|off,
/testmode status, /testmode <player>; permission
exceltestmode.admin (op default). Config at the plugin's own
root: enabled: false, floor-hp: 2.0, players: [],
excluded-causes: [VOID, SUICIDE], debug: false — ships OFF,
empty list, zero WRV hardcoding. NO EliteMobs dependency (no
depend/softdepend; plugin.yml parses verified). README states
plainly: fleet testing tool, not a gameplay feature.
One behavior note vs the module: enabling is a boot-time config
read (no /emfixes reload to ride); toggles still write through
live.

- ExcelTestMode-v1.jar — 9,825 B / SHA-256
  8d290a32b3c6e326e1561cd84d00fc6406f0dd9d29b78775b307a4a800d9b6a9
  Deterministic (two builds byte-identical).
- Tests: TestModeTest ported unchanged — 33 passed / 0 failed.
- Build fix worth noting: the first plugin.yml draft failed YAML
  parse (plain-scalar description containing ": " sequences) —
  caught pre-rig by a PyYAML check, reworded, jar rebuilt (the
  9,824 B / 6410a4bc… intermediate hash is superseded by the
  final above).

### ExcelEMFixes v32 (module removed) — build-v32/

The v31 tree minus test-mode: testmode package + its tests
deleted, registry line removed, plugin.yml paragraph +
excelemfixes.testmode.admin node removed, bundled-config
section removed, VERSION rolled to v32 (plugin.yml, VERSION
constant, two version-pin tests). Grep-verified: zero testmode
references in src/, build.sh, run-tests.sh; jar contains zero
testmode classes; bundled config parses to 28 module sections.

- ExcelEMFixes-v32.jar — 708,298 B / SHA-256
  0a5815192ca179e255a7a4d62ffe08cf9236bde64972eb2f20874510aab98012
  Deterministic (two builds byte-identical).
- Tests: 2,237 passed / 1 failed — the failure is the
  PRE-EXISTING ClassFormTable pin ("Jester-shaped: armor named
  first on the tie"), identical on the pristine v30 tree
  (2,237 / 1) and on v31 (2,270 / 1). v31's +33 was TestModeTest,
  which moved out with the module (33 / 0 in the new tree).
  Disclosed, not hidden; not introduced by v32.
- Deploy-config: packet-v32/deploy-config.yml — 105,312 B /
  SHA-256 ccf08517e8e59f5175f5368bc2cd9574f9962f4b39333e46b554b6771863874c
  = the fresh G1 live v31 config (108,601 B / 30812b3d…) minus
  exactly the 68-line test-mode section — byte-identical to the
  v30 live config. YAML parse: 28 module sections, no test-mode
  key, live leash values intact by construction.

## Rig proof (local Paper 26.3-256 + EliteMobs; rig-logs/)

- Boot A (v32 solo, v32 deploy-config): "ExcelEMFixes v32
  ready", 28 modules listed (test-mode NOT among them),
  Done (36.008s), SEVERE 0. (boot-v32-solo.log)
- Boot B (ExcelTestMode v1 placed, shipped defaults):
  "ExcelTestMode present but DISABLED (enabled: false in
  config.yml)", Done (30.201s), SEVERE 0. (boot-testmode-off.log)
- Boot C (ExcelTestMode enabled in the rig copy only): "Test
  Mode ready - listed players take full damage but cannot drop
  below 2.0 HP (0 player(s) listed)"; console /testmode status
  answered; /testmode RigTester on logged the TEST-MODE ON line
  (uuid, floor 2.0, ISO timestamp, by=CONSOLE) and status showed
  1 listed; per-player read answered; /testmode RigTester off
  logged and the config write-through left players: [] at
  shutdown; Done (34.416s), SEVERE 0. (boot-testmode-on.log)
- A behavioral floor proof needs a joined player; the rig
  harness has none (same limit as the v31 lane). It is owed on
  the live server after install — retargeted by drop (b).

## Filing (this lane = the one MANIFEST writer)

MANIFEST.md PENDING now carries, in chain order:
1. "## PENDING — ExcelEMFixes v32 (test-mode move-out)
   (added Nov 27, 2026)"
2. "## PENDING — ExcelTestMode v1 (standalone test-mode plugin)
   (added Nov 27, 2026)"
One chained window, v32 first. Fresh G1 receipts for both
(bridge router, transports up — chisel / tailscale-ssh / panel):
live jar v31 716,847 B / db06d7b9…, live config 108,601 B /
30812b3d…, no ExcelTestMode jar/folder on live. Backup-first +
pair rollback written in each packet (restore v31 jar+config,
remove ExcelTestMode jar + folder). This lane did NOT install —
the failsafe owns placement. No live changes beyond read-only
pulls; Mike's account untouched; no GitHub writes from this seat.

## Surfaces check (for the follow-on)

Checked Nov 27, 2026 by this lane: NO v31 surfaces and NO v31
GitHub release went out before the move-out was ordered — no
~/workspace/emfixes-v31-surfaces* folder exists, and the v31
follow-on items (P-EMFIXES-V31-SURFACES-20261127,
P-EMFIXES-V31-TESTMODE-LIVEPROOF-20261127) still sat gated in
the parent backlog on the install. The original v31 D228 drops
(drop-1795842514319.json, drop-1795842514320.json) sit in
~/hooks/state/followon-drops/sent/. The surfaces therefore land
fresh as v32 + ExcelTestMode v1 — no v31 text needs correcting.

## Finish-and-fire (D228 drops fired by this lane)

- ~/hooks/state/followon-drops/drop-1795844876211.json —
  surfaces adjustment: supersedes the v31 surfaces follow-on;
  website row to v32, GitHub release "ExcelEMFixes v32" whose
  changelog leads with the move-out (and states no v31 release
  went out), What's New BOTH surfaces reflecting v32 +
  ExcelTestMode v1 (drafted text in the drop).
- ~/hooks/state/followon-drops/drop-1795844876212.json —
  live floor proof retargeted to the standalone ExcelTestMode
  plugin after the chained install, then the EliteMobs 10.9.17
  combat-end heal proof under its protection.

## Records amended

- This RECORD.md (new folder testmode-moveout-20261127/ with
  build-v32/, build-testmode/, packet-v32/, packet-testmode/,
  rig/, rig-logs/).
- ~/workspace/emfixes-v31-testmode-20261127/RECORD.md — dated
  move-out section appended (ruling, where the code lives now,
  retargeted follow-ons).
