# excelemfixes-0.16.0-20261124 — RIG RESULTS

Rig: Paper 26.1.2 rig clone (fleet-standard launcher; the fleet's mineflayer stack cannot speak Paper 26.3's protocol yet — server version does not touch the module's logic) + the live EliteMobs jar (10.9.10 file / 10.9.14 internal) + the FULL genuine Primis quest (105) and NPC (266) files pulled fresh from the live server Nov 24, 2026. The genuine Alanus Secundas NPC file was staged with ONLY its spawn location repointed to a rig platform; quest files 100% genuine. Completions were marked through EliteMobs' OWN completion call (`PlayerQuestCooldowns.addCooldown` with each quest's own lockout permission/minutes — the exact call `CustomQuest.applyEndPermissions` makes) via a rig-only helper plugin (ChainProbe, NOT shipped). Bot: mineflayer ChainBot. Full detail: the lane's BUILD-RECORD.md (included in this package) and `~/workspace/em-quest-chain-clarity-20261124/rig/`.

Four cases, all PASS:
- **A (the specimen):** main quests 1–11 complete → click Alanus → EliteMobs' generic line, then: `Locked: finish 'The groves call!' first — Bromia Curio in The Groves gives it.`
- **B:** + The Groves Call complete → `Locked: finish 'Population control!' first — King Nemian gives it.` (no place clause — that quest's objectives name none).
- **C:** + Population Control complete → `Locked: finish 'Feeding the sick' first — King Nemian at the Lighthouse gives it.`
- **D (module-off control):** same marks, module disabled → EliteMobs' generic line only, zero Locked lines.

The rig also caught the build's one real defect before filing: EliteMobs cancels `PlayerInteractEntityEvent` for its NPCs (how it suppresses the vanilla villager trade screen), so a first version listening with `ignoreCancelled = true` never ran. Fixed (the listener observes the cancelled click; it never modifies the event) and re-proven by cases A–D.

Unit suites: 1,142 passed / 0 failed on the final tree, incl. 22 new QuestChainCore checks built on the genuine lighthouse chain data. One known intermittent failure may appear in other runs: the pre-existing SkillNudgeMath tie-break flake (salted `Map.of` iteration in untouched code, root-caused Nov 24, 2026 during the 0.15.0 build — not this build's change).

Deterministic build: two consecutive build.sh runs byte-identical.
