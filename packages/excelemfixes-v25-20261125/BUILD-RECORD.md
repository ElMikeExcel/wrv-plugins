# BUILD RECORD — ExcelEMFixes v25, bedrock-abilities module (Wave 4 fold of ExcelEMAbilityBedrockBridge 0.2.0)

Built Nov 25, 2026 (late evening, CT) by the Wave 4 worker
lane, working inside
`~/workspace/excelemfixes-bedrock-abilities-20261125/` on top
of the canonical v24 tree
(`~/workspace/excelemfixes-partydrops-20261125/`, jar
626,284 B / 372c4103…). Version scheme: plain v-numbers
(D247); v24 was the previous release. Ruling: **D245**
(consolidation programme Wave 4). Creation reason carried
in README + config comments, verbatim in substance from the
programme: an EliteMobs platform gap — EM's only ACS
ability input route is the Java F-key gesture layer and
`fLayerSupported` hard-returns false for Bedrock, so
Bedrock players could not fire class abilities at all.
Built Oct 19, 2026 as the standalone bridge. Not installed
by this lane; packet STAGED, not filed (filing is a
separate supervisor leg per the brief); the standalone jar
is NOT removed (that happens in the install window only);
no panel contact; no messages sent.

## Artifact

- `packet-v25/ExcelEMFixes-v25.jar` — 657,130 B — sha256
  `7641a8acec174fffdfef07d6d5bd6bf9cbf49f28e73e41918b2ecbf725aaa676`
- Deterministic: two consecutive builds byte-identical.
- Version bumped in `ExcelEMFixes.VERSION`, `plugin.yml`,
  `build.sh`. ModuleRegistry registers
  `BedrockAbilitiesModule` last — 27 modules total.

## The EM compile-pin decision (EXPLICIT, as required)

The standalone refuses to build against an EliteMobs jar
whose MD5 is not pinned (0.2.0 pinned EM 10.9.14, MD5
`9e5f16298ac558e30197a1529b1ef83c` — re-verified today
against the rename-build folder's jar). **Decision (made
in PROGRAMME.md Wave 4, executed here): REPLACE the
compile-time pin with the pack's house posture — guarded
seams + a boot-time ACS probe — because a compile pin
inside a multi-module pack would block every other
module's build on any EliteMobs bump.** Execution:

- The module compiles against the pack's existing
  compile-time-only EliteMobs jar (10.9.10), like every
  other EM module in the pack. No MD5 check exists in the
  pack build for this module.
- Only `EmAcsGateway` and the isolated `AcsProbe` name
  EliteMobs types; every gateway call is wrapped in
  try/catch(Throwable).
- The failure-reason mapping is BY NAME
  (`BridgeFailure.valueOf(reason.name())`, UNKNOWN on any
  miss) instead of the standalone's exhaustive enum switch,
  so an EliteMobs that ADDS a reason degrades it to a plain
  UNKNOWN line instead of breaking a compiled switch.
- The boot-time probe stands the module's leg down with
  one console line if EM's ACS surface has moved (rig
  Phase B is this leg, against a stub EliteMobs with no
  ACS classes).
- Live context: live EliteMobs is **10.9.16** (its jar
  file is still named EliteMobs-10.9.10.jar — updated in
  place; G1 receipt in the packet's RECEIPTS.md). The
  bridge mapping suite was re-run against the actual live
  10.9.16 jar bytes: 26/26 mapping checks + 71/71 logic
  checks pass — the ACS surface and failure vocabulary are
  unchanged from 10.9.10/10.9.14 for everything the module
  touches. The module's first live boot under 10.9.16 is
  part of the install window's verification, and the
  post-install live Bedrock cast closes the wave.

## What was ported (0.2.0 source → `excelemfixes.bedrockabilities`, from source, never memory)

Source of truth: `~/workspace/abilitybridge-rename-build-20261114/`
(the 0.2.0 rename build; its source is the live 0.2.0's).

- Verbatim ports (package line + log-prefix only):
  `AcsGateway`, `AcsProbe`, `BridgeFailure`, `BridgeForms`
  (the reflection Cumulus/Floodgate/Geyser transport —
  presence-guarded, no compile-time Geyser dependency),
  `BridgeSlot`, `ClassView`, `FailureMessages`,
  `FireOutcome`, `FormModel`, `FormModelBuilder`,
  `Labels`, `SlotView`, `StatusCommand`.
- `EmAcsGateway` — verbatim except the log prefix and the
  mapReason change above (name-based mapping; the
  toEmSlot name-for-name mapping is unchanged and still
  test-pinned).
- Standalone main class + `AbilitiesCommand` →
  `BedrockAbilitiesModule` (FixModule) + a re-homed
  `AbilitiesCommand` (CommandExecutor): `/abilities` +
  `/classabilities` alias registered dynamically through
  the pack core; permission node
  `exceleitemobsabilitybedrockbridge.admin` declared in
  plugin.yml (kept from 0.2.0 so existing grants carry
  over) — command name/shape FROZEN per programme gate 8.
  The three fail-soft shapes are the standalone's, with
  one deliberate difference: where the standalone disabled
  its whole plugin (EM absent / ACS unreachable), the
  module stands down ONLY its own leg — no command is
  registered and the pack boots clean (the programme's
  boot-probe leg). The standby shape is unchanged:
  gateway + command enable, a 5 s watcher logs the bridge
  coming online when ACS initializes.
- The form route is the standalone's Cumulus form, shape
  frozen: the pack's Views/MenuRenderer law (D225) governs
  menu-definition modules; this module's Bedrock surface
  IS the frozen form (title, content, button order all
  unit-pinned and rig-proven identical), and its Java
  surface is the standalone's one-line F-key pointer, so
  there is no menu definition to re-express. Noted here so
  the choice is on the record, not silent.
- Config: `modules.bedrock-abilities` section (enabled /
  debug-log-fires) with the reason comments; shipped
  default debug-log-fires false = the live standalone
  value (diffed at the gate — RECEIPTS.md).
- plugin.yml: floodgate added as a SOFTDEPEND only
  (Geyser-Spigot was already there); no hard depend added.

## Tests

- Full pack suite: **2,083 passed / 0 failed** on the
  clean run (v24 baseline 2,075 + 8 new guard checks:
  no hard depend, floodgate softdepend-only, frozen
  permission node, config section + default, module never
  disables the pack).
- Bridge suites (the standalone's three mains, ported):
  Run A (no EM jar at runtime) 71 + 24 checks; Run B (EM
  10.9.10 jar) 71 + 26 + 24 checks — all green on the
  clean run. Two test adaptations, both recorded in the
  test sources: the status suite's plugin.yml gate now
  reads the pack layout (src/plugin.yml, pack version,
  and "no abilities command declared in plugin.yml"
  replacing "no command-level permission", since module
  commands are dynamic and permission-free by design).
- Live-EM re-run: BridgeTestMain 71/71 +
  BridgeEmMappingTestMain 26/26 against the pulled live
  EM 10.9.16 jar.
- Known flake: SkillNudgeMathTest "armor named first on
  the tie" depends on Map.of iteration order (salted per
  JVM launch) — it failed in some runs of BOTH the
  pristine v24 tree and this tree and passed in the clean
  runs; pre-existing, documented in the v24 record, in a
  module this fold does not touch.

## Rig verdicts (Paper 26.2 + EM 10.9.14; BaModuleRig driver in `rig-driver/`, logs in `rig/`)

- **Phase A** (module on): standby line at enable, bridge
  online ~20 s after ACS post-init; console
  `/abilities status` answered in the frozen shape
  (EliteMobs link UP, ACS READY); fake player set up as
  Paladin through EM's own test surfaces; production
  gateway + form builder composed [Divine Steed
  (Mobility), Provoke (Signature), Repulse (Utility),
  Close] — identical to the standalone 0.1.0 rig proof;
  all three slots fired through the module's production
  `fireAbility` path, EM's ability-evidence ledger growing
  2 -> 3 -> 4 -> 5; SEVERE 0.
- **Phase B** (stub EliteMobs, no ACS classes — the
  boot-probe leg): one stand-down line, /abilities never
  registered, rest of pack enabled clean; SEVERE 0.
- **Phase C** (no EliteMobs): one stand-down line, pack
  clean; SEVERE 0.
- **Phase D** (module toggled off, real EM):
  bedrock-abilities absent from the ready list, all other
  modules enabled; SEVERE 0. Toggle is real.
- Standalone proofs reused, not re-proven: the 0.1.0/0.2.0
  boots A–D (real fires incl. Provoke TAUNT evidence,
  fail-soft shapes, EM 10.9.10 cross-version fire) in
  `~/workspace/excelabilitybridge/BUILD.md` + rig-logs.

## Live-premise receipts (G1, fresh Homebase SSH pull Nov 25, 2026 ~11:17 PM CT)

Full detail in `packet-v25/RECEIPTS.md`:

- Live ExcelEMFixes-v24.jar: 626,284 B / `372c4103…`
  (byte-identical to the v24 candidate).
- Live ExcelEMFixes config.yml: 91,671 B / `5a4a9468…`
  (byte-identical to the v24 deploy config). Deploy
  config for v25: live pull + only the bedrock-abilities
  section (+56 / −0 lines), 94,405 B / `a3490370…`.
- Live standalone jar: **`ExcelEMAbilityBedrockBridge-0.2.0.jar`**
  (the LIVE filename — not the records-build name),
  31,990 B / `9c415821…`; both live data folders' configs
  byte-identical, debug-log-fires: false = shipped default.
- Live EliteMobs: file `EliteMobs-10.9.10.jar`,
  14,573,497 B / `e38dad00…`, plugin.yml version **10.9.16**.

## D246 gates

- Toggleable + config-driven: yes; module-off rig-proven
  (Phase D).
- Shipped defaults vs WRV values: identical on the only
  knob (debug-log-fires false) — stated in README +
  config comments.
- Hardcoding sweep: module package grep for WRV literals
  hits only the D246 documentation comments and the frozen
  permission/status strings carried from the standalone.
  No WRV literal in code logic or shipped defaults; no
  local override needed (no WRV-specific data exists for
  this module).
- Gate 7 (self-contained): plugin.yml gains NO hard
  depend (guard-tested); EliteMobs via the pack's existing
  softdepend + guarded gateway/probe; Geyser/Floodgate
  reflection-only presence-guarded seams (floodgate
  softdepend added) — absent, the form leg stays inert,
  the chat fallback still lists abilities, the pack boots
  clean. EliteMobs remains the pack's only required
  companion.

## Packet (STAGED — filing is a separate supervisor leg)

- Packet folder: `packet-v25/` — INSTALL-PACKET.md
  (**Lint: 4689 B / max line 104 / non-ASCII 63**),
  RECEIPTS.md, SURFACES.md, SHA256SUMS (verified OK ×5),
  jar, deploy-config.yml.
- Packet directs the window to remove the standalone
  `ExcelEMAbilityBedrockBridge-0.2.0.jar` in the SAME
  window, only after module proof, kept in the backup set
  as rollback. The standalone jar was NOT removed by this
  lane.
- **NOT filed into MANIFEST**, per brief. Filing is
  follow-on #1 below; the packet's gate section requires
  a fresh re-pull at filing.

## Surfaces staging (SURFACES.md in the packet folder)

- Big-release verdict (gate 11): NOT big — v25 ships plain.
- In-game What's New entry text: STAGED.
- Website What's New + plugin row v24→v25 + standalone row
  marked folded: STAGED TEXT.
- Records draft PR: OWED — close-out leg after install.
- Guide: hibernated — verify-no-change noted (no behavior,
  command, or rate change).
- Tips + ExcelMenu/ExcelBedrockMenu: checked, no change owed.

## Finish-and-fire drops (D228)

Filed 4 drops to `~/hooks/state/followon-drops/`:
1. File the staged v25 packet into MANIFEST (pointer stub
   + FIRE) with a fresh G1 re-pull at filing.
2. Same-pass surfaces publication after the v25 install
   (What's New ×2 + website rows, per SURFACES.md).
3. wrv-plugins records draft PR at v25 install close-out.
4. Post-install live Bedrock ACS cast via the G1 harness
   (the wave's closing proof), then the upstream note to
   MagmaGuy naming ExcelEMFixes v25.
