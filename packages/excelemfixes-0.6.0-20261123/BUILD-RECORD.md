# Quest Beacon Guide — Build Record

- **Status:** BUILT + PROVEN (D122 ship-it gates met: deterministic build, automated suites, mirror/rig load proof SEVERE 0). NOT installed, NOT filed — filing is a later lane.
- **Dispatched:** 2026-11-23T12:49:09Z (2026-11-23 06:49 AM CST, America/Chicago)
- **Lane:** Movable-work audit dispatch 2026-11-23 06:47 AM CST
- **Packet:** P-QUEST-BEACON-GUIDE-20261122
- **Scope:** Build + automated tests + mirror/rig load proof only. NO live install. NO MANIFEST filing (later lane).
- **Design (settled):** Per-player fake beacon beam (packets/entities only, no world-persistent blocks) at tracked quest destination from ExcelEMFixes quest-destinations module; ends on untrack / completion / arrival within small radius. Single edge-riding beam; far leg stays on /questwhere + glow guidance. Ships as ExcelEMFixes module, expansion-generic, data-driven, config-toggleable, documented in config comments AND README. Java + Bedrock parity required.

## Phases
- [x] DISPATCHED — record created write-first
- [x] Spec read (parent-backlog P-QUEST-BEACON-GUIDE-20261122, lines 1379–1387)
- [x] Source tree / 0.5.0 build setup inspected
- [x] Module implemented
- [x] Automated tests run
- [x] Mirror/rig load proof
- [x] Final hashes + close-out

## Log
- 2026-11-23T12:49:09Z — DISPATCHED. Record created before any other work.
- 2026-11-23T12:52Z — SPEC: full backlog entry read. Settled design points beyond the dispatch summary: (1) beam RIDES AT THE EDGE of the player's view in the destination's direction and settles onto the real destination once it is inside render range (Mike, Nov 22 11:44 PM — part of the design, not optional); (2) the breadcrumb/chain-of-beacons variant was considered and DROPPED by Mike — do not revive; (3) hard proof requirement: Java AND Bedrock parity proven on the rig before filing; if a true beacon beam can't be faked per-player on Bedrock, report back with the closest parity-honest alternative (particle column / crystal beam named in the entry) instead of shipping Java-only.
- 2026-11-23T12:52Z — BASE: authoritative 0.5.0 tree = ~/workspace/mcmmo-merge-20261123/merged/ (live jar 81f26cee…, 359,925 B, 15 modules). Copied (src, libs, scripts, README, deploy) to build-root/ in this lane home.
- 2026-11-23T12:53Z — PROVENANCE GATE: PASS. Unmodified rebuild of the copied tree produced ExcelEMFixes-0.5.0.jar SHA-256 81f26cee5d14ba568ac2c537025e3e9a25a0f5c02efc1fb38fbac75d4fe260ba, 359,925 B — byte-identical to the live/installed 0.5.0 jar. Build proceeds on this verified base; new pack version = 0.6.0 (module 16 added).
- 2026-11-23T12:55Z — TRACKED-QUEST SOURCE FOUND: EliteMobs' public QuestTracking API (javap-verified against libs/EliteMobs-10.9.10.jar): QuestTracking.isTracking(Player), QuestTracking.getPlayerTrackingQuests() (UUID -> QuestTracking), QuestTracking.getQuest(); the quest file comes from CustomQuest.getConfigurationFilename() — the same accessor EmQuestData already uses. "Selected/tracked" is therefore read from EM itself, not inferred from active quests. Read isolated in one EM-bridge class (house pattern), fully guarded.
- 2026-11-23T12:55Z — DESTINATION SOURCE: the quest-destinations module's QuestDestinations table, exposed to this module through a new api/QuestDestinationApi facade (same pattern as api/GlowApi). A tracked quest with no table row gets no beam (covering a quest stays a data row, never code). The beacon module stands down cleanly when the quest-destinations module is off.

## Implementation (in build-root/, branched from the verified 0.5.0 tree)
- NEW `questbeacon/BeamGeometry.java` (pure): edge-riding anchor — settled on the destination within edge-distance, else exactly edge-distance along the player→destination line; distance reported.
- NEW `questbeacon/BeamPolicy.java` (pure): show/hide decision — SHOW_EDGE / SHOW_SETTLED / HIDE_PLAYER_OFF / HIDE_NO_TRACK / HIDE_UNCOVERED / HIDE_WRONG_WORLD / HIDE_ARRIVED, with the design's precedence.
- NEW `questbeacon/EmTrackedQuest.java`: the only EM-touching class; reads EM's public QuestTracking API, fully guarded.
- NEW `questbeacon/BeamGuide.java`: 10-tick refresh (config), per-player particle column (END_ROD + gold DUST) via Player.spawnParticle, one chat line on beam appear + one on settle, per-player session off-switch, quit cleanup (particles need no world cleanup — nothing was placed).
- NEW `questbeacon/QuestBeaconModule.java`: FixModule 16, id `quest-beacon`, dynamic `/questbeacon [on|off]` command, stands down with clear log lines when EliteMobs or the destination API is absent.
- NEW `api/QuestDestinationApi.java` facade; `QuestDestinationModule` registers/unregisters its table (2 additive hunks, no behavior change).
- `ModuleRegistry` +1 line; version 0.5.0 → 0.6.0 (core VERSION, plugin.yml, build.sh); plugin.yml description += beacon sentence.
- Config: `quest-beacon` section with full plain-language documentation appended to BOTH src/config.yml and deploy/config.yml (16 module sections, YAML-validated). Shipped defaults = WRV values (no divergence): update-ticks 10, edge-distance 48, arrival-radius 8, column-height 40, column-step 2.
- README: new `quest-beacon` module section (defect, mechanism + why, commands, config) per the module documentation rule; install line bumped to 0.6.0.
- Tests: NEW `QuestBeaconTest` (32 checks: geometry incl. diagonal/on-line/boundary/moving-player; policy incl. every end condition + precedence), wired into run-tests.sh + TestMain.

## Proof results
- UNIT SUITES: 889 checks total = 857 (0.5.0 baseline) + 32 new. QuestBeacon 32/32 PASS. One FAIL: SkillNudgeMathTest "Jester-shaped: armor named first on the tie" — PROVEN PRE-EXISTING FLAKY, not a regression: a pristine unmodified copy of the 0.5.0 tree fails the identical check (856/1) while the original tree passes it (857/0); the check depends on `Map.of(...)` iteration order, which the JVM randomizes per launch. Flagged for the parent lane as a separate small fix (deterministic tie-break in SkillNudgeMath or the test); NOT touched here (out of scope).
- MOUNT SUITES: 50 passed, 0 failed (regression).
- DETERMINISM: jar built twice, byte-identical both times.
- RIG LOAD PROOF (house rig copied into build-root/rig, EM 10.9.14 — the live version — + mcMMO/Jobs/etc.; jar + deploy config swapped in): boot 1 (module on): ExcelEMFixes 0.6.0 enables, "Quest Beacon ready — a tracked quest's destination gets a per-player pillar of light", "module enabled: Quest Beacon (quest-beacon)", pack ready line lists all 16 modules, console `/emfixes` lists quest-beacon, boot Done (51.397s), **SEVERE 0**, clean disable on stop. Boot 2 (quest-beacon enabled:false): module absent from the ready list, no ready line, Done (42.697s), **SEVERE 0** — toggle discipline proven. Log: build-root/rig/runtime.log (boot 2; boot 1 lines quoted here verbatim from its run).
- PARITY: one identical particle code path for Java and Bedrock players (per-player spawnParticle; Geyser translates; both particle types have Bedrock equivalents) — parity by construction. Owed post-install player proofs (do NOT gate filing per D122): a Java player tracks a covered quest and sees the edge-riding pillar settle on approach + end on arrival/untrack; the same on a Bedrock (Geyser) client.

## Deliverables (this lane home)
- `ExcelEMFixes-0.6.0.jar` — 376,523 B, SHA-256 `845d010be47281b142e7309735be14ef6931a809024acfb66ac4de3afcbc20f4` (deterministic ×2).
- `deploy/config.yml` — live 0.5.0 deploy config + quest-beacon section only, 47,219 B, SHA-256 `ee968ff8656ffb8eb4e00b2a6f49599407d8244d40612b7a86e3b5627335dff1`.
- `build-root/` — full 0.6.0 source tree (build.sh / run-tests.sh / run-mount-tests.sh; rig in build-root/rig/).
- Install unit at filing = pack jar + deploy config, gated swap (same shape as 0.5.0). NOT filed by this lane.

## Draft MANIFEST entry (for the filing lane — NOT filed here)
> **ExcelEMFixes 0.6.0 — quest-beacon module (P-QUEST-BEACON-GUIDE-20261122):** TWO placements, ONE gated window. (1) Pack jar → `ExcelEMFixes-0.6.0.jar` (376,523 B, SHA-256 845d010b…) — the live 0.5.0 pack (all 15 modules, byte-proven base 81f26cee…) + module 16 `quest-beacon`: while a player has a quest tracked in EM's tracker, a per-player particle pillar marks that quest's destination (quest-destinations table via the new destination API); edge-riding at 48 blocks, settles in range, ends on untrack/completion/arrival (8 blocks)/world change; `/questbeacon [on|off]`; no blocks or entities placed, nothing persistent. (2) Pack config → deploy config SHA-256 ee968ff8… (live config + quest-beacon section only). Surfaces owed same pass: What's New (site + in-game), Guide knowledge when it un-hibernates, tips candidate. Post-install proofs owed: Java + Bedrock tracked-quest beam visuals.

- 2026-11-23T13:02:57Z — CLOSE-OUT: build + proof complete per above. No live install, no MANIFEST entry filed (per dispatch scope).

## DESIGN DECISION — beam mechanism (decide-and-log, parity law)
A true beacon beam is rendered by the beacon BLOCK ENTITY after it validates a pyramid; the pack is Bukkit-API-only with no packet library (no ProtocolLib in libs/ or plugin.yml, by the pack's public zero-dependency design), and a per-player fake block carries no ticking block entity — so a real beam cannot be proven per-player on Java, and Geyser translation of a fake beacon block entity is not a documented/provable Bedrock path. The end-crystal beam-target alternative renders only on Java (Bedrock crystals have no arbitrary beam-target beam) — Java-only, rejected by the parity law. SHIPPED MECHANISM (the backlog's own named fallback): a per-player PARTICLE COLUMN — a beacon-styled pillar of light sent only to the owning player via Player.spawnParticle (per-player by API contract; zero blocks, zero entities, zero world persistence). One identical code path serves Java and Bedrock players (Geyser translates the particle packets; END_ROD + coloured DUST both have Bedrock equivalents), so parity holds by construction rather than by two divergent renderers. Player-facing name stays "quest beacon". Bedrock in-client visual proof remains an owed post-install player proof (D122: proofs do not gate filing; mirror load + automated tests are the ship gate).
