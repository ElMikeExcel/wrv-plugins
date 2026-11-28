# INSTALL PACKET - ExcelTestMode v1 (standalone)

Lane: build (filed Nov 27, 2026). CHAINED
WINDOW 2 of 2: installs SECOND, in the same
window as ExcelEMFixes v32 (packet-v32 in the
same lane folder), after it.

## What ships

A NEW standalone Paper plugin: ExcelTestMode
v1 - a fleet testing tool, NOT a gameplay
feature and NOT an EliteMobs fix. It carries
the test-mode code out of ExcelEMFixes v31,
where it lived for one evening as a module.

WHY (the ruling): Mike, Nov 27, 2026, 11:36 PM
CT - "Test mode probably shouldn't be part of
Excel em fixes I don't think." ExcelEMFixes is
the EliteMobs fix pack; test mode fixes
nothing, it is a fleet testing tool, so it
stands alone. ExcelEMFixes v32 (install 1 of 2
in this window) removes the module; this
plugin is the code's home from v1 on.

Behavior (identical to the v31 module): for
players on the plugin's list, incoming damage
registers FULLY and normally - the damage
event fires unmodified, combat state, aggro
and heal mechanics engage exactly as stock,
outgoing damage is untouched - except that the
portion of a hit which would carry the player
below the configured health floor (shipped
2.0 HP = one heart) is trimmed so the hit
lands them exactly ON the floor. A listed
player can be hurt, never killed. A player
already at or below the floor takes 0-damage
hits (the events still fire; nothing is ever
cancelled) until they heal back above it.

Mechanism: a victim-axis EntityDamageEvent
handler at HIGHEST priority pricing the
event's FINAL damage (the TrainingWheels
shield shape - the D208 per-player mitigation
precedent), the clamp expressed on the event's
base damage through its own final/base ratio
plus one settle pass. No resistance/absorption
effects; no side channels. VOID and SUICIDE
excluded by shipped default (config-driven).
No world scope: the list is the scope.

Control: /testmode <player> on|off (console,
or in game with exceltestmode.admin, default
op), /testmode status, /testmode <player>.
Toggles write through to the plugin's own
config.yml (players list, UUIDs) and log one
line each: player, on/off, floor, ISO
timestamp, issuer.

Differences from the module packaging (only
these): config keys sit at the plugin's own
config root (enabled / floor-hp / players /
excluded-causes / debug - same names, same
defaults); the permission node is
exceltestmode.admin; enabling is a boot-time
config read (set enabled: true and restart -
there is no /emfixes reload to ride anymore);
NO EliteMobs dependency (plain Bukkit/Paper,
no depend or softdepend entries).

Ships OFF (enabled: false) with an EMPTY list:
installing it changes NOTHING in live play -
the plugin loads, logs one DISABLED line, and
registers no handler and no command until an
operator enables it for a test session.

## Ordering

Install 2 of 2 in the chained window, after
ExcelEMFixes v32. No config composition: this
is a new plugin; its deploy-config.yml is its
own shipped config (place it as
plugins/ExcelTestMode/config.yml so the placed
defaults are explicit; it is byte-identical to
the config bundled in the jar).

## Live-premise receipts (G1)

Fresh pull, Nov 27, 2026 ~11:38 PM CT via the
bridge router (copies in this packet's
pull-g1/; see RECEIPTS.md):
- Live jar: plugins/ExcelEMFixes-v31.jar -
  716,847 B / SHA-256 db06d7b9... (the chained
  window's premise: v32 swaps this jar first).
- Live config: plugins/ExcelEMFixes/config.yml -
  108,601 B / SHA-256 30812b3d... (the v31
  deploy-config; its test-mode section has
  never been enabled - the list is empty).
- Live plugins census (same session): NO
  ExcelTestMode jar and NO ExcelTestMode data
  folder exist on live yet - this placement is
  purely additive.

## Candidate

- ExcelTestMode-v1.jar - 9,825 B / SHA-256
  8d290a32b3c6e326e1561cd84d00fc6406f0dd9d29b78775b307a4a800d9b6a9
  (deterministic build: two consecutive builds
  byte-identical). plugin.yml inside the jar:
  name ExcelTestMode, version v1, main
  exceltestmode.ExcelTestMode, no depend /
  softdepend.
- deploy-config.yml - 3,557 B / SHA-256
  795a8b7cee3b665f6ec91a18c1bda217e9857c25d119f20e26af35f454ccb18a
  = the jar's bundled config (enabled: false,
  floor-hp: 2.0, players: [], excluded-causes:
  [VOID, SUICIDE], debug: false; zero WRV
  values).
- Tests: the TestModeTest checks ported
  unchanged from the v31 tree - 33 passed / 0
  failed (floor sanitize, break test,
  allowance, rescale, the end-to-end promise:
  20 HP, floor 2, a 25-final hit -> health
  lands exactly on 2.0).
- Rig proof (local Paper 26.3-256 + EliteMobs
  alongside ExcelEMFixes v32; logs in the lane
  folder's rig-logs/):
  boot B (jar placed, no data-folder config -
  shipped defaults) - ExcelTestMode v1 loads,
  "present but DISABLED" line, Done (30.201s),
  SEVERE 0;
  boot C (rig copy of the config flipped to
  enabled: true) - "Test Mode ready - listed
  players take full damage but cannot drop
  below 2.0 HP (0 player(s) listed)", console
  /testmode status answered, /testmode
  RigTester on logged the TEST-MODE ON line
  (uuid, floor, ISO timestamp, by=CONSOLE) and
  wrote the list through to config, status then
  showed 1 listed, per-player read answered,
  /testmode RigTester off logged + emptied the
  list, Done (34.416s), SEVERE 0.
  The behavioral floor proof (a staged lethal
  hit on a listed player) needs a joined
  player; the rig harness has none - it is a
  live-server proof, owed after install (see
  Proof owed).

## Live-jar gate (STOP on mismatch)

Purely additive placement: STOP if an
ExcelTestMode jar or data folder already
exists on live at the swap moment (there was
none at G1). The chained window's ExcelEMFixes
gate lives in the v32 packet. Marker re-checks
before Shut Down and before Start per standing
discipline; BACKUP FIRST + cap-at-6; emptiness
list = 0/10 twice (weekend protocol if Mike
directs otherwise).

## Scope

Place ExcelTestMode-v1.jar + deploy-config.yml
as plugins/ExcelTestMode/config.yml ONLY (in
the same window as, and after, the v32 swap).
Untouched: every other plugin, all EliteMobs
content files and settings, all player data.
Rollback (pair): remove ExcelTestMode-v1.jar
and the plugins/ExcelTestMode folder, and
restore the v31 jar + config per the v32
packet - the exact pre-window state (test mode
was never enabled on live).

## Boot verify

- Boot Done line present; SEVERE 0 attributable.
- "ExcelTestMode present but DISABLED
  (enabled: false in config.yml)" line present
  (the shipped state). NO "Test Mode ready"
  line is expected at this boot - if one
  appears, the placed config is not this
  packet's deploy-config; stop and check.
- "ExcelEMFixes v32 ready" with 28 modules
  (verified under the v32 packet).
- Guild state unchanged (OPEN 5 of 5 at last
  record; verify, do not assume).
Marker on install:
~/workspace/testmode-moveout-20261127/packet-testmode/INSTALLED.txt

## Same-pass surfaces (owed with the install)

See SURFACES.md in the v32 packet: the surfaces
land once for the pair (website row to v32,
GitHub release for v32 naming the move-out,
What's New both surfaces reflecting v32 +
ExcelTestMode v1), riding the D228 adjustment
drop this lane fired. Guide / tips / menus: no
change (operator tool, never player-facing).

## Proof owed after install

Iris-first per standing testing split (NOT a
gate - ship-it rule). On the live server, in a
verified-empty moment: set enabled: true in
plugins/ExcelTestMode/config.yml, restart (or
the window's own boot if scheduled inside it),
/testmode MikeExcel on, then as MikeExcel take
a staged boss hit that would be lethal (e.g.
the Manifested Nightmare at the Ice Village)
and verify (a) health lands exactly on 2.0 and
never below across repeated hits, (b) the
boss's combat episode behaves as stock, (c)
/testmode MikeExcel off at the end with the
list empty (leave enabled as the session lane
finds it; note the end state in the record).
Then run the pending EliteMobs 10.9.17
combat-end heal proof under the floor's
protection. Filed as a D228 follow-on drop by
this lane (retarget of
P-EMFIXES-V31-TESTMODE-LIVEPROOF-20261127).
