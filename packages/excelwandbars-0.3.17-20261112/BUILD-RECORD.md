# ExcelWandBars 0.3.17 (Shape A: effective-page-count tap bar) — build record

Commission: round-3 diagnosis of the tap-bar defect. Governing
diagnosis: `~/workspace/wandbars-tapbar-diagnosis-20261112/DIAGNOSIS-ROUND3.md`
— on a real Magic `Wand`, `hotbar_count` is owned by the player's
path and `hotbar_inventory_count` is re-derived on every
`buildInventory`, so no WandBars counter write can take effect.
Mike's ruling (Nov 12, 2026, ~4:22 PM CST): SHAPE A — the tap bar
fits the wand's path size. Full rationale is in the build folder's
BUILD-NOTES.md (`~/workspace/wandbars-verify-race-build/build-0.3.17/`);
this record is the package summary. Rig evidence of record:
RIG-RESULTS.md in this package.

---

## 1. THE CHANGE — 7 files (BUILD-NOTES.md maps each)

- `core/BarLoadout.java` — new capped `layout(..., maxPages)`
  overload; 2-arg delegates uncapped (existing callers unchanged).
  Picks past the cap park in the book region in slot order; loadout
  data keeps all picks.
- `core/PageLayout.java` — new `packCapped(...)`; `pack()` untouched.
- `LoadoutHook.java` — capped layout overload; admin `describe()`
  view computes the capped layout.
- `StyleHook.java` — `layoutFor` targets the effective
  (path-resolved) page count from `impl.getHotbarCount()`; verifier
  is a fixed point by construction (`ok` = same spell map); BOTH
  counter writes removed (pre-0.3.16 `hotbar_count` write and
  0.3.16's F1 `hotbar_inventory_count` write).
- `RankUpHook.java` — F1 `"hotbar_inventory_count"` template copy
  reverted; `"hotbar_count"` stays.
- Resources: `BuildInfo.java`, `plugin.yml` (0.3.17,
  YAML-validated), `excel-build.txt`. `config.yml` /
  `spellbook.yml` byte-identical to 0.3.16's — JAR-ONLY.
- Kept from 0.3.16: F2 detailed FAILED lines, F3 tryOpen logging,
  F4 ApplyCoalescePlan coalescing.

First-candidate incident (recorded in BUILD-NOTES.md): the first
0.3.17 candidate (`db07038a…`, 553,822 B) carried an invalid
plugin.yml and was rejected by the mirror; it is NOT a deliverable
and must never be installed. Only `949e9cd4…` shipped.

## 2. DESK + RIG PROOFS (detail in TEST-RESULTS.txt / RIG-RESULTS.md)

- **Base gate:** 0.3.16 rebuilt from its sources came out
  byte-identical to the shipped 0.3.16 jar (553,302 B, SHA-256
  `897e589c93be86161400755f233cdcf10324c0ea9d2b021fb2690a5ef17a5055`).
- **Unit tests: 23 suites, 669 checks, 0 failed** — 22 carried
  suites (643 checks, unchanged) + new `ShapeAPageCapTest`
  (26 checks).
- **Real-Magic mirror rig (evidence of record):** STEP 1
  reproduced the live failure on shipped 0.3.16 (FAILED
  `pages=2/3`, tryOpen refusing); STEP 2 on 0.3.17 — student wand
  `pages=2/2 ok=true`, tap bar opened, fixed point across relog +
  churn, saved data 80/80 spells with page-3 picks parked at
  16–22; STEP 3 — master `3/3` + opens, beginner `1/1` + opens,
  sneak bars `2/2`, ORIGINAL `2/2`.
- The 0.3.16 desk sim was NOT used as evidence (disqualified by
  round 3; retired, not re-based).
- **Double-build:** byte-identical jars.

## 3. ARTIFACT

- `ExcelWandBars-0.3.17.jar` — **553,827 B**, SHA-256
  `949e9cd48790cd10bb2861ce32f4cc3442410aee219e91bbc48f6cb526330c9b`,
  build id `EXC-WANDBARS-0.3.17-WRV-20261112-01-9d0b77`.
  The jar itself is NOT in this staged package (per the staging
  instruction / house rule for this package); it is deposited via
  the Drive leg in PUSH-NOTES.md.
- Source zip: `ExcelWandBars-0.3.17-src.zip` — 283,848 B, SHA-256
  `e6758a449fb0cf182912330f877787148af87647f91c1b073a1315cdba002499`
  (96 files under `src/`, extracted in this package as `src/`;
  `tests/` carried alongside).
- Exactly 10 entries differ from the shipped 0.3.16 jar.

## 4. INSTALL (Nov 12, 2026 — parent hand window)

Full record in this package's `INSTALLED.txt` and the packet
folder's `OUTCOME.txt`
(`~/workspace/wandbars-verify-race-build/build-0.3.17/packet-0317-20261112/`).

- Installed Nov 12, 2026, 18:32 CST boot, parent hand window,
  PC lane (no fallback). JAR-ONLY under live filename
  `plugins/ExcelWandBars-0.3.4.jar` = 553,827 B / `949e9cd4…`
  (pull-back verified); predecessor 0.3.16 held as
  `plugins/ExcelWandBars-0.3.4.jar.held-20261112-pre0317`.
- Boot: Done (21.245s); Enabling ExcelWandBars v0.3.17; build
  `EXC-WANDBARS-0.3.17-WRV-20261112-01-9d0b77`; 83 plugins;
  guild LOCKED 4 of 5; SEVERE 0.
- Window backup: "WB 0.3.17 + Summons 0.1.7 pre 1112" (shared with
  the chained ExcelSummons 0.1.7 install); cap-at-6 pruned
  "WandBars 0.3.15 pre-install 1112"; total 6.

## 5. LIVE PROOF — PASSED

Mike's live Bedrock proof PASSED Nov 12, 2026, 6:45 PM CST.
That pass released the D29 hold on this package and on the
0.3.15 / 0.3.16 records packages. This package is STAGED only;
no GitHub push, Drive upload, or MANIFEST edit was made by this
staging lane.
