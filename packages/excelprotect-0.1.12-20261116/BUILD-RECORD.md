# ExcelProtect 0.1.12 — BUILD RECORD (P-PROTECT-EM-1 fix)

**Worker:** board supervisor run 432, build worker (desk work only — no installs, no live-server changes, no MANIFEST edits).
**Date:** Mon Nov 16, 2026.
**Design of record:** `~/workspace/board-r432/protect-em-fix/DIAGNOSIS.md` §5 (fix design), implemented exactly as specified; decision D95 adopts the design INCLUDING the optional additive `via` record field (DIAGNOSIS §5.1.4 values: `"damage" | "can_build" | "break" | "place"`).
**Build tree (new copy; the 0.1.11 tree was not mutated):** `~/workspace/board-r432/protect-build/build-tree/`

## Base gate — 0.1.11 rebuild

- Rebuilt ExcelProtect 0.1.11 from a scratch copy of its own tree (`/tmp/base0111`, copy of `~/workspace/protect-0111-build-20261115/build-tree/`) with its unmodified `local-build.sh`.
- Result: **PASS — byte-identical.** Rebuilt jar SHA-256 `ba10bbcd84de50f199f78b1e09d354ee189f8919ba9732d654ed2a4bcc94260f`, size 303,587 B — exactly the known 0.1.11 candidate/installed hash and size in DIAGNOSIS §1.1. Base suite: 117 tests, 0 failures. Fingerprint gate PASS.
- Post-build check: the original 0.1.11 tree is untouched (jar still `ba10bbcd…`; `BlockAttempts.java` still `a737d698…`, both per DIAGNOSIS §1.1).

## Candidate

| Artifact | Path | Size (B) | SHA-256 |
|---|---|---:|---|
| Candidate jar | `~/workspace/board-r432/protect-build/ExcelProtect-0.1.12.jar` (also at `build-tree/ExcelProtect-0.1.12.jar`, identical) | 304,634 | `6e3cf3981f38f92d3489bdde60b21f1c44ab202c26a55b70b57f9b5200d51e4b` |
| Source zip | `~/workspace/board-r432/protect-build/excelprotect-0.1.12-source.zip` (69 files: src/, tests/, plugin.yml, config.yml, local-build.sh) | 214,914 | `e392691c21de5b2b468af0f875923d711c5021180e7b75228f60214f244de7d2` |

- Jar contents verified: `plugin.yml` version `'0.1.12'`; `excel-build.txt` = `EXC-PROTECT-0.1.12-WRV-20261116-01-0d1a12`; fingerprint gate PASS on both builds.
- **Deterministic double-build: PASS.** Two consecutive `local-build.sh` runs produced byte-identical jars — SHA-256 `6e3cf398…` both times.
- YAML gate: plugin.yml + config.yml parse clean (build script gate; one initial failure from a straight apostrophe in the new description text was fixed by rephrasing before the successful builds).

## What changed (diff vs 0.1.11 — exactly 6 files)

| File | 0.1.11 (B) | 0.1.12 (B) | Change |
|---|---:|---:|---|
| `src/com/winkreyes/excelprotect/BlockAttemptWatch.java` | 4,182 | 8,621 | NEW `onDamage` (BlockDamageEvent, MONITOR, ignoreCancelled=false; records only when cancelled → action `break`, via `damage`); NEW `onCanBuild` (BlockCanBuildEvent, MONITOR, ignoreCancelled=false; records only when `!isBuildable()` and player non-null → action `place`, via `can_build`, attempted type from `e.getMaterial()` — never the target cell's type). Existing `onBreak`/`onPlace` kept unchanged as backstops (via `break`/`place`). Handlers are read-only (never `setCancelled`/`setBuildable`), fail-soft, no IO. Record conditions factored into pure static predicates `recordsDamage` / `recordsCanBuild` / `recordsTerminal` that the handlers delegate to, so the unit suite pins the exact enforced conditions. `feed` gained a via + explicit-block-type variant (can-build path). Class javadoc states the no-double-count invariant. |
| `src/com/winkreyes/excelprotect/BlockAttempts.java` | 5,761 | 7,193 | Additive `via` on `Attempt` + `Record` (new constructor with via; the pre-0.1.12 constructor delegates, defaulting via from action: break→`break`, place→`place`); `render()` emits `"via"` after `"block"`. Coalescer key deliberately EXCLUDES via (documented in code): an early-stage feed and a backstop feed for the same physical attempt merge into ONE record, first via wins — the second line of defence for the no-double-count invariant. Per-player keying, gap/coalesce, close semantics otherwise unchanged. |
| `src/com/winkreyes/excelprotect/ExcelProtect.java` | 77,294 | 77,901 | Version stamps only: class javadoc gains the 0.1.12 entry; READY log line and `/excelprotect status` report `version=0.1.12`; attempts boot line + registration comment describe the new observation points. No behaviour change. |
| `plugin.yml` | 3,193 | 3,574 | `version: '0.1.12'`; description gains the 0.1.12 EM interception fix clause (0.1.11 history retained). |
| `local-build.sh` | 3,091 | 3,091 | Fingerprint `EXC-PROTECT-0.1.12-WRV-20261116-01-0d1a12`, jar name `ExcelProtect-0.1.12.jar`, pinned jar/touch dates moved to 2026-11-16 (same deterministic scheme as 0.1.11). |
| `tests/com/winkreyes/excelprotect/ProtectTest.java` | 152,983 | 164,086 | +8 tests (below). No existing test modified. |

- **Config: unchanged.** `config.yml` is byte-identical to 0.1.11 (SHA-256 `c17204010170fe09d67bfeea348e89f50da071e3526bc834ee1897af69c98a9b` both trees) — no new keys; `log.attempts.*` untouched, per DIAGNOSIS §5.2. No new dependencies (both new events are Bukkit/Paper core; EliteMobs stays a softdepend only).

## Tests

- **Full suite: 125 tests found, 125 successful, 0 failed, 0 skipped** (117 pre-existing + 8 new), on both builds.
- New pinning tests (0.1.12 section of ProtectTest.java):
  1. `attemptHandlerConditions` — the exact record conditions: damage records only when cancelled; can-build records only when denied + player present; terminal backstops only when cancelled; all three require enabled + EM scope.
  2. `attemptHandlerWiring` — reflection pin: `onDamage(BlockDamageEvent)`, `onCanBuild(BlockCanBuildEvent)`, `onBreak(BlockBreakEvent)`, `onPlace(BlockPlaceEvent)` all exist with `@EventHandler(priority=MONITOR, ignoreCancelled=false)`; also pins the API fact the design rests on (BlockCanBuildEvent is NOT Cancellable; BlockDamageEvent is).
  3. `attemptViaDamageRecordsOnce` — cancelled damage feeds coalesce into exactly ONE record, via `damage`, action `break`, render carries `"via":"damage"`.
  4. `attemptViaCanBuildRecordsOnce` — a denied can-build feed records exactly ONE record, via `can_build`, action `place`, block = the attempted material (`minecraft:dirt`).
  5. `attemptBackstopDoesNotDoubleCount` — early-stage + backstop feeds for the same physical attempt (both orders) yield ONE open/closed record; first via wins.
  6. `attemptToggleOffRecordsNothing` — with `log.attempts.enabled=false` (real Settings), every handler gate refuses and the coalescer stays empty.
  7. `attemptPerPlayerSeparationAcrossVias` — same position/moment, two players: separate records per player UUID across the new vias; closing one player leaves the other untouched.
  8. `attemptViaDefaultsForTerminalFeeds` — pre-0.1.12 constructor feeds default via from action (`break`/`place`) and render the additive field.

## Notes / owed (not part of this build)

- The mirror re-proof with real EliteMobs as the canceller (DIAGNOSIS §6) and the gated install + live re-proof (§6.4) remain supervisor/failsafe work; this build is desk-only per the task. The attempts-toggle flip still rides the install's restart window (no reload command exists — unchanged from 0.1.11).
- `SHA256SUMS` beside this record covers the candidate jar, this record, the source zip, and the six changed sources (paths relative to `~/workspace/board-r432/protect-build/`, verifiable with `sha256sum -c SHA256SUMS` from that folder).

VERDICT: BUILD COMPLETE
