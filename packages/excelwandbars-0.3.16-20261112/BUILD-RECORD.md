# ExcelWandBars 0.3.16 (hotbar-counter fix) — build record

Commission: round-2 diagnosis of Mike's Bedrock tap-bar failure after
0.3.15. Diagnosis: `~/workspace/wandbars-tapbar-diagnosis-20261112/DIAGNOSIS-ROUND2.md`
(fix items F1–F5). Mike directed "push the fix asap" (Nov 12, 2026,
1:16 PM CST). Full per-file rationale is in the build folder's
BUILD-NOTES.md (`~/workspace/wandbars-verify-race-build/build-0.3.16/`);
this record is the package summary.

---

## 1. THE CHANGE — F1–F4 (BUILD-NOTES.md maps each to source)

- **F1 (root fix as diagnosed in round 2):** `StyleHook`'s layout
  configure writes `hotbar_inventory_count` alongside `hotbar_count`
  (gated `style != WandStyle.ORIGINAL`), and `RankUpHook`'s template
  upgrade key list gains `"hotbar_inventory_count"`. New pure core:
  `core/ApplyCoalescePlan.java`.
- **F2:** `layoutFor` records the wand's own
  `hotbar_inventory_count` (`hic=`) and first spell-map divergence
  (`firstDiff=`) on FAILED/RETRYING layout lines.
- **F3:** `TapBarHook` tryOpen refusals, RenderedExitGuard cancels,
  and join-grace closes log at INFO; no silent refusals.
- **F4:** applies coalesce before the ApplyGuard when a verify retry
  is pending or an apply is in flight; self-apply stamp suppresses
  the apply's own WandActivatedEvent echo (2-tick window).
- Resources: `plugin.yml` (0.3.16), `excel-build.txt`,
  `BuildInfo.java`. `config.yml` and `spellbook.yml` byte-identical
  to 0.3.15's — install was JAR-ONLY.

Files changed vs 0.3.15: NEW `core/ApplyCoalescePlan.java`; EDITED
`StyleHook.java`, `RankUpHook.java`, `TapBarHook.java`,
`BuildInfo.java`; RESOURCES `plugin.yml`, `excel-build.txt`.

## 2. DESK PROOFS (detail in TEST-RESULTS.txt)

- **Base gate:** 0.3.15 rebuilt from its sources came out
  byte-identical to the shipped 0.3.15 jar (550,474 B, SHA-256
  `61b278b46d8708dff04eb08a0e99a87ee25ac78b19310eeb10ba0b2411b700f2`).
- **Unit tests: 22 suites, 643 checks, 0 failed** — 21 carried
  suites (624 checks) + new `ApplyCoalescePlanTest` (19 checks).
- **Rig (desk sim):** `rig-probe/HotbarCounterRig` — 40 checks, all
  PASS on Mike's actual saved student-wand fixture; carried
  `VerifyRaceRig` (21 checks) also passes. NOTE: round-3 diagnosis
  later disqualified this desk sim as evidence for the counter
  defect class (it never ran a real Magic Wand object); see the
  0.3.17 record. The rig's PASS lines stand as history of why
  0.3.16 was believed.
- **Double-build:** byte-identical jars.

## 3. ARTIFACT

- `ExcelWandBars-0.3.16.jar` — **553,302 B**, SHA-256
  `897e589c93be86161400755f233cdcf10324c0ea9d2b021fb2690a5ef17a5055`,
  build id `EXC-WANDBARS-0.3.16-WRV-20261112-01-6a4cab`.
  The jar itself is NOT in this staged package (per the staging
  instruction / house rule for this package); it is deposited via
  the Drive leg in PUSH-NOTES.md.
- Source zip: `ExcelWandBars-0.3.16-src.zip` — 288,252 B, SHA-256
  `e3998a1da1ad299a684afd278043a8248eb7e874aa5cd78c2f74043bdaa2bf3c`
  (96 files under `src/`, extracted in this package as `src/`;
  `tests/` carried alongside).

## 4. INSTALL (Nov 12, 2026 — parent hand window)

Full record in this package's `INSTALLED.txt` and the packet
folder's `OUTCOME.txt`
(`~/workspace/wandbars-verify-race-build/packet-0316-20261112/`).

- Installed Nov 12, 2026, 13:48 CST boot, parent hand window,
  PC lane. JAR-ONLY under live filename
  `plugins/ExcelWandBars-0.3.4.jar` = 553,302 B / `897e589c…`
  (pull-back EXACT); predecessor 0.3.15 held as
  `plugins/ExcelWandBars-0.3.4.jar.held-20261112-pre0316`.
- Boot: Done (20.251s); Enabling ExcelWandBars v0.3.16; build
  `EXC-WANDBARS-0.3.16-WRV-20261112-01-6a4cab`; 83 plugins;
  guild LOCKED 4 of 5; SEVERE 0.
- Backup: "WandBars 0.3.16 pre-install 1112"; cap-at-6 pruned
  "MagicGate stage rename pre 1112"; total 6.

## 5. LIVE PROOF / HOLD RELEASE

0.3.16 was superseded the same day by 0.3.17 (round-3 diagnosis:
Magic's path owns `hotbar_count`, so the F1 counter writes could
never take effect on a real Wand). Publication of this package was
held under decision D29 until Mike's live proof passed on the
successor build — it PASSED Nov 12, 2026, 6:45 PM CST (on 0.3.17),
so the hold is released. This package is STAGED only; no GitHub
push, Drive upload, or MANIFEST edit was made by this staging lane.
