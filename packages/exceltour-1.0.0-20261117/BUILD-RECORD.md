# BUILD RECORD — ExcelTour 1.0.0 (guided tours, v1)

Built Nov 11, 2026 (build worker for the WRV board supervisor).
Design of record: `~/workspace/exceltour-design-20261111/DESIGN.md`
(43,398 B / SHA-256 7875b09e…), first route mirroring
`~/workspace/em-world-audit-20261109/PLAYER-FIRST-STEPS-DRAFT.md`.
**Not installed. No MANIFEST entry filed** — the supervisor files
the packet after verifying this record. No live install, no
records/GitHub/Drive legs in this build.

## Candidate

- `ExcelTour-1.0.0.jar` — **50,656 B** — SHA-256
  `4af7c455d98ead43603f75fe007444983c75345d70f0109c9149dc785c78dd72`
- Deterministic double-build byte-identical (`build.sh` builds
  twice and `cmp`s), AND a clean-room rebuild from the shipped
  source zip reproduced the same hash (three independent builds);
  fingerprint gate PASS — in-jar
  `excel-build.txt` = `EXC-TOUR-1.0.0-WRV-20261111-01-t0ur`).
- `ExcelTour-1.0.0-source.zip` — 32,897 B — SHA-256
  `836e5947a74d76890ab72874b9bf58aba4b36d7ac0892c4d5ac6172504e6b49f`
  (src/ + tests/ + plugin.yml + config.yml + build.sh + routes/;
  timestamps pinned 2026-11-11).
- Target: **Paper 26.3** (compiled against the house build-libs
  Paper API 1.21.4 surface first + `paper-api-26.3.build.231-stable`
  as fallback, the ExcelDowned house recipe), **JDK 25,
  `--release 21`**, `api-version: '1.21'` (current house value —
  ExcelDowned 0.1.2 / ExcelBossTrial 0.5.6). Hard dependency:
  Paper only. Soft: floodgate, Geyser-Spigot, ExcelMenu,
  ExcelBedrockMenu — all reflection-detected; the plugin enables
  with none of them present.
- Shipped route: `routes/guild-first-visit.yml` (also bundled in
  the jar) — 4,486 B — SHA-256
  `81756b4e9423466067131aedaf9501f66ed9859cbc5eeffd43380511cafd2c6f`.

## What shipped (the design, §§1–7)

- **Route/step YAML format** (§2): one file per route in
  `plugins/ExcelTour/routes/`, full field reference implemented;
  validation per §2.1 (missing id / zero steps / step without a
  trigger / duplicate step ids / arrival with neither a location
  nor TO-BE-PULLED / radius ≤ 0 → route does not load, others
  unaffected, failure logged by route + step id).
- **Triggers** (§3): `arrival` (0.5 s ticker, 3-D distance,
  already-inside counts, one step per evaluation — idempotent),
  `world-arrival` (positive `world` and the `not-world` variant;
  ticker + `PlayerChangedWorldEvent` fast path), `menu-advanced`,
  and `npc-interaction` (implemented as a name-match listener on
  `PlayerInteractEntityEvent` using Paper state only; **no shipped
  step depends on it** — see §8.3 below). `also-allow-menu-advance`
  backstops honoured.
- **Guild first-visit route** (§2.3): the 5 design steps in order
  (arrive → Casus → Rowan/class quests → Wood League Arena →
  transporter home), wording from PLAYER-FIRST-STEPS. Ships in
  the **§2.4 fallback posture**: no live coordinate pull was
  available to this build, so every unverified value stays
  `location: TO-BE-PULLED` — no coordinate was invented. The
  loader treats a TO-BE-PULLED trigger as degraded: it never
  auto-fires and gains the menu-advance backstop, so the whole
  route is walkable today via the Guide entry / `/tour next`
  (proven in tests + rig). Replacing a step's placeholders with
  verified values turns its arrival trigger live with no code
  change.
- **Presentation** (§4): Java = action bar (title + "Step N of
  M", re-sent on a 5 s refresh) + step body in chat on step
  change — §4.1 option (b), see deviations. Bedrock = step text
  in chat + a Cumulus SimpleForm (buttons: Done/Skip/Repeat/
  Restart/Close per step state) sent via the Floodgate API, else
  the Geyser API, else chat-only — reflection throughout, every
  failure falls back silently; closing a form changes nothing.
  Platform detection per §4.3 order (Floodgate API → Geyser API
  → Floodgate UUID-prefix heuristic, rendering only).
- **Guide entry** (§5): `/tour` / `/tour menu` is the contract
  the compass menus call. Java view = inventory menu (current
  step, Repeat, Done, Skip, Restart with two-click confirm,
  Stop); Bedrock view = the step form. Reading the view never
  advances anything.
- **Persistence** (§6): one YAML per player at
  `plugins/ExcelTour/players/<uuid>.yml`, keyed by the real UUID
  Paper reports; saved on every state change (completion saved
  in the same call that announces it) and on disable; unknown
  saved step id → that route resets to step 1 with a log line;
  corrupt/missing file = not-started, never a join error.
- **Commands/permissions** (§7): the full `/tour` table (alias
  `/exceltour`), tab completion, `exceltour.use` (default
  **true**) / `exceltour.admin` (default **op**). Parent surface:
  `/tour status <player>` works online and offline (server
  records + saved-file name lookup); `/tour reset <player>
  [route] [full]` — default keeps the completions count, the
  trailing `full` form clears it (the §6.3 distinguishable
  operations). No command advances or moves another player.

## Tests — 42/42 PASS (`bash build.sh`, TourTest plain-JVM suite)

Route parsing/validation (11: valid fixture, missing id, zero
steps, no trigger, duplicate step ids, arrival-without-location,
unknown trigger type, bad radius, TO-BE-PULLED fallback load +
warnings, placeholder never-fires, text stripping); advancement
per trigger type (13: wrong-world, menu refusal on a pure
world-arrival step, unskippable refusal, world-arrival fire,
menu-step immunity to location checks, menu-advanced fire,
arrival outside/inside radius, idempotency, NPC wrong/right
name, not-world stay/fire + completion, terminal completion);
skip/restart/reset (8, incl. skip recording, restart semantics,
default-vs-full reset completion handling); per-player isolation
(1); persistence save/load round-trip + name + missing/corrupt
(4); shipped-route fixtures (3: loads with the 5 design steps in
order, every step backstopped, whole route completable by menu
advances alone).

## Rig proof — PASS 5/5, SEVERE 0 (`rig-1.0.0/RIG-RESULTS.md`)

Paper 26.3 rig (SummonsRig tree) + Geyser/floodgate co-loaded,
NMS fake player: clean enable (guild route loaded in fallback
posture), console `/tour reload` of a driver-written rig route,
`tour start` → world-arrival auto-fired on the ticker →
`/tour next` menu-advanced → teleport into the arrival radius →
route completed with the completion persisted to the player's
real YAML in the same pass; console `/tour version` +
`/tour status TourRigA` read back "finished (1x)"; clean stop.
**Owed from the rig** (named, not assumed): a real Bedrock
client exercising the Cumulus form + dismiss path; the §8.11 EM
non-interference control run on an EM-bearing mirror; a live
relog-resume (covered instead by the persistence round-trip
tests); Java menu click-through (its actions share the proven
`handleTourAction` path).

## §8 build-time verification — settlement table

| # | Item | Verdict |
|---|---|---|
| 8.1 | Guild world identity | **OWED — G1 live pull.** Local indication only: the EM audit (`em-world-audit-20261109/AUDIT-PLAN.md`) records a Nov 6 live-captured boot line for world `em_adventurers_guild`. That is not a same-day receipt, so the shipped YAML keeps `world: TO-BE-PULLED`. |
| 8.2 | Guild package data as coordinate source | **OWED — G1 live pull.** No same-day package pull was available to this build; no NPC/arena/transporter coordinate is asserted anywhere in the shipped route. |
| 8.3 | Casus/Rowan names + locations; §3.4 interaction test | **OWED — live/rig-with-EM.** The npc-interaction listener is implemented (Paper name-match only, no EM internals) but no shipped step uses it; the live interaction test (what event/identity Casus carries) was not run — outcome unrecorded by design, arrival/menu triggers carry the route. |
| 8.4 | Wood League Arena location/anchor/radius | **OWED — G1 live pull.** Radius ships at the design's 12 pending the verified anchor. |
| 8.5 | Transporter behaviour → step-5 form | **DECIDED at build time, observation OWED.** Shipped form = arrival-at-transporter + menu backstop (the `not-world` form cannot be written while the guild world name itself is unverified); the decision + reason are recorded in the shipped YAML comment. Revisit when 8.1/8.5 observation lands. |
| 8.6 | Pairwise step distances | **N/A until coordinates exist** — no radius can contain a previous target while no targets are set. Must be run as part of the coordinate fill-in. |
| 8.7 | Paper 26.3 APIs | **SETTLED locally.** `PlayerChangedWorldEvent` + `PlayerInteractEntityEvent` confirmed present in `paper-api-26.3.build.231-stable.jar`; Adventure `sendActionBar` compiled + rig-proven on Paper 26.3; `api-version '1.21'` per current house builds; toolchain JDK 25 / release 21. Sidebar-vs-chat settled as §4.1(b) — see deviations. |
| 8.8 | Geyser/Floodgate versions + Cumulus | **SETTLED locally in part.** floodgate 2.2.5-SNAPSHOT + Geyser-Spigot 2.11.3-SNAPSHOT jars (rig stack; copies also at `~/workspace/minecraft-server/plugins/`) verified by jar listing to carry `FloodgateApi.isFloodgatePlayer` and Cumulus `SimpleForm` (floodgate: `org.geysermc.cumulus.*`; Geyser: `org.geysermc.cumulus.form.*`); co-boot with ExcelTour proven on the rig. **OWED:** a real Bedrock client's form render + dismiss-no-callback behaviour. |
| 8.9 | Compass-menu hook | **CONTRACT SETTLED; staged config OWED to the packet.** `/tour menu` is implemented as the entry point both menus call (the design's permanent contract). The ExcelMenu + ExcelBedrockMenu config edits are same-pass install surfaces for the supervisor's packet, not this build. |
| 8.10 | Command/alias collisions | **SETTLED locally, live pull still owed at packet time.** No `plugin.yml` anywhere in the workspace declares a `tour`/`exceltour` command or the `exceltour.*` nodes (searched). |
| 8.11 | EM non-interference | **BY CONSTRUCTION; control run OWED.** ExcelTour reads/writes no EM config or data; its listeners are join/quit, world-change, entity-interact (name read only), and a location ticker. The with/without-ExcelTour guild-flow control comparison needs an EM-bearing mirror and remains owed (DESIGN §9.2: this proof is not degradable before install). |

## Design deviations (each with its reason)

1. **§4.1 presentation: option (b) shipped, not (a).** The body
   goes to chat on step change instead of a sidebar objective —
   the design's own sanctioned alternative when sidebar ownership
   is contentious (§4.1/§9.2); WRV runs scoreboard-adjacent
   plugins and no live sidebar-conflict check was available to
   this build. Action-bar behaviour is unchanged.
2. **`find-casus` gained an explicit `also-allow-menu-advance:
   true`** (the design's §2.3 YAML shows it only on steps 3–5).
   Required by the §2.4 fallback posture the task specified: a
   TO-BE-PULLED arrival step without a backstop would strand the
   route. The loader also backstops any placeholder trigger
   automatically, so the flag is belt-and-braces documentation.
3. **Step 5 ships as arrival + backstop, not the `not-world`
   variant** — decided per the §2.3 note's own rule (see 8.5).

## Surfaces / next legs (supervisor's packet, not this build)

Compass-menu Guide entries (both menus), Guide knowledge,
What's New both surfaces, tips check, G1 live-premise receipts
(§8.1–8.5), the packet's MANIFEST filing, and records
close-out are the install packet's same-pass obligations
(DESIGN §9.1 step 5). This build touched none of them.
