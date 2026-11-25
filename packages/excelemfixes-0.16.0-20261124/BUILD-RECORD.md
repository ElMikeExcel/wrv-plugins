# BUILD RECORD — ExcelEMFixes 0.16.0, quest-chain-clarity module

**Lane:** em-quest-chain-clarity-20261124 · **Built:** Nov 24, 2026 · **Status: BUILT + RIG-PROVEN + FILED (PENDING in the MANIFEST). NOT installed.**

## The commission

Mike, Nov 24, 2026, 4:16 PM CT: when a player finds a chain-locked quest NPC, "it should let the player know what they need to do or what quests they need to do first before this one unlocks." Specimen the same afternoon: Jester clicked Alanus Secundas at the Pontis Groves Lighthouse and got only EliteMobs' generic red line ("This NPC has some quest(s) you can't accept yet! Finish the quests that come before them first."). The real chain, traced from the live quest files: Feeding the Sick ← Population Control! ← The Groves Call! ← the main quest line (King Nemian / Bromia Curio in the Groves).

## What was built

Module #22 in ExcelEMFixes, **quest-chain-clarity** (default ON, toggleable like every module; README + config carry the REASON per the module workflow). On a click that leaves a quest chain-locked, the player gets one extra line per locked chain after EliteMobs' own message:

`Locked: finish 'Population Control!' first — King Nemian gives it.`

- **Base stated plainly: ExcelEMFixes 0.15.0** (the skills-menu tree, copied at build start). Version = base + 0.1.0 = **0.16.0**. The diff against 0.15.0 is only: the new `questchain` package (3 classes), ModuleRegistry/VERSION/plugin.yml/build.sh wiring, the appended config section in both configs, the README section, the new test class + registration. Deploy config = the 0.15.0 packet config + exactly the appended 2,452-byte module section (strict-prefix verified).
- **Walk semantics** (`QuestChainCore`, pure logic, unit-tested): for the clicked NPC, its quests = giver list ∪ turn-in quests ∪ objective-filename quests from EM's registries. Carried quests are skipped. For each remaining quest whose accept permission the player lacks, follow accept-permission links (permission → the quest whose lockout permission it is) until reaching a quest whose own accept permissions are all held or which has none — that quest is the named next step. Steps dedupe by terminal quest and cap at `max-lines` (3). Cycles terminate silently; a permission no loaded quest grants yields no line (the chain cannot be named truthfully).
- **Giver/place:** the next quest's giver = an NPC listing it (first by sorted NPC filename), else its turn-in NPC's name, else the NPC name its objective carries; the place = the location text of that quest's own objective, when present. Clauses omit cleanly.
- **Player progress** is read exactly as EliteMobs' own `CustomQuest.hasPermissionForQuest` reads it (bytecode-verified against the live jar, internally 10.9.14): Bukkit metadata markers named by each quest's lockout permission — set by EM's `PlayerQuestCooldowns.addCooldown` on completion (which also grants a PermissionAttachment; the accept check reads the metadata) and re-applied at login. No LuckPerms, no file parsing at runtime: everything comes from EM's loaded registries, re-indexed every 30 s.
- **Accept permissions:** EM's content files (Primis included) carry the SINGULAR `questAcceptPermission` key; the plural list getter reads back null for them (rig-probe proven). The snapshot reads singular first, then list entries — mirroring EM's own check, which consults both.

## Detection route (stated per the task)

EliteMobs' api/ package has QuestAcceptEvent, QuestCompleteEvent, QuestLeaveEvent, QuestProgressionEvent, QuestObjectivesCompletedEvent, QuestRewardEvent — **no quest-refusal event exists**; the refusal is only a chat message inside `QuestInteractionHandler`. So the module listens to `PlayerInteractEntityEvent` (main hand, MONITOR, observe-only), identifies EM NPCs the way quest-giver-glow does (EM's NPCEntitySpawnEvent/RemoveEvent + a reflective harvest of EM's PersistentObjectHandler registry), and replicates only the accept-permission segment of EM's check. It never alters the event, the click, or EM's message.

**The one real defect, caught by the rig before filing:** EliteMobs' `NPCInteractions.interactWithNPC` calls `setCancelled(true)` on this event for its NPCs (bytecode-verified, three call sites — it is how EM suppresses the vanilla villager trade screen). The first build listened with `ignoreCancelled = true`, so the handler never ran: three rig boots produced EM's generic line and module silence while the index, the marks, and the identity were all verified good. Removing the flag (the module only reads and messages; it never un-cancels) fixed it, and cases A–D below are the re-proof. The finding is documented in the module's class comment so it is never reintroduced. Two debug-gated log lines were added during diagnosis and kept (identity miss; no-steps summary).

## Proof

- **Suites:** 1,142 passed / 0 failed on the final tree (`run-tests.sh`), incl. 22 new `QuestChainCoreTest` checks built on the genuine lighthouse chain data (specimen at three depths with exact line wording, grounding at the root quest, dedupe, carried-quest exclusion, cycle safety, unnameable prerequisite, cap, multi-permission walk). The known intermittent SkillNudgeMath tie-break flake (pre-existing, untouched code) may appear in other runs.
- **Deterministic build:** two consecutive `build.sh` runs byte-identical. Final jar: 506,738 B, SHA-256 `ae6b53a6a76c3a208797242c9474dc14951868f2213c6a2432e889af4d734d54`. Deploy config: 72,329 B, SHA-256 `ded17ff994b031ab524ce81a5730580b9f6fd82796e69f2bc8559bc84752fcb3`.
- **Rig proof** (Paper 26.1.2 rig clone in this lane's `rig/`; the fleet's mineflayer stack cannot speak Paper 26.3's protocol yet, so the rig ran the fleet-standard 26.1.2 launcher — server version does not touch the module's logic): live EliteMobs jar (10.9.10 file / 10.9.14 internal) + the FULL genuine Primis quest (105) and NPC (266) files pulled fresh from the live server on the PC Nov 24, 2026. The genuine Alanus file was staged with ONLY its spawn location repointed to a rig platform. Completions were marked through EM's own `PlayerQuestCooldowns.addCooldown(player, lockoutPerm, lockoutMinutes)` — the exact call `CustomQuest.applyEndPermissions` makes — via a rig-only helper plugin (ChainProbe, in `rig-probe/`, NOT shipped). Bot: mineflayer `ChainBot` (`rig/bot/chainbot.js`). Four cases, all PASS (chat evidence in `rig/bot/chain-chat.log`, server side in `rig/boot.log` — both rotated during the session; the decisive lines are quoted in the packet):
  - **A (specimen):** mains 1–11 marked → click → EM generic + `Locked: finish 'The groves call!' first — Bromia Curio in The Groves gives it.`
  - **B:** + Groves Call → `Locked: finish 'Population control!' first — King Nemian gives it.`
  - **C:** + Population Control → `Locked: finish 'Feeding the sick' first — King Nemian at the Lighthouse gives it.`
  - **D:** module disabled, same marks → EM generic only, zero Locked lines.
- **Rig-stage artifacts mapped (not module behavior):** EM's quest registry is empty at ExcelEMFixes enable and populates ~30–80 s later (the module starts empty and its 30 s re-index lands the census line — on the live server the same applies; the packet's acceptance includes the census line). EM completion markers set by the probe did not survive every abrupt rig restart (they did survive one clean restart; in-session marking is the proof standard used). On live, completions are written by EM's real completion flow into its database, which is the durable path production players already rely on.

## Filed (D212)

- Packet: `~/workspace/em-quest-chain-clarity-20261124/packet/` — INSTALL-PACKET.md (gates chain on live 0.15.0 jar `f93678f0…` + config `68602aa8…`, STOP if 0.15.0 not live — note: 0.15.0 INSTALLED later the same evening, so the gates describe the current live state), the jar, deploy-config.yml, SURFACES.md, INSTALLED.txt marker (empty until installed).
- MANIFEST: PENDING entry at the top of the PENDING entries section of `~/workspace/wrv-install-queue/MANIFEST.md`. **Not installed by this lane.**
- SURFACES.md carries: What's New draft `2026-11-24-quest-chain-clarity` (player-announcement format), website row bump 0.15.0 → 0.16.0 / modules 21 → 22, and the post-install Jester note — framed per Mike's Nov 24, 2026 rule (players are never assigned test work): a heads-up DM closing his own report; the live proof itself is FEEDBACK-WATCH (Jester clicking Alanus in natural play; Iris verifies from the log).

## Owed after install

1. Live-player proof (feedback-watch, above) — closes the module's proof; only then does the upstream notice go to MagmaGuy naming ExcelEMFixes 0.16.0 (module workflow + testing bar; draft gets Mike's eyes first).
2. Records close-out: wrv-plugins draft PR + Drive deposit (parent/supervisor chain).
3. Note for the fleet: any future module listening to entity-interact events must assume EliteMobs has already cancelled them — `ignoreCancelled` listeners will silently never fire on EM NPC clicks.
