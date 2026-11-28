# RECEIPTS - ExcelTestMode v1 (standalone)

## Live-premise receipts (G1)

Fresh pulls, Nov 27, 2026 ~11:38 PM CT via the
bridge router (transports up; copies in this
packet's pull-g1/):

- Live jar: plugins/ExcelEMFixes-v31.jar -
  716,847 B / SHA-256
  db06d7b9fcbd0f5fbcf98e75d90dfef6e2e28cfaf391ee78d65a88bf856d3073
  (the chained window's premise).
- Live config: plugins/ExcelEMFixes/config.yml -
  108,601 B / SHA-256
  30812b3d0daf3c591ad45f7bad21036c0a81a2728dec1f40598c2656da73ff95
  (the v31 deploy-config; test-mode section
  present but enabled: false, players: [] -
  never enabled on live).
- Live plugins census (same session, directory
  listing via the router): NO ExcelTestMode jar
  and NO ExcelTestMode folder on live. The only
  ExcelEMFixes jar is ExcelEMFixes-v31.jar
  (716,847 B).

## Candidate verification

- ExcelTestMode-v1.jar - 9,825 B / SHA-256
  8d290a32b3c6e326e1561cd84d00fc6406f0dd9d29b78775b307a4a800d9b6a
  Deterministic build: two consecutive builds
  byte-identical (build.sh in the lane folder's
  build-testmode). Jar contents: plugin.yml,
  config.yml, exceltestmode/ExcelTestMode.class,
  exceltestmode/TestModeMath.class - nothing
  else. plugin.yml parses (YAML verified):
  name ExcelTestMode, version v1, main
  exceltestmode.ExcelTestMode, commands:
  [testmode], permissions: [exceltestmode.admin]
  (default op), NO depend / softdepend entries.
- deploy-config.yml - 3,557 B / SHA-256
  795a8b7cee3b665f6ec91a18c1bda217e9857c25d119f20e26af35f454ccb18a
  Byte-identical to the config bundled in the
  jar. YAML parse: enabled false, floor-hp 2.0,
  players [], excluded-causes [VOID, SUICIDE],
  debug false.
- Tests: run-tests.sh on the standalone tree -
  33 passed / 0 failed (the TestModeTest suite
  ported unchanged from the ExcelEMFixes v31
  tree; same checks, same numbers).
- Rig boots (local Paper 26.3-256 + EliteMobs +
  ExcelEMFixes v32; logs in the lane folder's
  rig-logs/):
  boot B (shipped defaults) - ExcelTestMode v1
  in the plugin list, "ExcelTestMode present
  but DISABLED (enabled: false in config.yml)
  - nobody is protected..." line, Done
  (30.201s), SEVERE 0 (boot-testmode-off.log);
  boot C (enabled: true in the rig copy only) -
  "Test Mode ready - listed players take full
  damage but cannot drop below 2.0 HP
  (0 player(s) listed). Toggle with /testmode
  <player> on|off.", console /testmode status
  answered ("floor 2.0 HP, 0 player(s)
  listed"), /testmode RigTester on produced
  the TEST-MODE ON log line (player=RigTester
  uuid=faae3c7f-47f7-3c02-9464-bafd52a60870
  floor=2.0 HP, ISO timestamp, by=CONSOLE) and
  the status then showed 1 player listed; the
  per-player read answered "RigTester is ON
  (floor 2.0 HP)"; /testmode RigTester off
  produced the TEST-MODE OFF line and the
  config write-through left players: [] at
  shutdown, Done (34.416s), SEVERE 0
  (boot-testmode-on.log).
  A behavioral floor proof needs a joined
  player, which the rig harness does not
  provide; it is owed on the live server after
  install (see the packet's Proof owed).
