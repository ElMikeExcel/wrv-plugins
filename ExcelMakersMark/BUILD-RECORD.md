# ExcelMakersMark 0.1.6 — Windows-safe directory sync (fsync fix lane 4 of 4)

**Date:** Nov 14, 2026 · **Backlog:** P-FSYNC-1 · **Diagnosis:** `~/workspace/mirror-storage-diagnosis-20261114/DIAGNOSIS.md` §4
**Deliverable:** `ExcelMakersMark-0.1.6-lookup-prefilter-OFFLINE.jar` — 209,158 B, SHA-256 `ba4e6bdedf422a8cd8e35bf4bbdf15f58d93ef6103091009c0c9b536c57d9423`
Desk build only: no install, no MANIFEST edit, no live-server contact.

## The defect (from the diagnosis)

Every `HistoryLedger` save ends by fsync-ing the containing directory
(`FileChannel.open(directory)` + `force()`). Legal on Linux (the live
server); impossible via Java NIO on Windows, where the open itself throws
`AccessDeniedException` — so on the PC test-server mirror, history
recovery failed (`History recovery unavailable; archive left untouched`)
while `history\history.mmh2` sat present and intact. Live is unaffected.

## Source tree — identified and verified current

- Tree: `makersmark-recovery/complete-source/`, recovered from
  `makersmark-recovery/lookup-optimization.tar.gz` into this folder's
  `ExcelMakersMark/` (the recovery folder itself untouched).
- It is the fleet's pinned source for the live plugin: the Nov 3 source
  coverage index (`source-coverage-index-v7-main-20261103.md`) records the
  live ExcelMakersMark as **0.1.5-lookup-prefilter-OFFLINE**, jar
  208,757 B / SHA-256 `5cb6a717…`, source = this tree + FINAL-SHA256SUMS.
- Input hashes re-verified here: `inputs/BASELINE.jar` (installed 0.1.4) =
  `204260cc…` (matches the lineage BUILD-RECEIPT and build.py's own
  assert); both copies of the shipped 0.1.5 jar = `5cb6a717…`.
- **Lineage proof for the patched class (Gate B, PASS, byte-identical):**
  the unmodified tree's `HistoryLedger.java` — a JDK-only file — compiled
  with this VM's Temurin JDK 25 (default release; the shipped classes are
  class-file v69) reproduces **all 10** `HistoryLedger*.class` entries of
  the shipped jar byte-for-byte, and `javap -p -c` disassembly is
  identical. The tree's HistoryLedger source IS the live class's source.
- **Gate A (full-jar reproduction of the 0.1.5 candidate via the original
  build.py/rebuild.sh route): NOT reproducible in this VM** — recorded
  honestly. The route compiles `MakerMagicBridge.java` against the
  composed `Magic-LEARNED-OFFLINE.jar`, whose `wrv.magic.evidence.*`
  classes (EffectObserver, WandLoreBridge) exist nowhere in this
  workspace (only stock Magic-11.2.5 is present; the bridge compile fails
  on the missing packages). Mitigation: the candidate is patched FROM the
  shipped jar itself and the patch asserts its changed-entry set, so
  every non-HistoryLedger byte of the candidate is a shipped byte by
  construction; the one recompiled class is lineage-proven by Gate B.

## Idiom sweep

`FileChannel` / `.force(` / `getFD()` / `.sync()` occur in the tree's src
**only** in `history/HistoryLedger.java`. The other `.force(true)` calls
there are on FILE channels (legal on Windows; they stay mandatory). The
single directory-force site is the `forceDirectory(Path)` helper, called
from 3 places: the constructor's recovery path (line 56), and
`checkpoint()`'s link-publish and temp-delete (lines 161–162). One fix
covers all three.

## The change (only this) — `HistoryLedger.java.diff` (20 lines)

`forceDirectory` becomes best-effort: it still opens the directory and
forces it, but an `IOException` from that step (the Windows
`AccessDeniedException` class) is caught and the save proceeds on the
file-level fsync, which runs before it and stays mandatory. Signature
drops `throws IOException`; widened `private` → package-private solely
as a test seam for the same-package tests. Linux behavior unchanged
(the force still executes and succeeds there). Call sites untouched.

No other behavior changes. **No message or log-line changes at all** —
the class has no logging; both mirror ERROR lines disappear on Windows
only because recovery now succeeds. Tree `plugin.yml` bumped
0.1.5 → 0.1.6-lookup-prefilter-OFFLINE (one patch version, lineage suffix
kept) and the tree CHANGELOG gained the 0.1.6 entry.

## Build

Surgical patch in the lineage's own style (`patch.py`, modeled on its
build.py): start from the shipped 0.1.5 jar (hash-asserted), swap in the
freshly compiled `HistoryLedger*.class` set (set-equality asserted
against the jar's entries), replace the plugin.yml version string,
refresh the carried CHANGELOG.md from the tree; fixed zip metadata
(2026-11-14, DEFLATED-9, sorted entries). **Changed entries: exactly 3**
— `HistoryLedger.class`, `plugin.yml`, `CHANGELOG.md`. The 9 inner/record
classes recompiled byte-identical to shipped (the edit touches only the
outer class), which the patch cross-checks; the patch-compiled classes
are also byte-identical to the test-harness compile of the same source.
**Double-build byte-identical** (two patch runs → same SHA-256).

Bytecode verification: `javap -p -c` diff of `HistoryLedger` shipped →
candidate differs ONLY in `forceDirectory` (dropped `throws`, added the
`IOException` exception-table entry + catch). Every other method is
disassembly-identical. plugin.yml read back from the candidate jar:
`version: 0.1.6-lookup-prefilter-OFFLINE`.

## Rig verification still owed (supervisor/install lane)

Boot the candidate on the Windows PC mirror and confirm the two
ExcelMakersMark ERROR lines (`History recovery unavailable…`,
`Configured shared Magic history is unavailable…`) are gone and history
recovery opens against the existing `history.mmh2` — the exact
`AccessDeniedException` path cannot occur on this Linux build host, so
this is the one proof that must happen on Windows.
