# RECEIPTS - ExcelEMFixes v32 (test-mode move-out)

## Live-premise receipts (G1)

Fresh pulls, Nov 27, 2026 ~11:38 PM CT via the
bridge router (transports up - chisel /
tailscale-ssh / panel all answering; copies in
this packet's pull-g1/):

- Live jar: plugins/ExcelEMFixes-v31.jar -
  716,847 B / SHA-256
  db06d7b9fcbd0f5fbcf98e75d90dfef6e2e28cfaf391ee78d65a88bf856d3073
  (byte-identical to the v31 packet's installed
  candidate - the v31 jar is the live jar).
- Live config: plugins/ExcelEMFixes/config.yml -
  108,601 B / SHA-256
  30812b3d0daf3c591ad45f7bad21036c0a81a2728dec1f40598c2656da73ff95
  (the v31 deploy-config placed at the Nov 27,
  11:22 PM install; no drift).
- Live plugins census (same pull session): the
  only ExcelEMFixes jar on live is
  ExcelEMFixes-v31.jar; no ExcelTestMode jar or
  folder exists on live yet.

## Candidate verification

- ExcelEMFixes-v32.jar - 708,298 B / SHA-256
  0a5815192ca179e255a7a4d62ffe08cf9236bde64972eb2f20874510aab98012
  Deterministic build: two consecutive builds
  byte-identical (build.sh in the lane folder's
  build-v32). Jar listing verified: zero
  testmode classes; bundled plugin.yml reads
  version: v32 with no testmode text; bundled
  config.yml has no test-mode section.
- deploy-config.yml - 105,312 B / SHA-256
  ccf08517e8e59f5175f5368bc2cd9574f9962f4b39333e46b554b6771863874c
  Composition proof: the G1 live pull minus the
  68-line test-mode section is BYTE-IDENTICAL
  to the v30 live config (105,312 B /
  ccf08517...) - 68 lines removed, 0 added, 0
  changed. YAML parse: 28 module sections;
  modules.leash keeps its live values.
- Tests: run-tests.sh on the v32 tree - 2,237
  passed / 1 failed; the failure is the
  pre-existing ClassFormTable pin, identical on
  the pristine v30 tree (2,237 / 1) and on v31
  (2,270 / 1). Delta attributable to v32: the
  33 TestModeTest checks moved out with the
  module (they pass 33 / 0 in the ExcelTestMode
  tree).
- Rig boots (local Paper 26.3-256 + EliteMobs,
  logs in the lane folder's rig-logs/):
  boot A (v32 solo, this deploy-config) -
  "ExcelEMFixes v32 ready", 28 modules, Done
  36.008s, SEVERE 0 (boot-v32-solo.log);
  boots B/C with ExcelTestMode v1 alongside are
  recorded in the ExcelTestMode packet's
  RECEIPTS.md.
