# INSTALL PACKET — ExcelEMFixes 0.16.0 (quest-chain-clarity: locked quest NPCs name the next quest)

**Filed:** Nov 24, 2026 · **Lane:** em-quest-chain-clarity-20261124
**Commission:** Mike, Nov 24, 2026, 4:16 PM CT — when a player finds a chain-locked quest NPC, "it should let the player know what they need to do or what quests they need to do first before this one unlocks." Player report behind it: the same afternoon Jester clicked Alanus Secundas at the Pontis Groves Lighthouse and got only EliteMobs' generic red line — "This NPC has some quest(s) you can't accept yet! Finish the quests that come before them first." — with no way to learn which quests he was missing. Iris traced the chain by hand and told him (King Nemian → The Groves Call → Population Control → Feeding the Sick); this module makes the game itself say it, for every chain, from the data.

## What this ships

One new module, **quest-chain-clarity** (module #22, default ON): when a player clicks an EliteMobs quest NPC and a quest stays locked because its accept permission (the previous quest in its chain) is unmet, the player additionally gets one specific line per locked chain, right after EliteMobs' own message:

> `Locked: finish 'Population Control!' first — King Nemian gives it.`

- **The named quest is the NEXT one the player can actually go and do**: the walk follows accept-permission links from the locked quest until it reaches the first quest whose own accept permissions the player already holds (or which has none). A player deep in a chain is never told to redo finished links; a player at the start is told the chain's first quest.
- **The giver and the place come from that quest's own data**: the NPC who gives it (an NPC listing it, else its turn-in NPC, else the NPC its objective names), and the location text its objective carries when it has one ("Bromia Curio in The Groves gives it", "King Nemian at the Lighthouse gives it"). Clauses drop out cleanly when the content provides neither.
- **Computed entirely from EliteMobs' loaded data** — its quest registry (names, accept permissions singular + list, lockout permissions, turn-in NPCs, objective NPCs/places), its NPC registry (who gives what), and the player's completion markers read exactly the way EliteMobs' own `CustomQuest.hasPermissionForQuest` reads them (Bukkit metadata named by lockout permission, set by EliteMobs' `PlayerQuestCooldowns` on completion and re-applied at login). No quest, NPC, or place is known by name; any quest pack works.
- **Detection route (and why):** EliteMobs' api/ events cover quest accepts, completions, progression — but NO event fires on a refusal; the refusal is only a chat message inside its interaction handler. So the module listens to the same click EliteMobs does (`PlayerInteractEntityEvent`, main hand, MONITOR priority, observing only — it never alters the event, the click, or EliteMobs' message), identifies the NPC the way quest-giver-glow does (EliteMobs' own NPC spawn/remove events + a harvest of its spawned-NPC registry), and replicates only the accept-permission segment of EliteMobs' check. **Critical:** EliteMobs CANCELS this event for its NPCs (that is how it suppresses the vanilla villager trade screen), so the listener deliberately does NOT use `ignoreCancelled` — with it, the handler never runs (proven the hard way on the rig; see Proof).
- **Silence is the default for everything else:** quests EliteMobs refuses because they are already finished (lockout) or already carried earn no line; an NPC whose quests are all acceptable earns no line; a prerequisite no loaded quest grants (the chain cannot be named truthfully) earns no line; a cycle in the data terminates silently. Per-player + per-NPC cooldown (shipped 30 s) so repeated clicking never spams; at most 3 lines per click (config). Module off = zero behavior (rig-proven).
- README + config carry the REASON (Jester's Nov 24 report + Mike's direction), per the module workflow rule. Knobs: `cooldown-seconds` (30), `max-lines` (3), `debug` (false).

## Base + ordering (READ FIRST)

**Base tree:** ExcelEMFixes **0.15.0** (the skills-menu build, `~/workspace/em-native-skills-menu-20261124/`). This lane's tree was copied from the 0.15.0 build tree on Nov 24, 2026; 0.16.0 contains 0.15.0 entire (skills-menu included) plus quest-chain-clarity. Nothing else changed — the diff against the 0.15.0 tree is the new `questchain` package, its registry/version wiring, the appended config section, the README section, and its tests.

**ORDERING: chains on 0.15.0 — install the 0.15.0 entry FIRST (which itself chains on 0.14.0 → 0.13.0). If 0.15.0 is not live, STOP. Do not re-base onto 0.14.0 or earlier.**

## Artifacts

| File | Size | SHA-256 |
|---|---|---|
| `ExcelEMFixes-0.16.0.jar` (this folder) | 506,738 B | `ae6b53a6a76c3a208797242c9474dc14951868f2213c6a2432e889af4d734d54` |
| `deploy-config.yml` (this folder) | 72,329 B | `ded17ff994b031ab524ce81a5730580b9f6fd82796e69f2bc8559bc84752fcb3` |

Jar: deterministic double-build byte-identical. Deploy config = the 0.15.0 packet's deploy-config (68602aa8…) + exactly the appended quest-chain-clarity section (2,452 B; verified: the 0.15.0 config is a strict prefix — no other delta).

## Gates (STOP on any mismatch; re-pull fresh — hashes are the gate)

1. Live jar is `ExcelEMFixes-0.15.0.jar`, SHA-256 `f93678f0ed586d2cb20036d3506941c31ca5f38a3975411c1f7d39c66a86619c` (490,593 B) — the 0.15.0 packet's recorded hash.
2. Live `plugins/ExcelEMFixes/config.yml` SHA-256 `68602aa85027063a55964626f074972ec6e8c5a59aca2e3743443577dabfdc87` (69,861 B) — the 0.15.0 packet's deploy-config as placed.
   - **Fallback if this gate fails:** do NOT place the deploy config. Hand-append ONLY the quest-chain-clarity section (the block at the end of this packet's deploy-config.yml, from the `# MODULE: quest-chain-clarity` banner to the end of the file) to the live config, and record the deviation.
3. Live `plugins/ExcelEMFixes/` contains **NO** `clarity-menus.yml` (positive listing check — if an override appeared since the 0.15.0 window, STOP and reconcile before proceeding).

## Steps

1. Verified-empty window (standing rule). Backup: copy the live jar + `config.yml` to the window's backup names (`*.bak-<date>-pre0160`).
2. Server down. **MOVE** (never copy) the outgoing `ExcelEMFixes-0.15.0.jar` to its backup name; place `ExcelEMFixes-0.16.0.jar`. Exactly-one-ExcelEMFixes-jar census before Start.
3. Place `deploy-config.yml` as `plugins/ExcelEMFixes/config.yml` (or the hand-append fallback above). No other file placements.
4. Start. **Acceptance (all must hold):**
   - Boot line: `ExcelEMFixes 0.16.0 ready` with **22 modules** (0.15.0's 21 + quest-chain-clarity).
   - `Quest Chain Clarity ready` line at enable, and within ~2 minutes a `Quest Chain Clarity census — 112 quests indexed from EliteMobs' data.` line (EliteMobs populates its registries after our enable; the module re-indexes every 30 s — the census line is the proof the index is live; on live content the count is the live quest count, 112 at filing).
   - `emfixes reload` → ZERO reload-failed; SEVERE count 0; server reaches Done and stays up.
5. Write the marker: `~/workspace/em-quest-chain-clarity-20261124/packet/INSTALLED.txt` (window id, timestamp, end-state player count).

## Rollback

Restore the backup jar (MOVE back) + backup config, restart. The module is additive and read-only against EliteMobs: it writes nothing anywhere (no player data, no files), so rollback is just the jar/config restore and no data rollback exists or is needed.

## Proof behind this packet

- **Suites:** 1,142 passed / 0 failed on the final tree (22 new QuestChainCore checks built on the genuine lighthouse chain data: specimen walk at three depths, chain grounding at the root quest, dedupe across an NPC's quests, carried-quest exclusion, cycle safety, unnameable-prerequisite silence, line cap, multi-permission walk, exact line wording). One known intermittent failure may appear in other runs: the pre-existing SkillNudgeMath tie-break flake (salted `Map.of` iteration in untouched code, root-caused Nov 24, 2026 during the 0.15.0 build — not this build's change).
- **Rig proof (Mineflayer bot, Paper 26.1.2 rig clone + the live EliteMobs jar + the FULL genuine Primis quest/NPC content pulled fresh from the live server Nov 24, 2026):** the genuine Alanus Secundas NPC file, with only its spawn location repointed to a rig platform (quest files 100% genuine). Quest completions were marked through EliteMobs' OWN completion call (`PlayerQuestCooldowns.addCooldown` with each quest's own lockout permission/minutes — the exact call `CustomQuest.applyEndPermissions` makes), via a rig-only helper plugin that is NOT shipped. Four cases, all PASS:
  - **A (the specimen):** main quests 1–11 complete → click Alanus → EliteMobs' generic line, then ours: `Locked: finish 'The groves call!' first — Bromia Curio in The Groves gives it.`
  - **B:** + The Groves Call complete → `Locked: finish 'Population control!' first — King Nemian gives it.` (no place clause — that quest's objectives name none).
  - **C:** + Population Control complete → `Locked: finish 'Feeding the sick' first — King Nemian at the Lighthouse gives it.` (via the second lighthouse quest's chain — Feeding the Sick itself is acceptable by then).
  - **D (module-off control):** same marks, module disabled → EliteMobs' generic line only, zero Locked lines.
- **The rig also caught the build's one real defect before filing:** EliteMobs cancels `PlayerInteractEntityEvent` for its NPCs, so a first version listening with `ignoreCancelled = true` never ran (EliteMobs' line arrived, the module stayed silent through three diagnosis boots). Fixed (the listener observes the cancelled click; it never modifies the event), re-proven by cases A–D above, and documented in the module's class comment so it is never reintroduced. Two rig-stage artifacts were also mapped and are NOT module behavior: EliteMobs' quest registry populates ~30–80 s after ExcelEMFixes enables (the module's 30 s re-index covers it; the census line is the tell), and the rig's completion markers did not survive every abrupt rig restart (in-session marking + clicking is the proof standard used; live completions are written by EliteMobs' real completion flow).

## Proof owed after install

1. **Live-player proof (feedback-watch, per Mike's Nov 24, 2026 rule — players are never assigned test work):** Jester clicking Alanus Secundas at the Pontis Groves Lighthouse in natural play and receiving the Population Control / Groves Call line closes this. Iris verifies from the server log / ExcelProtect when it happens; the upstream report (below) waits for it. If it has not happened naturally within a few days, Iris can drive the same click on the live server as MikeExcel (no main-quest completions on that account — the line should name the chain's first quest) under the standing bot guardrails.
2. **Upstream (MagmaGuy):** per the module workflow + testing bar, once the live proof lands, the ExcelEMFixes release this module shipped in (0.16.0) goes back upstream naming the version — framed as what it is: a quality-of-life gap in EliteMobs' own refusal message (the generic line names no quest), with our module as the shipped workaround and a suggestion that EliteMobs name the blocking quest itself. Draft gets Mike's eyes first, per standing rule.
3. Records close-out (wrv-plugins draft PR + Drive deposit + website row) per the standing pattern — supervisor chain.
