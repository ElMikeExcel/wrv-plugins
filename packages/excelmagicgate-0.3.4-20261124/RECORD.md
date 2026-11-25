# Re-base — ExcelMagicGate wand-owner repair onto live 0.3.3 — Nov 24, 2026

**Lane:** RE-BASE BUILD (parent-dispatched). Finish line = FILED (D212).
NO live install. The 0.2.1 packet stopped at its gate in the
hand-20261124-0200-d189 window because live runs 0.3.3 under the
0.2.0 filename; this lane ports the repair onto 0.3.3 as 0.3.4.

## 1. 0.3.3 source provenance — PROVEN three ways

Source of truth: ~/workspace/board-r365/magicgate-grant/
ExcelMagicGate-0.3.3/ExcelMagicGate/ (the Nov 15, 2026 grant-
verification build tree; build EXC-MAGICGATE-0.3.3-WRV-20261115-01-a33f01).

- Fresh pull of the live jar (Homebase scp, Nov 24, 2026 ~02:19 CT):
  plugins/ExcelMagicGate-0.2.0.jar = 247,765 B / SHA-256
  4894969672002da96ee3b1220efb7564b194ef3cf60f4aaacea8ce73cf7d066f;
  its plugin.yml reads `version: 0.3.3`.
- The build tree's shipped candidate (and its -repro.jar):
  247,765 B / 48949696… — identical.
- A pristine rebuild of the untouched source tree in this lane
  (work/base-0.3.3, stock build.sh): 48949696… — byte-identical.
- Baseline suite on the pristine tree in this environment:
  282 passed / 0 failed without fixtures; the full run in the
  0.3.4 tree (fixtures present) carries all 305 checks.

Receipts: ~/workspace/magicgate-rebase-20261124/RECEIPTS.md.

## 2. The port (0.3.4)

Build tree: ~/workspace/magicgate-rebase-20261124/build-0.3.4/
ExcelMagicGate/ — a copy of the proven 0.3.3 tree plus:

- NEW DownedMarkOwner.java — the 0.2.1 pure matcher, logic
  byte-for-byte (header comment updated to 0.3.4).
- NEW WandOwnerRepair.java — the 0.2.1 repair, logic
  byte-for-byte. Its wand-discovery mirror claim re-verified
  against 0.3.3: MagicFloorHook.looksLikeRealWand in 0.3.3
  (line 1247) is the same isBound/getTemplateKey/getSpells bar.
- ExcelMagicGate.java wiring, same seams as 0.2.1: field +
  construction in onEnable; a NEW synchronous
  PlayerJoinEvent handler at EventPriority.LOWEST
  (onJoinRepairWandOwner); the repair re-run at the head of the
  existing 40-tick delayed join reconcile; the repair re-run in
  the 1-tick activation reconcile. (0.3.3 has no separate
  onItemHeld body change — both held/swap events funnel into
  scheduleActivationReconcile, exactly as in 0.2.1.)
- config.yml (bundled): the `wand-owner-repair:` block inserted
  after the floor-reconcile section — the same position the
  0.2.1 bundled config used.
- plugin.yml: version 0.3.4 + one appended description sentence
  (the 0.2.1 pattern).
- BuildInfo / excel-build.txt: EXC-MAGICGATE-0.3.4-WRV-20261124-01-wandfix.
- build.sh: header + output default + pinned jar date updated.

### Diff scope — the repair and nothing else

Source diff vs the pristine 0.3.3 tree: exactly the files above
(2 new classes; ExcelMagicGate.java +33/-0 wiring lines;
BuildInfo, plugin.yml, config.yml, excel-build.txt, build.sh,
tests). Class diff of the built jars (baseline rebuilt jar vs
0.3.4 jar):

- Differ behaviorally: ExcelMagicGate.class (wiring),
  BuildInfo.class (stamp), plugin.yml / config.yml /
  excel-build.txt (version + repair block).
- New: DownedMarkOwner.class, WandOwnerRepair.class.
- Byte-differ but bytecode-IDENTICAL (javap -p -c compared):
  ExcelMagicGate$1.class and ExcelMagicGate$ApplySteps.class —
  same size, same instructions; only their LineNumberTable
  entries shifted (e.g. 604-606 -> 637+) because the inserted
  handler added lines above them in the shared compilation
  unit. Debug metadata, not behavior.
- EVERY rank-system class is byte-identical to the 0.3.3
  baseline: CeilingPlan, CreditPolicy, EarnedRank, EarnedStore,
  MagicCreditHook, MagicFloorHook, PlacementCalculator,
  RankFloor, SpellTemplateGate, WandInventorySync,
  WandLoreHook, StageApplier, StagePlan, StageMessages,
  FloorPlan, GrantList, BridgeState (+ all nested classes).

### Tests

- Full suite: **315 passed / 0 failed** = the 305 carried 0.3.3
  checks (incl. placement + inventory fixtures) + the 10
  wand-owner matcher checks ported verbatim from the 0.2.1
  suite (incident string, raw color-coded mark, clean owner,
  foreign marked wand both directions, near-miss suffix/prefix,
  null/empty, Bedrock dotted name, prefix-literal pin).
- One carried check updated: the build-fingerprint pin
  ("EXC-MAGICGATE-0.3.3-WRV-20261115-01-") rolled forward to
  0.3.4 — the standard lineage-pin roll (same as the 0.2.1 and
  Downed 1.0.4 builds did); it failed once pre-roll as designed.
- Double-build byte-identical: ExcelMagicGate-0.3.4.jar
  252,773 B / SHA-256
  624f6f00193dcdda36d92c5bcb1c510ddcc4e8993a706dfaf9ece89b016e1eae.

### Proof level (honest)

Mirror end-to-end validation remains impossible for the same
reason as 0.2.1: the mirror's Magic is the WRV-patched build
(byte-identical to live) whose RawWandLoadBridge gate throws
"raw item baseline not durably confirmed" for any command-minted
wand on a fresh mage, so the repair's wand construction cannot
be exercised there. Substitute stack (unchanged from the 0.2.1
record, which see): unit suites pin the matcher to the incident
string; join-order trace (LOWEST synchronous repair precedes
Magic's next-tick checkWand validation and Downed's NORMAL join
handling); Magic 11.2.5 bytecode semantics (boundDisplayName
defaults true; takeOwnership re-stamps owner + ownerId). The
ported repair code is byte-for-byte the 0.2.1 logic, and on live
it uses the same reflective paths 0.3.3 already exercises
against real (registered) wands every join.

## 3. Contaminated-wand data check — ONE live hit, and it matters

Method + full receipts in RECEIPTS.md. Summary: findstr sweep
for the literal `DOWNED` over (a) all live Magic data
(plugins\Magic — mage saves are plugins\Magic\data\players\*.dat)
and (b) all world playerdata (world\players\data\*.dat, which
holds both inventory and ender chest).

- Mage saves: 11 live .dat files; exactly ONE contaminated:
  MikeExcel's (7401bc22-…). Line-level: the stored wand copy
  still reads `owner:"✝ DOWNED MikeExcel"`,
  mana_timestamp 1795497469460L — the Nov 23 23:17:49 incident
  save, never overwritten since. His CARRIED wand was restored
  clean by hand Nov 23, but Magic's stored copy still carries
  the mark; at his next login Magic loads this record. Whether
  the deletion path re-fires depends on Magic's internal load
  order vs the restored carried item — the repair's LOWEST join
  scan + delayed recheck are the safety net, and the packet
  names his next login as the post-install proof (heal log line
  or clean load; stored owner verified clean after his next
  logout save).
- Playerdata: ZERO hits — no other player's carried wand is
  contaminated, and no other mage save is.
- Not covered: wands parked in world containers (base chests);
  the repair's activation leg covers those at take-in-hand.

Conclusion: the repair half is NOT merely hypothetical — one
real contaminated owner string still sits in live data today.
ExcelDowned 1.0.4 (installed Nov 24) prevents any new one.

## 4. Filename investigation — why 0.3.3 lives in a "0.2.0" jar

Timeline from MANIFEST-ARCHIVE install records:

- Nov 8-9, 2026: genuine 0.2.0 installed as
  ExcelMagicGate-0.2.0.jar (173,699 B / 2e576bf4…).
- Nov 9, 2026 (parent hand window): the 0.2.1 stage-template
  refresh (build …20261109-01-d34112 — a different 0.2.1 from
  the wandfix build) was placed as a "jar swap only under the
  live filename ExcelMagicGate-0.2.0.jar"; the genuine 0.2.0
  was kept as ExcelMagicGate-0.2.0.jar.bak-0.2.0. THE SPLIT
  STARTS HERE.
- Every later install repeated the step verbatim: 0.3.0 and
  0.3.0-amended (Nov 10), 0.3.1 (Nov 11), 0.3.2 (Nov 12), 0.3.3
  (Nov 15) — each record states "placed under the live filename
  ExcelMagicGate-0.2.0.jar", outgoing kept as a suffix variant
  (.held-<date>-pre<ver> / .bak-<date>-pre<ver>) of the same
  base name.

So the filename is a standing swap convention, adopted Nov 9
and followed five times since: exactly one MagicGate jar ever
exists in plugins/ (no duplicate-plugin risk), rollback copies
are uniform suffix variants of one base name, and all install
gates are hash-based — the filename was never load-bearing.
(The same under-the-live-filename convention appears elsewhere
in the fleet's records, e.g. ExcelHalloweenToggles 0.1.1 and
the BossTrial candidate staging rule.)

RECOMMENDATION (carried in the packet): do NOT correct the
filename at the 0.3.4 install. Place 0.3.4 under
ExcelMagicGate-0.2.0.jar per convention. A rename is
functionally free (Bukkit identity comes from plugin.yml) but
breaks five records of uniform rollback bookkeeping for zero
gain; the confusion that stopped the 0.2.1 wandfix packet came
from its premise being written off stale lineage instead of a
fresh pull — the fix for that is the gate discipline this
packet follows, not a rename. If filename honesty is wanted,
it should be one deliberate fleet-wide rename decision by
Mike with records updated in the same pass.

## 5. Filing

- Packet: ~/workspace/magicgate-rebase-20261124/INSTALL-PACKET.md
  (lint 8749 B / max line 79 / non-ASCII 54).
- MANIFEST: new PENDING entry "ExcelMagicGate 0.3.4
  (wand-owner repair at join, re-based on 0.3.3)" added directly
  below the 0.2.1 entry; the 0.2.1 entry keeps its full STOPPED
  evidence and gains one appended SUPERSEDED line pointing here.
- Candidate jar + source zip in this folder (hashes in the
  packet). Marker on install:
  ~/workspace/magicgate-rebase-20261124/INSTALLED.txt.
- NOT installed by this lane; no players messaged.
