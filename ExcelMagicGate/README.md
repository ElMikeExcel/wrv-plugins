# ExcelMagicGate 0.1.0

The automation bridge for the WRV Magic rollout (Route Sheet **WRV-070**).
Wink Reyes Valley's Magic (by NathanWolf) releases in five stages as the
Adventurers Guild seals fall. ExcelMagicGate watches the seal count, and
when a stage becomes eligible it applies that stage's curated config
packet automatically — config swap, Magic reload, LuckPerms grants — and
announces the moment server-wide.

**Status: BUILT, NOT INSTALLED.** Install is gated on the WRV-070 Phase 0
player trial passing (see INSTALL-RUNBOOK.md). Nothing in this folder
has touched the live server.

## How it decides anything: the GuildGate hook surface

ExcelGuildGate 0.1.0's source and data model were inspected first
(`~/workspace/excelguildgate/src/ExcelGuildGate/`, and the API verified
against the shipped jar with `javap`):

- Seal state lives in GuildGate's `SealState`, persisted to
  `plugins/ExcelGuildGate/state.yml`.
- GuildGate fires **no custom Bukkit event** when a seal breaks — seal
  changes happen inside `creditSeal()` (boss kills) and the admin
  `/guildgate grant|revoke|reset` commands.
- What it does expose, deliberately, is a public static API:
  `com.winkreyes.guildgate.api.GuildGateAPI` with `isUnlocked()`,
  `sealsBrokenCount()` and `sealsTotalCount()`. Its own javadoc says
  consumers read it **by reflection** to keep no hard dependency
  (ExcelBedrockMenu already consumes it that way), and it is null-safe /
  zero-safe when GuildGate has not enabled.

**Chosen surface:** reflection-read of `GuildGateAPI.sealsBrokenCount()`
on a light poll (default every 60 s, configurable), plus one extra check
whenever a player joins. Why not the alternatives: there is no event to
listen to (inventing one would mean modifying GuildGate — a separate
release); parsing GuildGate's `state.yml` from disk couples the bridge
to a private file format that can change silently, while the API is the
author's declared, versioned contract. The poll also distinguishes
"GuildGate absent" (plugin missing — watch idles, status reports it)
from "0 seals", which a file read cannot do reliably.

## What it does

1. **Stage apply (idempotent, failure-atomic).** `appliedStage` persists
   in the bridge's own `plugins/ExcelMagicGate/state.yml`. The watch
   applies only when `appliedStage < eligibleStage` (eligible = fallen
   seals; packets are cumulative, so it applies straight to the highest
   eligible stage). Steps, in order: **back up** the six managed Magic
   config files to `plugins/ExcelMagicGate/backups/<timestamp>-from-stage<N>/`,
   **swap** them for the target stage's packet files, **reload** Magic,
   **run grants**, **persist** `appliedStage`, **announce**. Any failure
   before persist logs SEVERE with the failed step, leaves
   `appliedStage` untouched, and retries on the next watch tick.
   Re-running an apply is harmless: swaps are overwrites, grants are
   idempotent `permission set` commands.
2. **Magic reload mechanism.** Upstream source (MagicPlugin 11.2.5,
   commit baa3d91 — the exact build of the shipped jar) shows
   `/magic reload` → `MagicController.updateConfiguration()` →
   `loadConfiguration()`: the same full configuration load used on
   enable, re-reading spells/paths/wands/classes from disk. It runs as
   a load task and logs its `Loaded N spells` / `Loaded N wands` lines
   on completion. The bridge dispatches it as a console command (per the
   build brief). Caveat, stated plainly: dispatch success means Magic
   accepted the reload; the reload's own completion is visible in the
   server log, and INSTALL-RUNBOOK keeps the packet runbooks' probes
   (`spells` count, wand count) as the operator's confirmation.
3. **Grants: the packets' own lists, verbatim.** Each stage directory
   ships the packet's `luckperms-default-group-grants.txt` untouched as
   `grants.txt` — the same human-audited console commands the packet
   runbooks have an operator paste by hand. **Console dispatch was
   chosen over the LuckPerms API** because (a) the bridge then applies
   byte-for-byte what was scratch-verified by hand, comments-adjacent
   ordering and all, instead of re-implementing it node by node; (b) the
   lists include group-targeted grants (Seal 4's `builder` group) that
   an API implementation would need special cases for; (c) it keeps the
   bridge free of compile/runtime coupling to LuckPerms internals, the
   same posture as the GuildGate reflection read. One deliberate
   addition: if a stage's list targets the `builder` group, the bridge
   first dispatches `lp creategroup builder` — the packet file only
   mentions group creation in a comment, and the builder grants fail
   without the group existing. Every dispatched command's acceptance is
   checked; a refusal fails the apply at the grants step. Limitation:
   LuckPerms reports per-node results in its own console output, not to
   the dispatcher, so the runbook keeps the `lp ... permission check`
   probes as the post-apply verification.
4. **Stage 0 (dormant).** A fresh install sits at stage 0: exactly the
   Phase 1 packet's `config.yml`, `spells.yml`, `paths.yml`,
   `classes.yml` and `messages.yml`, with precisely two differences —
   **`grants.txt` is empty** (zero LuckPerms commands; nobody holds a
   single `magic.cast.*` grant) and **`wands.yml` has the four rank wand
   templates removed** (beginner/student/apprentice/master), leaving
   only the hidden, spell-less base templates. No loaded wand template
   carries any spell, so there is no obtainable wand that can cast
   anything, and the curated spell universe on disk stays unreachable
   until Seal 1 falls and stage 1 restores the packet's full
   `wands.yml`. On a truly fresh install (no Magic config set at all)
   the bridge seeds these stage-0 files itself; a packet canary
   (`stormshackle` must exist in `spells.yml`) warns SEVERE at stage 0
   if the live Magic config is not a WRV packet, and clobbers nothing.
5. **The announcement.** On a successful apply: a title + subtitle to
   every online player, broadcast lines, and a sound (default
   `ui.toast.challenge_complete`), naming the beat and its headline
   unlocks. Per-stage text lives in the bridge `config.yml`
   (`messages.stages.<n>`), with built-in defaults from the roadmap —
   1 The Healer, 2 The Pathfinder, 3 The Wonder, 4 The Masons,
   5 The Guild Opens, the Schools Open. `{stage}`, `{name}` and
   `{unlocks}` placeholders are supported. Stage 0 never announces.
6. **Revoke safety.** If the seal count ever drops below `appliedStage`
   (`/guildgate revoke` is the remedy path), the bridge **never**
   removes grants or downgrades configs automatically. It logs a
   warning, messages online holders of `excelmagicgate.admin` once per
   stage, and leaves everything live until an operator acts by hand.
   Stages only move up — including via `/magicgate apply`, which
   refuses lower stages.
7. **Admin.** `/magicgate status` (master switch, seal source, seals
   fallen, applied stage, per-stage state, last apply result),
   `/magicgate apply <stage>` (the same apply path by hand — seal night
   run manually, or testing; equal-stage re-apply is allowed as a
   repair). Permission `excelmagicgate.admin` (default op). A master
   `enabled` flag in `config.yml` makes the watch inert while status
   still answers.

**Not in this plugin, by design (Mike's call, Oct 17 2026):** join
catch-up messaging for missed unlocks. That lives in ExcelGuildGate
(0.2.0), which reads campaign state itself. The two plugins stay
independent; the bridge keeps no per-player state of any kind.

## Layout

- `src/ExcelMagicGate/` — full source (plugin class + pure-logic core:
  `StagePlan`, `GrantList`, `StageApplier`, `BridgeState`,
  `StageMessages`), `plugin.yml`, `config.yml`, the six bundled stage
  packets under `stages/stage0..stage5/` (assembled verbatim from
  `~/workspace/magic-scratch/` packets; stage 0 derived as documented
  above), `build.sh`, `run-tests.sh`, and the test main under `tests/`.
- `build/ExcelMagicGate-0.1.0.jar` — the built jar (also copied at this
  folder's root).
- `scratch-evidence/` — the scratch-stack proof logs (see below).

## Tests

`run-tests.sh` compiles everything and runs a plain-JVM suite over the
pure logic, including assertions against the **real bundled packets**
(grant counts 0/13/33/59/67/100, stage-4 builder pre-command, heal/cure
probes, lava never granted): **107 checks, all passing.** The apply
orchestrator is tested with scripted fake steps (order, abort-on-failure
at every step, announce-failure tolerance), the planner across its whole
decision table, the state codec against garbage input.

## Scratch proof (summary)

On the local scratch stack (Paper 26.2 + LuckPerms 5.5.87 + Magic
11.2.5 + ExcelGuildGate 0.1.0 + ExcelMagicGate 0.1.0, stage 0
pre-seeded, fresh LuckPerms storage): booted dormant at stage 0 (30
spells loaded, only the 6 spell-less base wands, `magic.cast.heal`
undefined in LuckPerms); granted Seal 1 through GuildGate's own
`/guildgate grant ender_dragon`; the bridge's watch detected the seal
and applied stage 1 end-to-end (backup written, config swapped, reload
logged, 13 grants run, `magic.cast.heal` true, `chainlightning` still
undefined, announcement broadcast); revoked the seal and confirmed the
no-downgrade warning with stage 1 left live; restarted and confirmed
persistence (no re-apply, grants intact). Full logs in
`scratch-evidence/`. What console-only scratch cannot prove: the title
and sound rendering for a real player (the broadcast half is in the
log), and Bedrock-client behavior — both are Phase 0 trial items, as
with the packets themselves.
