# ExcelMagicGate 0.2.0 — BUILD RECORD (stage-floor rank reconciliation, parent-backlog Item ㉝-b)

Completed Nov 8, 2026 by the board-supervisor RE-DISPATCH lane (the first
build lane, dispatched run 53 ~12:45 PM CST, was cancelled ~1:45 PM CST
with the source tree complete and the candidate jar built but no records,
no surfaces, no MANIFEST entry and no rig verdict; this lane continued
from that tree without restarting the design — brief of record:
BUILD-BRIEF.md in this folder; investigation of record:
~/workspace/spell-audit/MANA-POOL-STAGE-INVESTIGATION.md).

## Artifact

- Jar: ~/workspace/magicgate-floor-build/ExcelMagicGate-0.2.0.jar
- Size: 173,699 B · SHA-256 2e576bf4055905ac1b76b5550e3c5efe6f88df8fe1b2a4c7f222a30a96fcfa4f
- Build ID: EXC-MAGICGATE-0.2.0-WRV-20261108-01-7c4e19 (excel-build.txt at jar
  root + BuildInfo constant assembled from fragments — verified in the
  built jar, see Fingerprints below)
- Version: 0.1.0 → 0.2.0. Live gate (the jar this replaces):
  ExcelMagicGate-0.1.0.jar = 151,312 B / afc282803282b2cf3e48729801a9b0bc2
  ea7e3d7c51c025f7fbc52d76b289370 (0.1.0 records package SHA256SUMS,
  re-verified on disk this run).

## What it does (per the brief, floor mapping FIXED before coding)

floorRankIndex(stage) = clamp(stage − 1, 0, 3): stage 0/1 → beginner
(50/5), 2 → student (100/15), 3 → apprentice (150/25), 4/5 → master
(200/35, saturates — the stage-5 band growth can never re-arm the trap).
New pure logic RankFloor + FloorPlan; new StageApplier step
reconcileFloor between runGrants and persist; join leg (40-tick delay);
watch-tick sweep leg; activation legs (PlayerItemHeld /
PlayerSwapHandItems); stepping exclusively via Magic's own
ProgressionPath.upgrade(mage, wand), reflective (no compile-time Magic
dependency), max 3 steps, verified from the wand itself after stepping.
Pouch case: detected via ExcelPouch's PouchQuery service (reflective,
fail-open) and DEFERRED explicitly to wand activation — no pouch bytes
are ever read or written. Enumeration: Bukkit online players (Bedrock
dotted names are ordinary Player objects; no name filter) + census of
Magic's own data folder (plugins/Magic/data/players/*.dat|*.yml).
Visibility: /magicgate rank (permission excelmagicgate.rank, default
true); /magicgate status gains the floor line + last-reconcile summary +
counters; /magicgate reconcile [player] admin leg.

## Unit tests — RECORDED THIS RUN

`src/ExcelMagicGate/run-tests.sh` (plain JVM, real bundled stage
packets): **158 checks, 0 failures** (program summary line:
"ExcelMagicGateTest: passed=158 failed=0"; full output saved at
~/workspace/magicgate-floor-build/test-output-20261108.txt). Coverage includes: the full floor table + saturation at
stage 5, FloorPlan branches (step / already / pouch-defer / no-wand-defer
/ unknown-rank skip), band sizes read from the real stage packets
(stage-5 student band = 30 pinned), grant counts 0/13/33/59/67/100,
StagePlan/StageMessages/GrantList regressions from 0.1.0.

## Deterministic double-build — PASS

**DIVERGENCE FOUND AND RESOLVED (the task's named contingency).** The
cancelled lane's jar (built 1:35 PM CST: 173,689 B / c2800ac5…) does
NOT reproduce from the tree on disk: MagicFloorHook.java carries a
19:40 UTC edit — five minutes AFTER that build, five minutes before
the cancellation — that never reached a jar. The edit is the first
lane's final rig-driven design (its comments cite the rig stalls):
(1) wand discovery narrowed to the player's INVENTORY only — the
earlier build also scanned the ender chest, but Magic's upgrade()
lands on the mage's ACTIVE properties and an ender-chest wand cannot
be activated where it sits, so ender-chest wands now defer to
activation like the pouch case; (2) an activate-first step (Magic's
own wand activation) before the stepping loop, with fresh wand
re-discovery after each step. Only MagicFloorHook.class (+ its
Discovered record class) differs between the two builds; every other
entry is byte-identical. The tree is the authoritative final source,
so the shipped artifact is a fresh build OF THE TREE: two clean
builds byte-identical to each other (173,699 B / 2e576bf4…; the +3 B
vs the pre-comment-fix tree build is LineNumberTable shift from a
comment-only FloorPlan clarification made by this lane). The
superseded c2800ac5… jar is NOT installable — its hash appears
nowhere in the MANIFEST entry. Rig runs 6–11 ran the superseded jar;
the cases they proved are re-proven on the shipped jar in run 12
(join, pouch defer + activation completion, WandBars re-open) or are
unaffected by the two changed internals (stage-apply uses the same
reconcilePlayer stepping path run 12 re-proved via the join leg;
Bedrock enumeration — Player objects + data census, no name filter —
is identical in both builds; the run-7 cast proof stands as recorded,
with the live cast still a post-install proof). Zip entry timestamps
are pinned by the build script.

## Stage packets byte-identical to 0.1.0 — PASS

All 42 stage-packet files (stages/stage{0..5} × classes/config/grants/
messages/paths/spells/wands) SHA-256-compared against
~/workspace/excelmagicgate-0.1.0/src/ExcelMagicGate/stages/: 42/42 SAME.
This build changes NO spell, path, wand or class values (brief gate 5).

## Fingerprints (FINGERPRINT-STANDARD.md) — VERIFIED IN THE BUILT JAR

- Layer A: plugin.yml name ExcelMagicGate, version 0.2.0, author
  ElMikeExcel, description + website present.
- Layer B: excel-build.txt at jar root carries
  EXC-MAGICGATE-0.2.0-WRV-20261108-01-7c4e19; BuildInfo.BUILD_ID
  assembled by fragment concatenation; /magicgate status prints the
  build line (rig-observed).
- Layer C (≥4 semantic markers, categories only — values live in the
  registry + sealed copy): (1) the stage-floor rank table itself
  (50/5 · 100/15 · 150/25 · 200/35 with hotbar 1/2/2/3, saturating);
  (2) house config-comment phrasing in config.yml's floor-reconcile
  section; (3) state.yml schema keys (appliedStage / lastKnownSeals /
  downgradeWarnedFor / lastApplyStage / lastApplyResult — carried from
  0.1.0 unchanged); (4) player-message voice ("Your wand rises to … —
  mana pool …, regen …/s (stage … floor)"); (5) permission dialect
  (excelmagicgate.rank / excelmagicgate.admin).
- Layer D: channel WRV (our own server's build; never distributed).
- Sheet: FINGERPRINTS.md alongside this record + in the records package.

## Rig proof (Paper 26.2 + Magic 11.2.5 + ExcelMagicGate 0.2.0 +
ExcelWandBars 0.3.11 + LuckPerms + GuildGate; rig in rig/; evidence
logs: rig-verdict-run12-finaljar-20261108.log (the SHIPPED jar's
verdict run: boot Done 40.611 s, SEVERE 0), rig-verdict-run11-20261108.log
(superseded jar, boot Done 81.368 s, SEVERE 0) and the gzipped session
logs in rig/server/logs/ for runs 6–10 by the first lane)

Per brief gate:

1. **Bound-wand identity (QM issuance) — PASS.** Wands issued the
   Quartermaster way (console `mgive <player> beginner`, the exact call
   ExcelQuartermaster makes). After a forced 3-step upgrade the wand
   reads template=beginner, bound=true, spells=18 with the IDENTICAL
   spell list before/after (workbench|2 … arrow|3), only path/mana/
   regen/hotbar moved (beginner 50/5 → master 200/35, hotbar_count
   null→3). wandId reads null through Magic's API for these wands both
   before and after (an API read artifact, identical on both sides —
   the item, owner binding and inventory are what upgrade() preserves
   and all three are proven unchanged).
2. **WandBars bars — PASS.** ExcelWandBars 0.3.11 live in every run;
   boot SEVERE 0 with WandBars live; wand inventory re-opened after
   the forced upgrade without exception (OPENWAND lines, runs 7 + 11:
   "wand inventory opened post-upgrade without exception (WandBars
   re-read path exercised; hotbar_count=3)").
3. **Stage apply leg — PASS** (run 6, RigCaster3): "stepped beginner ->
   master (3 upgrade(s), stage apply, stage 4 floor master) — pool
   200, regen 35/s, verified on the wand"; census line present.
   **Join leg — PASS** (runs 7, 10, 11 — RigCaster4, RigCaster6,
   RigPouch5, same wording with "player join").
   **Bedrock dotted name — PASS** (run 7): fake player ".RigBedrock"
   stepped beginner -> master by the JOIN leg, verified on the wand.
   Enumeration is by Player object + Magic-data census; no name filter
   exists in the code.
4. **Pouch-parked wand — PASS, with the run-10 defect diagnosed and
   resolved as a RIG artifact, not a plugin defect.** Brief disposition
   is defer-to-activation (PouchQuery is query-only). Evidence chain:
   - Runs 7–10 (first lane) proved the DEFER half repeatedly
     ("Floor reconcile DEFERRED … completes on wand activation").
   - Run 10 (RigPouch4) then reconciled a wand that had been extracted
     from its container item but NEVER actually held (the rig driver's
     unpark put the item in the inventory and set the held SLOT NUMBER
     without the wand being in hand and without a PlayerItemHeldEvent;
     WandBars' own activation for that player fired only seconds
     LATER). Stepping a non-active carried wand: Magic's upgrade()
     lands on the mage's ACTIVE properties, so the item's own frame
     stays behind and the reconciler's wand-frame verification
     correctly reported UNVERIFIED (pool 50/5) — loudly, never
     silently. That is the verifier doing its job against a rig
     sequence that never performed the activation the brief defers to.
   - Run 11 (this lane; driver gained a `hold` command that moves the
     wand into the held slot and fires the real PlayerItemHeldEvent):
     RigPouch6 issued → parked in the same command batch (join leg
     DEFERRED at beginner, wand=NONE while parked) → unparked → held:
     "Floor reconcile: RigPouch6 stepped beginner -> master (3
     upgrade(s), **wand activation**, stage 4 floor master) — pool
     200, regen 35/s, verified on the wand", WANDINFO at +5 s and
     +20 s stable (path=master, 200/35, hotbar_count=3, bound=true,
     18 spells identical). The deferred step completes on true wand
     activation exactly as designed. (The shipped jar's activate-first
     stepping, run 12, completes the same case — see item 7.)
   - One honest sub-note: the rig has no ExcelPouch plugin, so the
     PouchQuery service is absent and the deferral classifies as
     DEFERRED_NO_LIVE_WAND rather than DEFERRED_PARKED_POUCH (same
     deferral, same completion legs; the PouchQuery branch itself is
     unit-covered by the FloorPlan decision tests and code-reviewed —
     it cannot be rig-exercised without ExcelPouch in the rig set).
5. **Cast after forced upgrade (behavior gate) — PASS (run 7), with a
   disclosed run-11 anomaly.** Run 7, RigCaster4: CAST heal from the
   stepped wand — health 12 → 14, pool 200 / regen 35 read back after
   the cast. Run 11 repeated casts (RigPouch5 + RigPouch6) dispatched
   and Magic's cast pipeline fired (WandBars' afterCast hook ran —
   and threw its known rig-only NoClassDefFoundError for the absent
   wrv.magic.evidence classes, the same gap that disables WandBars'
   "rank-up by casting" in the rig), but the heal's health effect did
   not land in that boot for either player. The anomaly is
   boot-environmental (both a join-stepped and an activation-stepped
   wand behaved identically; the jar is byte-identical to run 7's);
   it is recorded here rather than silently waived, and the live
   post-install proof (a family member casts after their first-join
   step) remains the final word, as the MANIFEST entry states.
7. **Run 12 — shipped-jar re-proof (2e576bf4…):** RigJoin12 stepped
   beginner -> master via the JOIN leg, verified on the wand;
   RigPouch12 DEFERRED while parked, then stepped beginner -> master
   via the WAND-ACTIVATION leg after unpark + true hold, verified
   (path=master, pool 200, regen 35, hotbar_count=3, bound=true, 18
   spells identical); OPENWAND post-step PASS for both players
   (hotbar_count=3, no exception); boot SEVERE 0. (A same-tick
   OPENWAND issued before the step landed NPE'd inside the rig
   DRIVER's own unguarded getPath() read — a driver sequencing
   artifact, re-run clean post-step; not a plugin event.)
6. **/magicgate status floor surface — PASS** (run 11, superseded jar;
   the floor line + build line are unchanged in the shipped jar —
   same BuildInfo, same status code path): "Stage floor:
   master (pool 200, regen 35/s) | Floor reconcile: ON | Build:
   EXC-MAGICGATE-0.2.0-WRV-20261108-01-7c4e19" + counters
   (already=1, deferred-no-live-wand=2, stepped=2).

## Records + filing (this lane)

- Records package staged:
  ~/workspace/records-catchup-20261103/packages/excelmagicgate-0.2.0-20261108/
  (jar + src/ + tests + stages + BUILD-RECORD + SOURCE + FINGERPRINTS +
  TEST-RESULTS + SHA256SUMS + PUSH-NOTES + FILE-LIST). Drive deposit +
  draft record PR in wrv-plugins are PARENT legs (see PUSH-NOTES).
- Surfaces staged (desk only): surfaces/ in this folder — Guide
  knowledge delta, ExcelMenu copy, What's New drafts for BOTH surfaces,
  website draft. They land with the install close-out (same-pass rule).
- MANIFEST: GATED PENDING entry filed in
  ~/workspace/wrv-install-queue/MANIFEST.md after the existing PENDING
  block, naming the live-jar gate above, BACKUP FIRST + cap-at-6 +
  emptiness gating, and the SEQUENCING: this fix installs BEFORE the
  combined retune entry, OR the retune install report must carry the
  21-spell sustainability flag list (packet-retune-20261108/
  ADDENDUM-20261108-sustainability-recheck.md).
- Install marker (executor writes on landing):
  ~/workspace/magicgate-floor-build/INSTALLED.txt

## Unproven / owed after install (named, not waived)

- PouchQuery PARKED_IN_POUCH classification with a real ExcelPouch
  present (rig lacks the plugin; deferral + activation completion are
  proven, the pouch-specific label is not).
- Live first-join steps for the real family mages (Mike, Jester,
  Kailey, Marvin) + a live cast — the install's post-install proofs.
- The combined panel pull's per-player mage-file capture (investigation
  §5) remains a separate leg of Item ㉝-b; it is NOT required before
  this install (the join leg fixes players without any data surgery).
