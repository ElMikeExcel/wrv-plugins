# ExcelProtect 0.1.6 "Recovery" — BUILD RECORD

Built Nov 6, 2026 (Iris, subagent session cda8e30d) from the shipped
0.1.5 tree (`~/workspace/protect-clarity-build/`, jar of record
225,181 B, SHA-256 `c9b5dc1f58cd67de7db60e67e82b8e1cd8cb83b6ed0207ae9597bca0fa3ed5cb`).
Purpose: close the rollback gap list from
`~/workspace/protect-rollback-proof/VERDICT.md` against Mike's standing
ambition — "ExcelProtect is the ultimate version of CoreProtect or any
tool like it" (total observability; replaces CoreProtect outright).

**Jar of record:** `ExcelProtect-0.1.6.jar` — **234,451 B**, SHA-256
`097a4cbc836ef7252db3510770673c7173531eb900c5500d4d6d26c39566b028`.
Deterministic double-build byte-identical (`ExcelProtect-0.1.6-repro.jar`
alongside, same hash). Fingerprint (Layer B, injected at build time,
gate-verified post-build): `EXC-PROTECT-0.1.6-WRV-20261106-01-c016rf`.
plugin.yml: version 0.1.6, **authors: [Iris, Mike]** (Nov 5 authorship
ruling; 0.1.5 still said "Excel").

**Tests:** 77/77 green — the full carried 0.1.5 suite (66) plus 11 new
0.1.6 tests (parseLocal forms + loud-error surface, from/to window
acceptance, container target selection incl. the between-transactions
case, container undo line shape/selection, entity line selection +
mark, death-snapshot selection + counts, slot-preserving payload list,
WorldEdit line shape + rollback consumption).

## Features — verdicts

1. **parseLocal fix + loud filter errors — SHIPPED.**
   `Exporter.Query.parseLocal` now accepts ISO-with-T (the form the
   pre-0.1.6 code destroyed by stripping the T before parsing),
   space-separated, and bare-date (start of day) forms. A malformed
   value on a KNOWN filter key is recorded on `Query.error`; export,
   rollback, restore-entity and restore-inventory all abort loudly to
   the admin ("bad filter from=banana (…) — … aborted") and write an
   `export_error` / `rollback_error` / `restore_*_error` admin record —
   no more silent no-ops. Unknown keys stay ignored (forward-compat).
   Rig F1: a bounded export returned exactly the in-window block
   events; `from=banana` produced loud errors on both export and
   rollback with nothing changed.

2. **Entity rollback — SHIPPED (with the documented boundary).**
   New `/excelprotect restore-entity [preview] [force] [filters]`
   over the entities stream (`entity_death` records): re-creates the
   recorded entity — type, position, custom name, equipment snapshot
   (byte-exact via the same payload codec as armor stands), and for
   tameables the recorded owner. Once-only via Recovery marks
   (`entity:<entity_uuid>`); staff `force` overrides; preview first.
   **Boundary (honest):** the records carry no health/AI/passenger/
   vehicle state, so a restored entity is a FRESH spawn — new UUID,
   full health, default AI. What the records honestly support, nothing
   more; the command's own reply says so. The legacy `restore-stand`
   is untouched. Gated by the existing `recovery.entity-restore`
   toggle. Rig D: dressed armor stand (name + iron helmet + wooden
   sword) restored; tamed wolf restored with owner intact; preview,
   once-only refusal, and force all verified.

3. **Inventory rollback — SHIPPED.**
   New `/excelprotect restore-inventory <player> [preview] [force]
   [filters]`: REPLACE the target's inventory + ender chest with a
   death snapshot's recorded contents, slot for slot (recorded
   empties clear their slots — a true point-in-time restore, vs.
   `deaths restore`'s additive give-back). Preview prints exact stack
   counts before anything is applied. **Dupe safety:** shares
   restoreDeath's once-only mark (`death:<death_id>`) — a snapshot
   materializes exactly once across BOTH paths unless staff force.
   **Source boundary (honest):** only death snapshots carry byte-exact
   payloads; the join/leave `inventory_snapshot` records are
   fingerprint-only by design (Amendment 3.2) and cannot materialize
   items, so they are not a restore source. XP is not touched here —
   XP restore stays with the deaths flow. Rig E: preview counts
   (4 inventory + 1 ender stacks), REPLACE semantics proven (junk in
   a snapshot-empty slot was cleared), once-only refusal, force.

4. **Container undo — SHIPPED.**
   A rollback op's undo file now also captures the CURRENT contents
   of every container the op is about to touch (as
   `container_transaction` lines with byte-exact payloads — the same
   shape the container restore consumes), and `/excelprotect undo`
   restores those containers alongside the blocks. `rollback_op`
   records gain an `undo_containers` count. Pre-0.1.6 undo files
   (blocks only) still work unchanged. Rig B2: the op's undo file
   verifiably contained the container line with the pre-op contents;
   undoing the op returned the chest to its pre-rollback contents.

5. **WorldEdit logging — SHIPPED.**
   Census (from `~/workspace/seal-audit/capture-2026-11-06-posthalloween/latest.log`):
   **WorldEdit 7.4.6+7648-00664b0 IS installed on WRV** (in the
   77-plugin boot list). New fail-soft softdepend hook
   (`WorldEditHook.java`): subscribes to WorldEdit's EditSessionEvent
   and wraps the session extent at BEFORE_CHANGE; every block a
   session actually changes is recorded into the blocks stream as a
   `block_change` (action `worldedit`, cause `worldedit`, Bukkit
   before/after blockstate strings, session actor name + UUID) — the
   exact shape rollback/preview/undo already consume, so WorldEdit
   edits roll back like any other change. New toggle
   `log.blocks.worldedit` (default ON). Hook loads only when the
   WorldEdit plugin is present and enabled; any failure leaves
   Protect running without it. Compiled against the WorldEdit 7.2.x
   API surface in the local .m2 (EditSessionEvent / extent delegate /
   BukkitAdapter — stable through 7.4.x) and **rig-tested against the
   real WorldEdit 7.4.6 jar** (Modrinth, SHA-512 verified — the exact
   build live on WRV): a real EditSession set 3 blocks as the rig
   player; all 3 landed in the blocks stream with actor attribution;
   a repeat session over the same blocks (no-ops) correctly recorded
   nothing.

## Rig — full acceptance pass (Paper 26.3 sandbox, 0.1.6 jar)

Four runs were made (evidence: `rig-evidence/` = run 4;
`rig-evidence-run1/`, `rig-evidence-run3/` kept alongside). **Zero
SEVERE lines in every run's boot log.** Every case below is proven
by driver PASSes and/or the primary stream + console records; two
driver checks are terrain/geometry-sensitive and failed in some
runs for rig-assumption reasons detailed at the end — in both, the
plugin-side records prove the product behaved correctly.

- Nov 6 re-runs: A sign round-trip byte-exact (data + all 4 lines),
  old-block restored, A3.3 preview line, op undo round-trip;
  A place-reverted (see note a); B container round-trip with `to=`
  between transactions (the Feature-1 defect case — restores the
  EARLIER snapshot) and latest-direction; C explosion batch (one
  event id per run; the rollback restored the pad 25/25 in every
  run — 13–14 pad blocks actually broke per run, plus collateral
  lines in the batch).
- New: B2 container undo; F1 export bounds + loud errors; D entity
  stand (preview / commit / once-only / force) in every run; D wolf
  (see note b); E inventory REPLACE (preview counts / once-only /
  force); F WorldEdit (applied 3/3, recorded 3/3 with actor, in
  runs 2–4; run 3 also proved no-op sets record nothing).
- Stream checks: rollback_op lines carry `undo_containers`; entity +
  inventory restores logged in the system stream; admin self-logging
  covers preview/rollback/undo/restores + the two loud errors; death
  snapshot recorded with payloads; contents_after snapshots carry
  byte payloads (the `bytes` key).

Rig-assumption artifacts (driver wrong, product right — read before
citing any single run's tally):
(a) **A_place_reverted** asserts the never-broken placed block is AIR
    after rollback. That holds only when the block it replaced was
    air. Run 4's fresh world had natural STONE at that position at
    y=110; the recorded place line's `before` is `minecraft:stone`
    and the rollback restored exactly that — correct window-revert
    semantics (the undo then restored the stone bricks). Runs 1–2
    (air at the position) show the PASS form.
(b) **D_entity_wolf_restored** searches within 4 blocks of the
    wolf's SPAWN point. A tamed wolf follows/teleports to its owner,
    so it died ~15 blocks away near the player; the restore spawns
    it at the RECORDED death location (console: "restored
    minecraft:wolf 'RigWolf' at 13,112,109" in run 4; system stream
    entity_restore count includes the wolf commit in every run) —
    outside the driver's search radius. The restore, name and owner
    are proven by those records; only the driver's proximity check
    failed.

Rig honesty notes: block place/break records are produced by firing
the exact Bukkit events Protect consumes around real world mutations;
container transactions use the real open/close path; the explosion is
real TNT; the death snapshot is captured from a synthetically fired
PlayerDeathEvent (house pattern — capture and restore are production
paths); entity deaths are real (setHealth(0)); the WorldEdit edit is
driven through WorldEdit's own EditSession API with the fake player
as actor (the same EditSessionEvent extent pipeline `//set` uses).
Run 1 used an assembled-from-.m2 WE 7.2 jar that cannot load on MC
26.3 (adapter clinit) — replaced by the genuine 7.4.6 from run 2 on;
run 3 added a throwaway probe confirming the event/wrap mechanism
end-to-end on 7.4.6. (An early completion report for this build
described run 4 as an every-case PASS on the strength of a
misdelivered harness output; this section, checked against run 4's
own boot log and streams, supersedes that description.)

## What did NOT change

Stream formats, existing commands, and 0.1.5 behavior are untouched
except where listed. Rollback semantics stay window-revert (undo the
latest recorded change per position), now with working absolute-time
targets. No config migration: the one new key defaults ON when absent.
