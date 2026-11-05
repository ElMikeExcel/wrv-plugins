# ExcelEliteLadder 0.2.1 — elite speed buff + the retuned default table (Nov 5, 2026)

Mike's direction: the Nov 5, 2026 retune bundle (decisions 08:34–08:37 CST) —
elite damage stepped down, elites tankier, spawn rates up, and a "tiny"
elite movement-speed buff whose knob, in Mike's words, is "giving them a
speed potion. Sort of how we did for projectiles making them faster."
Mechanism research: `SPEED-NOTE.md` in the retune packet
(`~/workspace/eliteladder-retune-20261105/`).

## What changed

- **NEW: natural elites get a Speed potion buff.** An `EliteSpeedListener`
  (registered only when EliteMobs is enabled — softdepend discipline
  unchanged) applies `Speed` to **natural elites only**:
  - **Trigger 1 — spawn:** on `EliteMobSpawnEvent` (not cancelled), 1 tick
    later (mirroring EliteMobs' own `movement_speed` power, which runs its
    application at `run_after(1)`), re-checking the mob is still valid.
  - **Trigger 2 — combat re-assert:** on `EliteMobEnterCombatEvent`, a
    natural elite that somehow lost the effect regains it. Event-driven;
    no polling, no timers.
  - **Effect:** `PotionEffect(SPEED, duration, amplifier, ambient=false,
    particles=false, icon=false)` — a hidden buff, the Halloween fork's
    spawn-buff idiom. An elite that already HAS a Speed effect is never
    touched: EM's own rolled `movement_speed` power (Speed II) outranks
    ours and is never downgraded, and neither is a player-splashed Speed.
  - Every application logs one INFO line (`Elite speed applied (spawn): …`)
    — the post-install proof hook.
- **Config — `config.yml` section `elite-speed:`** (NOT a ladder.yml key:
  the table validator refuses unknown keys, and that refusal is the
  typo-proofing Mike bought with the 0.2.0 trade; speed also maps to no
  MobCombatSettings.yml line, so it does not belong in the table):
  - `enabled: true` (default) — off stops NEW applications only. Living
    mobs are never stripped: our effect cannot be told apart from an EM
    power's own Speed effect, and population turnover clears it.
  - `amplifier: 0` (default) = Speed I (+20%, Mike's "tiny"). Setting 1
    reproduces EM's own boss-power value (Speed II, +40%).
  - `duration-ticks: -1` (default) = permanent for the mob's life
    (Integer.MAX_VALUE). EM's finite 100,000 ticks was deliberately NOT
    copied: a chunk-saved elite can be re-encountered over far more than
    ~83 minutes of wall-clock life, and a buff that silently expires
    mid-life is the defect shape to avoid.
  - Read at enable and on `/eliteladder reload`. Junk values fall back to
    the defaults, each named in a WARN (the house posture for the plugin's
    own config; the strict-refusal posture stays reserved for ladder.yml).
  - `/eliteladder status` gains the line `Elite speed: on (Speed I,
    permanent)` / `off`; the enable line gains `elite speed: ON
    (amplifier 0, duration permanent)`.
- **The built-in default table IS the retune now.** The jar's bundled
  `ladder.yml` resource AND `LadderTable.builtin()` carry the Nov 5, 2026
  retuned values (spawn 0.10/0.12/0.14/0.17/0.17; scaled-to-elite
  0.50/0.45/0.40/0.35/0.35; scaled-to-player 1.58/1.71/1.85/1.94/1.94;
  cap and dungeon values unchanged). Rationale: the fallback must never
  silently resurrect pre-retune tuning if the live ladder.yml is lost or
  malformed — a first-boot refusal on 0.2.0 fell back to the OLD table;
  on 0.2.1 it falls back to the retune. The shipped==builtin equality
  suite now pins the retuned values. Refusal/baseline messages that named
  "the exact 0.1.2 values" and the old S1-norm file hash were updated
  (new S1-norm sha `ff017c5d…`).

## Build-time confirmations (owed by SPEED-NOTE §3, settled against the real jar)

- **Accessor:** `EliteEntity.isNaturalEntity()` — public boolean, exact
  name confirmed via javap on EliteMobs 10.9.12. Its semantics are
  bytecode-exact: the EliteEntity constructor (and the normalized-actor
  path) sets it to `spawnReason == CreatureSpawnEvent.SpawnReason.NATURAL`
  — nothing else is "natural", by EM's own definition.
- **Registry lookup: NONE CLEAN — trigger 3 (chunk-load catch-up) is NOT
  implemented.** The jar has no EliteEntityManager, no static registry on
  EliteEntity, and `EliteMindService` only accepts an EliteEntity you
  already hold (it never resolves one from a Bukkit Entity). Per the
  spec's own fallback, 0.2.1 ships triggers 1+2 and records the omission
  here: toggling the feature on covers new spawns immediately and
  already-alive natural elites at their next combat — not at chunk load.

## What did NOT change

- The ladder machinery: strict validation, last-good fallback, up-only
  applies, empty-server gate, state.yml format, the six MobCombatSettings
  keys — all byte-for-byte the 0.2.0 code paths. config.yml's pre-existing
  keys keep their meaning; elite-speed keys are appended.
- Custom bosses: excluded by the isNaturalEntity gate, always. They can
  still roll EM's own movement_speed power themselves.
- With EliteMobs absent, the plugin behaves exactly as 0.2.0 minus the
  listener (never loaded, no errors).

## Tests

272 plain-JVM checks, 0 failed (TEST-REPORT.md): the full carried suite
re-run against the retuned table (fixtures regenerated to the retuned
-norm files; the pre-retune 0.2.0 S4/S5 files are now proven OFF-ladder),
shipped==builtin equality on the retuned values, the 25-case malformed
battery, the reload flip, plus a new 39-check elite-speed suite (parse /
clamp / warning behaviour, the natural-only gate truth table, labels, and
the hidden-effect spec the glue applies verbatim).

Rig (Paper 26.3-235 + the real EliteMobs 10.9.12 + GuildGate 0.4.0):
fresh-install generation (generated ladder.yml byte-equal to the bundled
resource), baseline adoption at the NEW S3 row, status + reload proofs,
the speed toggle read at boot — SEVERE 0 on every boot. The spawn
application itself is proven NEGATIVE on the rig (summon-converted elites
are CUSTOM-reason, isNaturalEntity=false by EM's constructor rule, and an
NBT readback shows zero effects on them — the exclusion gate working on a
real server) and pinned POSITIVE at unit level: a playerless rig cannot
produce a NATURAL-reason spawn (monster spawns need a nearby player), so
the positive landing is the packet's post-install player proof — the
first natural elite spawned after install carries the buff, visible as
~20% faster movement and named by the application log line.
