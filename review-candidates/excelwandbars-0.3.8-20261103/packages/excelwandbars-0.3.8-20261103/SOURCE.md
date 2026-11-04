# ExcelWandBars 0.3.8 — source record

- **Base:** ExcelWandBars 0.3.7 source tree (`~/workspace/wandbars-0.3.7/work/`),
  itself verified byte-identical to the shipped 0.3.7 jar
  (`fc77e22f40e4719da86ea639efb91a0e46ddbf3ee02db37b12bf09ae5d770c59`,
  499,456 B) from the 0.3.6 base jar
  (`8335db03f015adc4a6a2464eee268fe7306f09d1abe96dec137f6474ba42a98f`)
  before any 0.3.8 change was made. Base verification repeated in this
  build folder: the unmodified tree was rebuilt and repacked and matched
  the shipped hash exactly.
- **This folder:** `work/` is the full 0.3.8 source tree (the 0.3.7 tree
  plus the delta below); `base-0.3.6.jar` and `base-0.3.7-shipped.jar` are
  the reference jars; `tests/` holds the pure suites (the nine 0.3.7 suites
  copied unchanged, plus three new 0.3.8 suites); `rig/` holds Probe038,
  its prep script and evidence.
- **Delta vs 0.3.7 (12 changed + 5 added jar entries, 0 removed):**
  - NEW `PagerHook.java` — pager buttons in the spell inventory.
  - NEW `core/PagerPlan.java`, `core/JoinGuardPlan.java` — pure plans.
  - `ArrangeHook.java` — pager-slot guards on all four interpreters;
    commit-context for in-flight drag-write gestures; lift rescue;
    hold-read cancels lifts only when its form actually opens.
  - `TapBarHook.java` — the join guard (stamp, scheduled checks,
    `joinGuardCheck`), `barOpen()` for the pager, join counters.
  - `MagicHook.java` — `flipPage(player, dir)` for the pager buttons.
  - `CompassGuard.java` — `checkLater()` entry for the join guard's tidy.
  - `ExcelWandBars.java` — PagerHook registration + `compassGuard()`.
  - `plugin.yml` — version 0.3.8 + description; `config.yml` — the three
    new `tap-bar` keys (all honored from code defaults when absent).
  - Inner-class byte shifts in `ArrangeHook$1`, `ExcelWandBars$1`,
    `MagicHook$Ctx/$LastForm/$View` are synthetic-accessor renumbering from
    the added methods/lambdas; no source in those classes changed beyond
    the listed edits (same phenomenon as the 0.3.7 build record).
- **Build:** Temurin 25.0.4.1, `javac --release 25` over `work/cp.txt`,
  repacked from the 0.3.6 base with `work/repack.py` (5 Geyser hook classes
  pinned to shipped v65 bytes; sorted entries; 2026-01-01 timestamps;
  deflate-9). Built twice → byte-identical.
- **Candidate:** `ExcelWandBars-0.3.8.jar`, SHA-256
  `57b86601a8eafc87e16630cd32e725e9c3d329dd6d5e2cae8976008c0e5a37db`,
  511,977 bytes, 228 entries.
- **Upstream:** none — ExcelWandBars is our own plugin (WinkReyesValley).
  Magic (elmakers) is untouched; all Magic interaction is through its API
  and the impl Wand's public page methods, as in prior versions.
