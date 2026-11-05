# ExcelEliteLadder 0.2.0 — test report (Nov 5, 2026)

## Artifact

- `ExcelEliteLadder-0.2.0.jar` — **40,475 B**, SHA-256
  `781eea2fe99269fb1400b217f5026f29bcd03dc919501cc84b28aa56e083afd0`
- Deterministic: build.sh builds twice and `cmp`s them — byte-identical.
- Base proof: the UNMODIFIED 0.1.2 sources rebuilt with this VM's toolchain
  come out 31,499 B / `336a57d2…` vs the jar of record 31,403 B / `7ea61276…`
  — entry lists identical, plugin.yml + config.yml byte-identical, every
  .class differs. Cause: the original build machine's exact Paper API jar /
  javac patch (JDK 25.0.5+8, `/workspace/es135` paths) does not exist in this
  VM; the local compile uses Paper API 26.3 build 231 + JDK 25.0.4.1. This is
  a toolchain delta, recorded honestly — 0.2.0's acceptance does not depend
  on it: the migration proof below is value-level, and the rig runs the
  actual 0.2.0 jar on the actual Paper 26.3-235.

## Plain-JVM suites — 231 passed, 0 failed (`./run-tests.sh`)

- **Carried 0.1.2 suite** (fixture matching, peak/no-op semantics, patch
  bytes in EliteMobs number form, reader refusals, StepPlan decisions,
  FileApplier apply/no-op/refusal/fault-restore) — unchanged checks, now run
  against `LadderTable.builtin()` through the instance API.
- **Migration equality (67 checks):** the shipped `ladder.yml`, parsed with
  SnakeYAML (the engine family Bukkit uses), equals `LadderTable.builtin()`
  on all 5 steps × all 6 MobCombat keys (exact double equality) — and the
  `defaultSectionMap` round-trip equals it too. `matchStep` parity between
  the config table and the built-in table on all 7 staged fixtures
  (S1–S5-norm, old-S0, old-S1).
- **Malformed battery (25 defect cases × refused + defect-named +
  last-good-instance-kept = 75 checks):** missing step-4; extra step-6;
  renamed step-02; missing field; typo key `spwan`; spawn/cap as text;
  cap 120.5; spawn 0; spawn 1.5; cap 0; multiplier −1; multiplier 99; NaN;
  +Infinity; boolean value; blank value; spawn decreases at S3; cap
  decreases at S4; dealt (scaled-to-player) decreases at S4; taken
  (scaled-to-elite) increases at S2; step row not a mapping; ladder section
  not a mapping; section absent; null root (empty file). Plus two
  text-level cases: broken YAML throws at parse (the plugin converts that
  to a refusal), and the shipped file's own text with `spawn: .nan` is
  refused by the finite check with the built-in kept.
- **Reload flip:** holder loads the shipped table; an edit moving the peak
  spawn to 0.12 (S4+S5 together) is accepted and provably live — a
  MobCombat file carrying the edited S4 values matches the flipped table at
  S4 and is OFF the old table; a subsequent broken edit (step-5 removed) is
  refused and the flipped instance is still in force.
  - Note: the suite's first flip attempt edited S4 alone and was REFUSED by
    monotonicity (0.12 → 0.11 would decrease at S5). That is the validator
    working as designed; the scenario was corrected to edit the plateau
    pair, and the refusal itself is covered by the battery's shape.

## Rig — Paper 26.3-235, EliteMobs 10.9.12, ExcelGuildGate 0.4.0 (real jars)

Copied 0.1.2 rig (`rig/server/`), evidence in `rig/evidence/`.

- **R1 — fresh install (no plugin folder, EliteMobs file at S3-norm,
  3 seals broken):** ladder.yml GENERATED and byte-equal to the bundled
  default; enable line `…applied step none (no verified baseline), ladder
  table: ladder.yml (config) — default generated on first boot, watching…`;
  `Baseline adopted: live MobCombatSettings.yml equals ladder S3-norm
  (spawn 0.09, cap 120, toElite 0.5, toPlayer 2.05, …)` — identical
  behavior to 0.1.2; Done (32.372s); **SEVERE 0**.
- **R2 — reload flip in the same session:** `eliteladder status` shows the
  source, the load note, and all five rows. Peak spawn edited 0.11 → 0.12
  (S4+S5) in ladder.yml, then console `eliteladder reload` → accepted:
  `Ladder table reloaded from ladder.yml and is now live (S5: spawn 0.12,
  cap 150, …)`; status shows `Table load: reloaded from ladder.yml
  (accepted)` and S4/S5 at 0.12 — no restart. Then step-2 cap corrupted to
  text and reload again → WARN (not SEVERE): `Ladder table REFUSED on
  reload: ladder.step-2.cap must be a number, found: 'many' (text). The
  previous table stays in force; nothing was changed.` Status still shows
  the flipped 0.12 table. **SEVERE 0.**
- **R2b — the flipped table drives a real apply:** corruption repaired,
  GuildGate flipped to 4 seals, reboot → enable line `applied step S3,
  ladder table: ladder.yml (config)`; `EliteLadder applied S4 (from S3,
  seals 4, 0 online): spawn 0.12, cap 150, toElite 0.4, toPlayer 2.35…`
  with dated backup; `Post-reload check S4: EliteMobs enabled, file values
  match, sha c0cf3077… (unchanged by EliteMobs)`; the live MobCombat file
  carries `eliteMobsSpawnPercentage: 0.12`. **SEVERE 0.**
- **R3 — broken-config boot:** fresh folder with a pre-placed ladder.yml
  missing step-5 → WARN `Ladder table REFUSED at boot: ladder.step-5 is
  missing — the table must define step-1 through step-5. Running on the
  built-in default table (identical to 0.1.2). Fix ladder.yml and run
  /eliteladder reload.`; enable line `ladder table: built-in default`;
  baseline S3 still adopted on the built-in table; status shows the source
  and the REFUSED load note. **SEVERE 0.**

## Deviations / notes

1. Base-proof byte delta vs the 0.1.2 jar of record (toolchain, above) —
   the only deviation, and it predates the refactor (proven on unmodified
   sources).
2. The design summary's candidate shape for the config file was a per-step
   section list; the shipped shape uses kebab-case field names
   (`normalized-to-elite` etc.) in a dedicated `ladder.yml` — semantics
   identical to the hardcoded table; the dedicated file avoids Bukkit's
   comment-stripping rewrite of the existing commented config.yml.
3. YAML duplicate keys (the same key twice in ladder.yml) are not
   separately defect-checked — SnakeYAML/Bukkit resolve them last-wins
   before validation sees the map. All value-level defect classes are
   covered; this residual is named here rather than hidden.
