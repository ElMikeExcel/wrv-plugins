# WandBars tap-bar attempted verify-race change — ExcelWandBars 0.3.15 build record

Commission: Mike's Bedrock tap bar still failed after MagicGate 0.3.2
landed (Nov 12, 2026) — the spell-key sync errors were fixed, but the
Tap bar itself did not stay up. The first diagnosis identified a
verify-race in WandBars' style apply: the apply re-reads the wand
immediately after flipping its control keys, Bedrock item churn makes
that verify read see the pre-flip item, the apply reports FAILED, and
every failed attempt closed the bar first. This build is the attempted
retry change for that race.

STATUS CORRECTION (Nov 12, 2026): ExcelWandBars 0.3.15 installed and
booted clean, BUT Mike's live Bedrock tap-bar proof FAILED at ~12:58 PM
CST on Nov 12, 2026 — his report was "Still the same." The verify-race
change is therefore NOT player-proven and must not be described as a
working or reliable fix. A round-2 diagnosis afterward found the real
root cause to be a `hotbar_inventory_count` desync; an ExcelWandBars
0.3.16 fix follows from that diagnosis. Publication of this 0.3.15
records package is HELD until 0.3.16 lands and Mike's live proof
passes.

Desk work: built, unit-tested, and rig-tested on the desk, then
INSTALLED Nov 12, 2026 in a parent hand window (see section 6). This
records package was staged Nov 12, 2026 by the board supervisor's
records lane, mirroring the 0.3.14 package. Full per-file rationale is
in the build folder's BUILD-NOTES.md (`~/workspace/wandbars-verify-race-build/build-0.3.15/`);
this record is the package summary.

---

## 1. INITIAL MECHANISM HYPOTHESIS — the verify-race (superseded by round-2 diagnosis)

This section records the hypothesis this build was built against.
Round-2 diagnosis later found the real root cause to be a
`hotbar_inventory_count` desync (see the status correction above and
section 5); the mechanism below is not carried as the proven cause.

`StyleHook.apply` ran `configure → deactivate → checkWand` and then
verified the wand IMMEDIATELY. On Bedrock, the wand item churns after
a control-key flip, so the verify read can return the pre-flip item:
the residual control diff looks like a failed write even though the
write landed. The old tail turned that stale read straight into
FAILED, fed the WandActivatedEvent loop, and — because the apply
closed the inventory unconditionally before `saveState()` — tore down
the bar the player had just opened on every failed attempt.

## 2. THE ATTEMPTED CHANGE — four spec items (BUILD-NOTES.md maps each to source)

- **Spec 1 — delayed verify retry (new `core/VerifyRetryPlan.java` +
  `StyleHook.applyInternal`):** a verify miss no longer fails at
  once. `VerifyRetryPlan.verdict(...)` returns RETRY, one forced
  re-check/re-apply is scheduled 10 ticks later
  (`RETRY_DELAY_TICKS = 10`, `MAX_RETRIES = 2`), and the apply returns
  the new `Result.RETRYING` instead of FAILED. The retry's first act
  is a fresh re-check: if the item has settled it finds ALREADY and
  writes nothing. At most one pending retry per player; retries are
  guard-exempt by design (the first apply already consumed a guard
  token) and bounded tighter than the event path by the 2-per-chain
  cap + one-pending rule.
- **Spec 2 — no close on verify-fail:** the unconditional
  `closeInventory()` is now gated on
  `VerifyRetryPlan.shouldCloseInventory(...)` — close only when the
  pass will change something, the trigger is not a bar-open activation
  ("activate", "bar open"), and the pass is not a verify retry. Every
  pass of an activate/bar-open chain closes 0 times, retries never
  close on any chain, and deliberate applies (join, magic load,
  respawn, reload, style pick, spell bar change) keep Magic's
  original close → save → configure sequence. `TapBarHook`'s open-path
  repair now calls `apply(p, force, "bar open")` so it is a named
  bar-open trigger under this rule.
- **Spec 3 — branch logging:** FAILED and RETRYING verdict lines
  append the residual detail via `verdictDetail(...)`: branch name
  (`props` / `layout` / `props+layout`), the residual map for the
  props branch, layout description for the layout branch, and the
  wand's template key. `choose()` gained a RETRYING player line
  ("Your wand is still switching — hold it a moment.").
- **Spec 4 — held wand only (verified, no behavior change):** the
  apply chain reads and writes ONLY `mage.getActiveWand()`; no
  inventory scan exists on this path. Beginner-wand loadout scoping
  deliberately UNCHANGED in this build, per the backlog item.

Files changed vs 0.3.14 (complete list): NEW
`core/VerifyRetryPlan.java`; EDITED `StyleHook.java`,
`TapBarHook.java` (one call site), `BuildInfo.java`; RESOURCES
`plugin.yml` (0.3.15), `excel-build.txt`. `config.yml` and
`spellbook.yml` byte-identical to 0.3.14's — the install was JAR-ONLY.
Jar entry delta vs shipped 0.3.14 (repack report): changed —
`BuildInfo.class`, `StyleHook.class`, `StyleHook$Result.class`,
`StyleHook$Layout.class` (javap-proven bytecode-identical; metadata
only), `TapBarHook.class`, `plugin.yml`, `excel-build.txt`; added —
`core/VerifyRetryPlan.class` + `$Verdict` + `$Branch`; total entries
247 → 250.

## 3. DESK PROOFS (detail in TEST-RESULTS.txt)

- **Base gate:** 0.3.14 rebuilt from the drive 0.3.14 sources with
  the 0.3.14 toolchain came out **byte-identical** to the shipped jar
  (545,907 B, SHA-256
  `c87f6005f92539993d05ec8f9f52726a0b3c99a129c36f94153f7fd723774d63`).
  Only then were the 0.3.15 changes applied.
- **Unit tests: 21 suites, 624 checks, 0 failed** — the 20 carried
  0.3.14 suites (577 checks, test files byte-identical, all green
  against the 0.3.15 tree) plus new `VerifyRetryPlanTest` (47 checks):
  the spec numbers (10 ticks, max 2), Mike's exact 4-key case (RETRY
  at retries 0 and 1, FAILED at 2), partial stale reads, foreign-key
  residuals failing at once, both layout-branch gates, branch naming,
  and the full close-rule matrix.
- **Rig proof (desk):** `rig-probe/VerifyRaceRig` drives the REAL
  compiled `StyleProfile` + `VerifyRetryPlan` classes through
  StyleHook's decision sequence against a simulated wand whose item
  lags the write — 21 checks, all passed: (A) race then settle →
  RETRY (not FAILED), exactly 1 retry, 0 closes, retry re-check needs
  no second write; (B) never settles, bar-open chain → RETRY, RETRY,
  FAILED, capped at 2, 0 closes including the FAILED pass; (B2) join
  chain → first deliberate pass closes once, retries close 0;
  (C) foreign diff → FAILED immediately, no retry; (D) layout branch
  gates both ways. Wiring evidence via javap on the shipped classes
  (recorded in TEST-RESULTS.txt). Rig source kept in the build
  folder's `rig-probe/`, not packaged — house pattern.
- **Double-build:** two clean compiles → class trees identical; two
  repacks → **byte-identical** jars.

## 4. ARTIFACT

- `ExcelWandBars-0.3.15.jar` — **550,474 B**, SHA-256
  `61b278b46d8708dff04eb08a0e99a87ee25ac78b19310eeb10ba0b2411b700f2`,
  build id `EXC-WANDBARS-0.3.15-WRV-20261112-01-9bc99f`
  (suffix = first 6 hex of SHA-256 of "EXC-WANDBARS-0.3.15-verify-race").
- Source zip: `ExcelWandBars-0.3.15-src.zip` (95 files under `src/`,
  extracted in this package as `src/`; `tests/` carried alongside).
- Fingerprints: Layer A — name ExcelWandBars, author WinkReyesValley,
  version 0.3.15 real. Layer B — the build id above in `BuildInfo`,
  `excel-build.txt` (verified in the candidate jar), and the enable
  log line. Layer C — house voice in the plugin.yml 0.3.15 note.
  Layer D — channel WRV.

## 5. LIVE PROOF — FAILED (deciding evidence)

- **Player proof FAILED:** Mike tested the installed 0.3.15 build live
  on Bedrock on Nov 12, 2026 at ~12:58 PM CST and reported "Still the
  same" — the tap bar did not show/work as required. The desk rig
  exercised the decision core the jar ships, but the wand, item lag,
  and scheduler were simulated, and no desk result overrides this live
  failure. The verify-race change is NOT player-proven.
- **Superseding diagnosis:** round-2 diagnosis found the real root
  cause to be a `hotbar_inventory_count` desync. ExcelWandBars 0.3.16
  is the follow-on fix for that cause; tracker row 61 remains open
  until Mike's live proof passes on the successor build.

## 6. INSTALL (Nov 12, 2026 — parent hand window, Mike-directed)

Full record in this package's `INSTALLED.txt` and the packet folder's
`OUTCOME.txt` (`~/workspace/wandbars-verify-race-build/packet-20261112/`).

- Installed Nov 12, 2026, parent hand window (Mike-directed "push it
  now" ~12:30 PM CST), PC lane (probe passed attempt 1,
  DESKTOP-IL05DQR; no fallback). JAR-ONLY, placed under the live
  filename `plugins/ExcelWandBars-0.3.4.jar` = 550,474 B / SHA-256
  `61b278b4…` (pull-back EXACT); predecessor held as
  `plugins/ExcelWandBars-0.3.4.jar.held-20261112-pre0315` = 545,907 B /
  `c87f6005…` (pull-back EXACT). Live-jar gate: fresh PC pull
  545,907 B / `c87f6005…` EXACT vs packet.
- Window backup: "WandBars 0.3.15 pre-install 1112" 7.5 GB completed;
  cap-at-6 pruned oldest manual "MagicGate 0.3.2 pre-install 1112";
  total 6 after.
- Boot: Done (20.651s) at 12:49:23 PM CST; Initialized 83 plugins;
  Enabling ExcelWandBars v0.3.15; Build
  `EXC-WANDBARS-0.3.15-WRV-20261112-01-9bc99f`; ExcelGuildGate enabled:
  4 of 5 seals broken, guild LOCKED; SEVERE 0 in latest.log. Arrival
  online 0/10; departure online 0/10 (server left ONLINE, outside the
  maintenance window).
- Close-out status: Mike's live Bedrock proof FAILED after this
  install (section 5), so all 0.3.15 close-out publication is HELD —
  What's New BOTH surfaces and this records publication (Drive +
  GitHub legs per PUSH-NOTES.md) do not proceed until ExcelWandBars
  0.3.16 lands and Mike's live proof passes, and the supervisor
  releases this package.
