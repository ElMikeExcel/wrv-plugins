# INSTALL PACKET — ExcelEMLeash 0.1.0 (FINAL)

**Lane:** Runner (primary install lane per D156).
**Unit:** NEW plugin — ExcelEMLeash 0.1.0 (D167). No live premise
conflicts: no ExcelEMLeash jar or folder exists live; EliteMobs stays
at the pinned 10.9.14 (jar file named 10.9.10). Nothing else changes:
no boss files are edited — by hand or by the plugin. Leg B's route is
the confirmed RUNTIME route (P0): it mutates EM's loaded
`CustomBossesConfigFields` objects in memory via
`setLeashRadius(double)` (a bare putfield, no file persistence) and
re-applies on a 5-minute self-heal pass, so the floor survives EM
reloads and content-pack updates. The managed-file route was NOT
needed and is not used.

## Candidate
- `ExcelEMLeash-0.1.0.jar` — **11,438 B / SHA-256
  `9da74f34ec97da8227b3531e76e4fe7e921abcdf2325742ead08cf2d29dbc90b`**
  (double-build byte-identical; also in SHA256SUMS).
- plugin.yml: name `ExcelEMLeash`, version `0.1.0`,
  `softdepend: [EliteMobs]`.
- Default config: aggro-clear on, radii on, `minimum-radius: 35`,
  no overrides, `debug: true` (house precedent — flip off after the
  post-install proof, below).

## Install steps (Runner lane)
1. STOP gate: no `ExcelEMLeash*.jar` already live; marker discipline
   per the lane (no foreign window in progress).
2. BACKUP FIRST (house rule, cap-at-6): window backup before the swap.
3. Place `ExcelEMLeash-0.1.0.jar` in `plugins/`.
4. Emptiness gate: `list` = 0 twice; restart (not reload) so EM loads
   first (softdepend ordering) and ExcelEMLeash applies on a clean
   boot, before any post-install boss spawn reads the fields.
5. Boot-verify (below). Rollback: remove the jar + the generated
   `plugins/ExcelEMLeash/` folder, restart. The floored radii live
   only in EM's in-memory fields, so the restart returns every boss
   to its file value on its own. (P0 nuance: EM's own framework may
   persist field values on its own schedule; if it ever wrote a
   floored value into a boss file, that value is a plain valid
   `leashRadius` — harmless, and exactly what the floor would set
   anyway.)

## Boot-verify lines (expected, SEVERE 0)
- `[ExcelEMLeash] Enabling ExcelEMLeash v0.1.0`
- `[ExcelEMLeash] ExcelEMLeash 0.1.0 ready — aggro-clear on, radii floor 35.0.`
- One `LEASH` summary line from Leg B (fires on the first apply,
  ~20 ticks after enable), exact format:
  `[ExcelEMLeash] LEASH radii applied: bosses seen <N>, with leash <N>, floor 35: raised <N>, overrides in force 0, values changed <N>`
  Live expectation from the investigation census: bosses seen ≈ 252,
  with leash ≈ 221, raised = the count of Primis bosses whose file
  radius is below 35. The line must state bosses seen, how many
  raised, and the floor used — if it is missing, or reads
  `EliteMobs boss registry is empty`, the install has NOT landed.
- Per-boss debug line for the proof boss (debug is on):
  `[ExcelEMLeash] [debug] LEASH primis_feral_wolf.yml: leashRadius 20 -> 35 (floor)`
- No new WARN/ERROR from EliteMobs about custom boss configs.

## Post-install proof (player-visible, after install)
1. In Primis, pull a regional boss with a tight stock leash — the
   feral wolf (`primis_feral_wolf.yml`, stock radius 20) — past 20
   blocks from its spawn: with Leg B the effective radius is the
   35 floor, so no return starts at 10/20 blocks anymore; the boot
   `LEASH` line (and the per-boss debug line above) shows the wolf
   at 35.
2. Drag the same boss past 35 blocks: the leash return completes and,
   with Leg A, the boss STAYS home (healed, as EM designs) and does
   NOT charge back out at the player who pulled it. It only
   re-engages when genuinely re-pulled (a player comes to it /
   damages it). Debug evidence in the log:
   `[ExcelEMLeash] [debug] LEASH aggro cleared: <boss> stays home (hardObjective=…)`.
   (This is the end-to-end Leg A test: the scratch rig could not
   spawn a full EM regional boss — FreeMinecraftModels absent — so
   the rig proved Leg A's skip path and decision core, and this
   live proof proves the clear itself. See BUILD-RECORD.md, P4.)
3. Regression spot-check: a dungeon/instance boss and a natural elite
   behave exactly as before (Leg A never touches them).

## Surfaces owed at install (same-pass rule)
- What's New (website + in-game ExcelWhatsNew), website plugin list,
  GitHub record — handled by the supervisor chain at filing/install.
- Drafted What's New line: "Bosses that get dragged too far from home
  now stay home after they return — no more teleport-loop fights — and
  the tightest boss leashes got some breathing room."
- After the proof lands: flip `debug: false` in
  `plugins/ExcelEMLeash/config.yml` (live-edit + restart or reload of
  ExcelEMLeash only).
