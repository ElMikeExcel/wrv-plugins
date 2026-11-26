# RECEIPTS — ExcelEMFixes v25 (bedrock-abilities)

All live receipts pulled fresh via the Homebase SSH route
(`ssh -F ~/.ssh/config mike-pc` + scp) on Nov 25, 2026,
~11:17-11:19 PM CT. Pull folder:
`~/workspace/excelemfixes-bedrock-abilities-20261125/live-pull/`

## Live-premise receipts (G1)

- Live pack jar `plugins/ExcelEMFixes-v24.jar` — 626,284 B —
  sha256 `372c41031c8336a78d1113a1e9778c0fbcadd3afbb15c1840e16c56b0e042df9`
  (byte-identical to the v24 candidate in its build record).
- Live pack config `plugins/ExcelEMFixes/config.yml` —
  91,671 B — sha256
  `5a4a9468808a540409f1fee66ced8aab78c36acebeae5d25abd009ad59b63a01`
  (byte-identical to the v24 deploy config).
- Live standalone jar `plugins/ExcelEMAbilityBedrockBridge-0.2.0.jar`
  — 31,990 B — sha256
  `9c4158212c7443f49be5371784aab229855381cc1f8ba46359a7637b7a32a4c1`.
  Its plugin.yml: name `ExcelEMAbilityBedrockBridge`,
  version 0.2.0, main
  `com.winkreyes.exceleitemobsabilitybedrockbridge.ExcelEliteMobsAbilityBedrockBridge`,
  softdepend [EliteMobs, Geyser-Spigot, floodgate]. NOTE the
  live filename: the window removes THIS name, not the
  records-build name (ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar).
- Live standalone configs: BOTH data folders exist live —
  `plugins/ExcelEMAbilityBedrockBridge/config.yml` and
  `plugins/ExcelEliteMobsAbilityBedrockBridge/config.yml` —
  each 297 B, sha256
  `a99d7fbe87fdc0f20979d38b6213ef784d357f74c45a84d912637ffa3d3a78e1`,
  byte-identical, content: `debug-log-fires: false` (the
  standalone's only key). Diffed against the staged module
  config: the module's shipped default is also false —
  identical behavior.
- Live EliteMobs jar `plugins/EliteMobs-10.9.10.jar` —
  14,573,497 B — sha256
  `e38dad008037184e11e5b51dd6b60651419e9c1b29297f3372ec23d54e58705b`.
  CAUTION: the FILENAME still says 10.9.10 but the jar's own
  plugin.yml reads version **10.9.16** — EliteMobs was
  updated in place under the old filename. The v25 gate is
  on the sha256 above, never the filename.

## Candidate

- `packet-v25/ExcelEMFixes-v25.jar` — 657,130 B — sha256
  `7641a8acec174fffdfef07d6d5bd6bf9cbf49f28e73e41918b2ecbf725aaa676`.
  Deterministic: two consecutive builds byte-identical.
- `packet-v25/deploy-config.yml` — 94,405 B — sha256
  `a34903709ba2dfb3a882a01ba17356b2bbc94b09d80411e19fbf673597e9e13e`
  = the live config above + ONLY the bedrock-abilities
  section (+56 / -0 lines, diffed).

## Tests (clean run, `test-run.log` in the build folder)

- Pack suite: 2,083 passed / 0 failed (v24 baseline 2,075 +
  8 new bedrock-abilities guard checks). Known pre-existing
  flake: SkillNudgeMathTest "armor named first on the tie"
  depends on Map.of iteration order (salted per JVM launch);
  it failed in some runs of BOTH the pristine v24 tree and
  this tree, and passed in the clean run recorded here. It
  is in a module this fold does not touch (documented in
  the v24 build record as well).
- Bridge suites ported from the standalone 0.2.0, run
  against the pack's compile-time-only EM 10.9.10 jar:
  Run A (no EM jar at runtime): BridgeTestMain 71/71,
  BridgeStatusTestMain 24/24. Run B (EM jar present):
  BridgeTestMain 71/71, BridgeEmMappingTestMain 26/26,
  BridgeStatusTestMain 24/24.
- Same bridge suites re-run against the LIVE EliteMobs jar
  pulled above (EM 10.9.16 bytes): BridgeTestMain 71/71,
  BridgeEmMappingTestMain 26/26 — the ACS surface and the
  failure-reason vocabulary the module maps are unchanged
  in 10.9.16.

## Rig verdicts (Paper 26.2 + EliteMobs 10.9.14 — the
## standalone 0.2.0's pinned version; driver in `rig-driver/`,
## logs in `rig/logs/`, runner `rig/run-ba-rig.sh`)

- Phase A (module on, real EM): standby line at enable,
  "ACS is up - Bedrock ability bridge online" ~20 s later;
  27 modules registered (26 enabled under the rig config —
  party-drops was off in that config); console
  `/abilities status` answered with the frozen status shape
  ("ExcelEliteMobsAbilityBedrockBridge status: enabled
  (v25)" / "EliteMobs link: UP - EliteMobs 10.9.14 enabled,
  Advanced Combat System READY"); NMS fake player given a
  Paladin class through EM's own test surfaces (skill setAll
  + admin set-level + select); the production gateway +
  form builder composed buttons [Divine Steed (Mobility),
  Provoke (Signature), Repulse (Utility), Close] — identical
  to the standalone 0.1.0 rig proof; all three slots fired
  through the module's production fireAbility path with
  EM's own ability-evidence ledger growing 2 -> 3 -> 4 -> 5;
  SEVERE 0.
- Phase B (stub "EliteMobs" plugin with no ACS classes —
  the boot-probe leg): one line "EliteMobs class system
  (ACS) could not be reached (an EliteMobs update may have
  moved it) - Bedrock ability bridge standing down
  (bedrock-abilities inert)"; /abilities never registered;
  the rest of the pack enabled clean; SEVERE 0.
- Phase C (no EliteMobs): one line "EliteMobs not found or
  not enabled - Bedrock ability bridge standing down
  (bedrock-abilities inert)"; pack clean; SEVERE 0.
- Phase D (modules.bedrock-abilities.enabled=false, real
  EM): bedrock-abilities absent from the ready list, all
  other modules enabled, SEVERE 0. Toggle is real.

## D246 gates

- Toggleable + config-driven: yes (enabled +
  debug-log-fires); module-off rig-proven (Phase D).
- Shipped defaults vs WRV values: the module's only knob,
  debug-log-fires, ships false; WRV's live standalone
  config is false — identical (stated in README + config
  comments).
- Hardcoding sweep: `grep -rn "Wink|WRV|winkreyes|Jester|Marvin|duckdns|webhook"`
  over the module package → hits ONLY in documentation
  comments the D246 gate itself requires (the WRV-values
  notes) and the frozen permission node / status strings
  carried from the standalone. No WRV literal in code logic
  or shipped defaults. No WRV-specific data exists for this
  module → no local override needed.
- Gate 7 (self-contained): plugin.yml gains NO hard depend
  (guard-tested). Cross-system legs: EliteMobs ACS via the
  pack's existing softdepend + guarded gateway/probe;
  Geyser/Floodgate via reflection-only presence-guarded
  seams (floodgate added as softdepend only) — absent,
  the form leg stays inert, the chat fallback still works,
  and the pack boots clean (Phases B/C prove the EM side;
  the Geyser-absent shape is the standalone's proven
  fail-soft plus the unit suites' form tests).

## Compile-pin decision receipt

The standalone 0.2.0 build.sh refused to build against an
EM jar whose MD5 was not `9e5f16298ac558e30197a1529b1ef83c`
(EM 10.9.14; that jar, pulled from the rename-build folder,
still hashes to exactly that MD5 today). Per PROGRAMME.md
Wave 4, the module REPLACES the pin with guarded seams +
the boot-time ACS probe (Phase B is the probe leg), because
a compile pin inside a multi-module pack would block every
other module's build on an EM bump. The module compiles
against the pack's existing compile-time-only EM 10.9.10
jar; the failure-reason mapping is by name, so EM 10.9.16's
vocabulary (verified identical above) and any future added
reason degrade safely. Full reasoning in the module's class
comment and the build record.
