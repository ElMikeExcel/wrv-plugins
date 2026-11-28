# INSTALL PACKET - ExcelEMFixes v32 (test-mode move-out)

Lane: build (filed Nov 27, 2026). CHAINED WINDOW
1 of 2: this entry installs FIRST; ExcelTestMode
v1 (packet-testmode in the same lane folder)
installs SECOND in the same window.

## What ships

ExcelEMFixes v32 = the v31 tree with the
test-mode module REMOVED cleanly. The pack goes
29 -> 28 modules (the v30 set). No test-mode
code, no /testmode command registration, no
test-mode config section, no test-mode
permission node.

WHY (the ruling): Mike, Nov 27, 2026, 11:36 PM
CT - "Test mode probably shouldn't be part of
Excel em fixes I don't think." ExcelEMFixes is
the EliteMobs fix pack; test mode fixes nothing,
it is a fleet testing tool. The code moves out
to the standalone ExcelTestMode v1 plugin
(install 2 of 2 in this window) with identical
behavior. Nothing else changes: every other
module, command and config value is
byte-preserved from v31.

Live context: v31 was installed Nov 27, 2026,
11:22 PM CT (failsafe window
wrv-install-failsafe-20261127-2315). Its
test-mode module shipped OFF with an empty
player list and was NEVER enabled on live, so
this removal changes nothing in live play.

## Ordering

CHAINED with ExcelTestMode v1 in ONE window,
this entry first. The deploy-config composes
directly on the LIVE v31 config: it is the
live config minus the 68-line test-mode section
ONLY, and is byte-identical to the v30 live
config (see RECEIPTS.md).

## Live-premise receipts (G1)

Fresh pull, Nov 27, 2026 ~11:38 PM CT via the
bridge router (copies in this packet's
pull-g1/; see RECEIPTS.md):
- Live jar: plugins/ExcelEMFixes-v31.jar -
  716,847 B / SHA-256
  db06d7b9fcbd0f5fbcf98e75d90dfef6e2e28cfaf391ee78d65a88bf856d3073
  (byte-identical to the v31 packet's installed
  candidate).
- Live config: plugins/ExcelEMFixes/config.yml -
  108,601 B / SHA-256
  30812b3d0daf3c591ad45f7bad21036c0a81a2728dec1f40598c2656da73ff95
  (the v31 deploy-config, placed at the v31
  install; no drift).

## Candidate

- ExcelEMFixes-v32.jar - 708,298 B / SHA-256
  0a5815192ca179e255a7a4d62ffe08cf9236bde64972eb2f20874510aab98012
  (deterministic build: two consecutive builds
  byte-identical). Built on the v31 tree with
  the test-mode package, its registry line, its
  plugin.yml paragraph + permission node, its
  bundled-config section and its test suite
  removed; VERSION rolled v31 -> v32 (plugin.yml,
  VERSION constant, the two version-pin tests).
  Jar contents verified: zero testmode classes,
  plugin.yml reads version: v32, bundled
  config.yml parses to 28 module sections with
  no test-mode key.
- deploy-config.yml - 105,312 B / SHA-256
  ccf08517e8e59f5175f5368bc2cd9574f9962f4b39333e46b554b6771863874c
  = pulled live v31 config minus the test-mode
  section ONLY (68 lines removed, 0 other
  changes; byte-identical to the v30 live
  config; YAML parse: 28 module sections, live
  leash values kept). Do NOT place any
  build-root config on the live server.
- Tests: full plain-JVM suite 2,237 passed / 1
  failed - the 1 failure is the PRE-EXISTING
  ClassFormTable pin ("armor named first on the
  tie") that fails identically on the pristine
  v30 tree (2,237 / 1 there) and on v31 (2,270 /
  1, the +33 being the TestModeTest checks that
  moved out with the module; they run in the
  ExcelTestMode tree now, 33 / 0). Disclosed,
  not hidden; not introduced by v32.
- Rig proof (local Paper 26.3-256 + EliteMobs;
  logs in the lane folder's rig-logs/):
  solo boot with v32 + this deploy-config -
  "ExcelEMFixes v32 ready", 28 modules listed
  (test-mode NOT among them), Done (36.008s),
  SEVERE 0. Two further boots with ExcelTestMode
  v1 alongside are recorded in the ExcelTestMode
  packet.

## Live-jar gate (STOP on mismatch)

At the swap step, the outgoing jar is v31's:
716,847 B / SHA-256
db06d7b9fcbd0f5fbcf98e75d90dfef6e2e28cfaf391ee78d65a88bf856d3073
- STOP unless the on-disk jar matches at the
swap moment; record the pulled size + SHA-256
in RECEIPTS.md either way. Marker re-checks
before Shut Down and before Start per standing
discipline; BACKUP FIRST + cap-at-6; emptiness
list = 0/10 twice (weekend protocol if Mike
directs otherwise).

## Scope

Jar swap (ExcelEMFixes-v31.jar -> v32) +
full-config placement of deploy-config.yml
ONLY, then the ExcelTestMode v1 placement from
its own packet in the same window. Untouched:
every other plugin, all EliteMobs content files
and settings, all player data.
Rollback (pair): restore the outgoing v31 jar
(kept as .bak per house pattern) + the v31
config (30812b3d...; a copy sits in this
packet's pull-g1/), and remove
ExcelTestMode-v1.jar plus its data folder
(plugins/ExcelTestMode). Because test mode was
never enabled on live, rollback restores the
exact pre-window state.

## Boot verify

- Boot Done line present; SEVERE 0 attributable.
- "ExcelEMFixes v32 ready" line; 28 modules
  listed (test-mode NOT among them).
- No "Test Mode ready" line from ExcelEMFixes.
  (ExcelTestMode's own lines are verified under
  its packet: DISABLED line expected, since its
  deploy-config ships enabled: false.)
- Guild state unchanged (OPEN 5 of 5 at last
  record; verify, do not assume).
Marker on install:
~/workspace/testmode-moveout-20261127/packet-v32/INSTALLED.txt

## Same-pass surfaces (owed with the install)

See SURFACES.md in this packet. The v31
surfaces follow-on (P-EMFIXES-V31-SURFACES-
20261127) is superseded by the move-out: the
surfaces land as v32 + ExcelTestMode v1 (a D228
adjustment drop was fired by this lane).

## Proof owed after install

None for v32 itself (a pure removal; the rig
solo boot is its proof). The test-mode floor
proof retargets to the standalone ExcelTestMode
plugin - see its packet.
