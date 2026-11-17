# ExcelPartyDrops 0.1.0 — BUILD RECORD

**Built:** Oct 20, 2026 (overnight), by Argus (subagent build)
**Spec:** `~/workspace/mob-scaling/PARTY-DROPS-SPEC-2026-10-20.md` — implemented EXACTLY,
including §7 as DECIDED by Mike (~12:17 AM, Oct 20): the quiet hint ships.
**Approved:** spec Oct 20 ~12:00 AM ("Go for it"); build Oct 20 ~12:26 AM ("Build both").
**Jar:** `ExcelPartyDrops-0.1.0.jar` — 14,731 B,
SHA-256 `afae735b991d683cf45f4aa30187f51aaf53bf2b7b114761d4279979beba6196`,
MD5 `590d97fe78e7b03d74bcfcd5fb8659b8`.
**Drive deposits (MD5-verified local == Drive):**
jar → Build files `1cUXP9OKVxcgDBdB351TztnLLBiMV2rz5`;
source zip → Source `1CaBwAtetwcXP2TxBjljzpF2HWqiN4gDE` (29,765 B, MD5 `c65b451e…`);
SOURCE.md → Source `1rGgSVptzlpCstgDLzup6wL0XGsJEe3aS`.
**Install:** packet `INSTALL-PACKET.md`; manifest entry appended to
`~/workspace/wrv-install-queue/MANIFEST.md` (end of PENDING); marker
`~/workspace/excelpartydrops/INSTALLED.txt`.

## Design (as specced, §1–§8)

- **Vehicle:** new plugin, `softdepend: EliteMobs`. A dedicated
  `EmDeathListener` class (the only class referencing EliteMobs types) is
  registered only when EliteMobs is present and enabled at onEnable — the
  AbilityBridge guarded pattern; the main class references no EM types.
  Without EM the plugin enables, logs "EliteMobs NOT found - inert", and
  `/partydrops` still works.
- **Event:** `EliteMobDeathEvent` at **MONITOR** (observe-and-mutate only
  `getEntityDeathEvent().getDrops()`; the vanilla death is already fully
  resolved by then; our mutation happens before Paper scatters the list —
  rig-verified). No plain `EntityDeathEvent` listener exists (anti-spam
  by construction: non-elites never produce a log line or a hint).
- **Multiplier:** participants = distinct real players (Floodgate UUIDs
  count — they are real players), alive (not dead), same world, within
  50.0 blocks 3D (inclusive) of the elite's death location, snapshot via
  `world.getPlayers()` at the death tick; killer NOT required to be in
  range. Multiplier = 1 + 0.5 × (participants − 1), cap 3.0, participant
  cap 5 → 1 / 1.5 / 2 / 2.5 / 3.
- **Range math:** `core/PartyRange.within()` is ExcelTogether's
  `PartyBonus.within()` semantics copied verbatim (`sameWorld &&
  dx²+dy²+dz² <= radius²`; Together exposes no callable API — its finders
  are player-anchored and membership-gated). Deliberate divergence,
  per spec: alive-only (Together's `near()` never checks `isDead()`).
  A conformance unit test pins the shared boundary behavior.
- **Scaling:** per-stack probabilistic rounding — floor(amount × m) +
  one Bernoulli(frac) draw per stack; exact-integer cases take ZERO RNG
  draws (pinned by a counting RandomSource in tests). Stacks split at
  the material's max stack size. Empty drop lists return before any
  change or audit line.
- **Origin gate (both must hold):** `EliteEntity.isNaturalEntity()` AND
  `getSpawnReason() == NATURAL`, plus stand-down when
  `isTriggeredAntiExploit()`. Bytecode-verified against the pinned jar:
  EliteEntity's constructor sets `isNaturalEntity = (spawnReason ==
  NATURAL)`, so the two conditions are exactly EM's own definition of a
  natural elite.
- **Untouched, by construction:** EM-generated gear (never enters
  `getDrops()`; EM pays its ≥10% damagers itself), coin showers (items
  are not coins), dropped XP, Jobs, mcMMO. The EM 20/30-min rewarded-kill
  cap is deliberately NOT inherited; `respect-farming-cap` exists in
  config as a reserved key — 0.1.0 logs a warning if it is set true
  (accepted-but-inert), documented in config.yml comments.
- **Hint (§7 DECIDED):** action bar to participants only, text
  "Hunting together pays off — the group earned extra drops.", gray;
  first multiplied kill for a player always shows; afterwards at most
  once per `hint-cooldown-seconds` (default 600) per player; nobody off
  the participant list ever sees it. One console audit line per
  multiplied kill with a non-empty drop list:
  `multiplied elite drops: type=… level=… participants=… multiplier=… items A -> B`.
- **Command:** `/partydrops status|reload`, permission
  `excelpartydrops.admin` (default op).
- **Config keys** (all validated with the house warning+fallback
  pattern): enabled, party-radius 50.0, step-per-extra 0.5,
  multiplier-cap 3.0, participant-cap 5, respect-farming-cap false,
  hint-cooldown-seconds 600, debug false.

## Automated tests — 94/94 PASS (plain-JVM harness, `run-tests.sh`)

Multiplier table 0–12 participants (cap behavior incl.); rounding
identity at exact integers with ZERO RNG draws asserted; rounding
distributions over 20,000 fixed-seed trials (stack 1 @1.5 → mean 1.5
±0.02; stack 3 @2.5 → mean 7.5 ±0.03; stack 5 @3.0 → exactly 15);
range conformance (exactly 50.0 in, 50.0001 out, 30/40 diagonal on the
boundary, cross-world out, dead out, spectator out); participant filter
(killer-not-required, out-of-range excluded, duplicate UUID counted
once, cap 5, Floodgate-style UUID counted); origin-gate matrix (NATURAL
eligible; SPAWNER / SPAWNER_EGG / BREEDING / RAID / COMMAND / CUSTOM /
DEFAULT all ineligible; anti-exploit flag stands down; not-natural
stands down); config validation fallbacks; hint cadence (first kill
shows, cooldown blocks, other players independent, 100 multiplied kills
in 100 s → exactly 1 show).

## Scratch-rig proof — Paper 26.2-123 + EliteMobs 10.9.8 (live-baseline config set)

Rig: `scratch/server/` (AimAssist rig copy; RIG-ONLY config edits:
`MobCombatSettings.yml` eliteMobsSpawnPercentage 1.0 — natural conversion
at natural-cycle rates is near-zero in a headless rig — and
doSpawnersSpawnEliteMobs true; later, for the party-kill leg only,
`ItemSettings.yml` flatDropRateV3 1.0). Driver: 5 fake ServerPlayers,
fake #2 carries a Floodgate-format UUID (`.RigBedrock`). Paired drop
probes at LOWEST (pre) and MONITOR (post) on every EntityDeathEvent.
**Zero SEVERE in all 7 boots.** Logs: `scratch/RIG-LOG-2026-10-20-BOOT{1..7}.txt`.

| Proof | Result |
|---|---|
| Solo, NATURAL ×30 | **PASS** — 30/30 converted, 30 EliteMobDeathEvents, pre 32 = post 32, ratio 1.000 |
| Duo, NATURAL ×30 | **PASS** — pre 34 → post 53, ratio 1.559 (Bernoulli band around 1.5); audit lines correct; participant #2 was the Bedrock-UUID fake |
| 5 players, NATURAL ×12 | **PASS** — pre 13 → post 39, ratio exactly 3.000 |
| SPAWNER-reason batch ×15 | **PASS** — pre 19 = post 19, exactly 1× (EM does not convert API-reason spawns; the death path was exercised end-to-end) |
| Real spawner-block elites | **EM did not convert vanilla spawner zombies at all** on 10.9.8 across two boots, placed and load-time spawners alike (0 of 9+ spawns, probe-verified reason=SPAWNER, elite=false) — recorded as an EM behavior observation, not a PartyDrops result. The gate against a spawner-born elite therefore stands on: (a) the unit origin-gate matrix, (b) the API-reason batch above, and (c) the bytecode fact `isNaturalEntity ⟺ spawnReason == NATURAL` in EliteEntity construction — a spawner elite, if one ever exists, fails both gate conditions by construction |
| Wither (vanilla boss) | **PASS** — pre 1 = post 1, zero EliteMobDeathEvents; non-elites are never engaged |
| Quiet hint | **PASS** — exactly 1 ClientboundSetActionBarTextPacket reached a participant across 3 multiplied kills (first teaches, cooldown silences). First attempt read 0 because EM's first-join initialization stalled the server ~15 s and the kills landed before EM finished initializing (watchdog thread dump in boot-2 log); the re-run after init completed passed cleanly — rig artifact, documented here rather than hidden |
| Real combat kills (killer actually fighting) | Drop scaling held on every kill (2→3, 2→3, 1→2, 3→5 at ×1.5) |
| EM party coexistence | **PASS** — a real EM party was formed through EM's own commands (create/invite/accept between two fakes); on the party kill, the vanilla list scaled (1→2 at ×1.5) AND 3 EliteMobs-generated items (custom name/lore) dropped on the ground beside the corpse — EM's own gear pipeline delivered on the same kill, undisturbed. PartyDrops never calls EM internals; it edits only the vanilla list at MONITOR |
| EM absent (jar removed) | **PASS** — clean enable with the inert line, `/partydrops status` answers "EliteMobs not hooked (inert)", zero SEVERE |

Honest bounds: the rig's elites are programmatic NATURAL-reason spawns
(which EM converts and marks natural — the same EliteEntity state a
natural-cycle elite carries); a headless rig cannot reproduce the
natural spawn cycle itself. EM gear in the non-party gear-kill leg
rolled zero in 6 kills at the stock 0.2 flatDropRateV3 (P ≈ 26% —
underpowered sample), so the gear proof was re-run at rig-only rate
1.0 in the party leg, where it passed.

## Rebuild reproduction

Two in-tree builds byte-identical; clean-room build from the deposited
source zip (`ExcelPartyDrops-0.1.0-source.zip`, 29,765 B, SHA-256
`87ae13d9…`) reproduced SHA-256 `afae735b…` exactly, tests 94/94 in the
clean room too (Oct 20, 2026).

## Effort

One overnight session: spec verification against the pinned jars
(javap), core + glue implementation, 94 unit checks, 7 rig boots,
bytecode gate proof, deposits + records. No failures in the shipped
artifact; the two rig surprises (EM first-join init stall; EM not
converting spawner spawns) are documented above with their evidence.

// ExcelPartyDrops — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
