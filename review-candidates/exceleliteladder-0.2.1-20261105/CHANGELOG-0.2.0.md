# ExcelEliteLadder 0.2.0 — the ladder table moves to config (Nov 5, 2026)

Mike's approval: 2.0 decision sheet item 3, Nov 5, 2026 ("Yes to all 2.0").
The trade, in his accepted terms: the table leaves the jar so a ladder retune
no longer needs a jar cycle — and in exchange the config loader is strict
enough to be "nearly as typo-proof" as the hardcoded table was: every defect
refuses the whole table with the exact defect named, and the last-good table
keeps running.

## What changed

- **NEW FILE `ladder.yml`** in the plugin data folder (`plugins/ExcelEliteLadder/ladder.yml`),
  generated from the bundled default on first boot when absent. It holds the
  ladder table — `ladder:` → `step-1`..`step-5`, each with `spawn`, `cap`,
  `normalized-to-elite`, `normalized-to-player`, `scaled-to-elite`,
  `scaled-to-player`. The shipped default is the exact 0.1.2 table
  value-for-value (proven by the migration equality suite, 231 checks total).
- **NEW core class `LadderConfig`** — strict parser/validator (pure Java, maps
  in, table out). Refuses, each with the defect named: missing/extra/renamed
  step rows, a step row that is not a mapping, a missing field, an UNKNOWN
  field (where typos land), a non-numeric/blank/boolean value, NaN or
  infinite numbers, a non-integral cap, out-of-range values (spawn in (0, 1],
  cap 1..1000, multipliers in (0, 10]), and monotonicity violations (spawn
  and cap must never decrease step over step, scaled-to-player must never
  decrease, scaled-to-elite must never increase; plateaus allowed — the
  S4==S5 peak row depends on it). The two normalized (dungeon) values carry
  no monotonicity rule, by design (range only) — they have been constant in
  every shipped table.
- **`LadderTable` is now an instance.** `builtin()` carries the exact 0.1.x
  hardcoded values as the first-boot fallback and the migration reference.
  `step()` keeps its 0.1.x semantics (throws on out-of-range); `matches`,
  `differences`, `describeOffLadder`, `clampSeals`, and the pre-ladder
  OLD_S0/OLD_S1 recognition are unchanged statics. `StepPlan.driftCheck` and
  `FileApplier.apply` take the active table as a parameter; nothing else in
  the apply path changed (dated .bak, atomic move, verify, restore-on-failure
  are byte-for-byte the 0.1.2 code).
- **Loader discipline in the plugin.** At boot: missing ladder.yml →
  generated from the bundled default and used; valid → used; ANY defect →
  loud WARN (never SEVERE) naming the defect, and the built-in default table
  runs. On `/eliteladder reload`: the file is re-read; accepted → the new
  table is live immediately (and the watcher re-evaluates against it);
  refused → WARN + the reply names the defect, and the previous table keeps
  running untouched.
- **Console admin surface** (fleet standard): `/eliteladder reload` is new;
  `/eliteladder status` now reports the table source (`ladder.yml (config)`
  or `built-in default`), the last load outcome (including the exact defect
  when refused), and all five effective step rows. The enable line reports
  the table source too.
- **config.yml** keys unchanged; its stale "table is built into the jar"
  header now points at ladder.yml. plugin.yml usage/description updated.

## What did NOT change

- The ladder VALUES. A fresh 0.2.0 install behaves identically to 0.1.2:
  same baseline adoption, same up-only policy, same players-online gate,
  same state.yml format (carries over), same MobCombatSettings.yml writes.
- The EliteMobs file is still the only thing written; ladder.yml is only read.
- Retuning the peak now means editing the S4 AND S5 rows together (they are
  one plateau) — editing S4 alone is refused by monotonicity, by design.

## Tests

231 checks, 0 failed (TEST-REPORT.md): the full 0.1.2 suite carried over
against the built-in table, the migration equality suite, a 25-case
malformed battery (each: refused + defect named + last-good instance kept),
the reload-flip suite, and a three-boot Paper 26.3-235 rig (fresh-install
generation, in-session reload flip + refused reload, apply driven by the
flipped table, broken-config boot on the built-in default). SEVERE 0 on
every clean boot.
