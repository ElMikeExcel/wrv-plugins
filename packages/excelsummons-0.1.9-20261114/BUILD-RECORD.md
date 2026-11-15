# ExcelSummons 0.1.9 seq 02 — BUILD RECORD (summon cap counts CAST only)

Built Sat Nov 14, 2026 (desk build, worker lane for board
supervisor run 328). Task: P-SUMMONS-CAP-1 — Mike, Nov 14,
2026, ~12:48 AM CST. This CHANGES his locked Nov 10, 2026 cap
rule. Design: `ExcelSummons/DESIGN-0.1.9-capfix.md`. NOT filed,
NOT installed (per task).

## Version choice (with evidence)

**Version stays 0.1.9; build seq rotates 01 → 02.** The task
directs the fix to fold into the built-but-unfiled 0.1.9 line.
The base tree's own record supports seq rotation rather than a
version bump: its BUILD-RECORD.md pins the build ID
`EXC-SUMMONS-0.1.9-WRV-20261113-01-d19a4c` and local-build.sh
states "The build ID is PINNED for a version (keeps the
double-build byte-identical); release tooling rotates seq/hex
per distributed build." 0.1.9 was never filed or installed, so
no shipped artifact carries the 0.1.9 version yet; the jar
content changed, so the fingerprint MUST change — new build:
date 2026-11-14, seq 02, tag c9a114. plugin.yml still declares
0.1.9; BuildInfo.VERSION still "0.1.9".

## Candidate

- **Jar:** `ExcelSummons-0.1.9.jar` — **102,260 bytes**
  (seq-01 jar was 102,062 bytes)
- **SHA-256:** `ead3beec553da8e37d255971c8e8e5f96a707835c0a8811a90df115759e6df46`
- **Build ID:** `EXC-SUMMONS-0.1.9-WRV-20261114-02-c9a114`
  (plugin.yml inside the jar declares 0.1.9; excel-build.txt
  matches; fingerprint gate PASS)
- **Sources:** `ExcelSummons-0.1.9-sources.zip` — 136,977 bytes,
  SHA-256 `9cba1ff3394da6061f6f89714ef323cc7577d4800b2a9326a89ec15c58689406`
  (45 files; full project tree in `ExcelSummons/` beside it).
  `SHA256SUMS` verifies both.
- **Base:** ExcelSummons 0.1.9 seq 01
  (`~/workspace/summons-019-build-20261113/`). **Base gate:**
  the copied tree rebuilt UNMODIFIED in this folder produced
  `753977e0…` — byte-identical to the seq-01 jar — with 104/104
  tests green, BEFORE any edit. The seq-01 jar is also kept
  here as `REFERENCE-0.1.9-shipped.jar` (same hash).
- **Tests:** 107 found / 107 successful / 0 failed (104 carried
  + 3 new). Deterministic double-build byte-identical inside
  the build script AND across two full build runs. Full
  detail: `TEST-RESULTS.md`.

## The cap-count delta

- **Before:** `SummonRegistry.countCounted` counted every
  record in COMBAT + COMPANION + PACK regardless of origin —
  adopted pets occupied cap slots.
- **After:** the new predicate `SummonRegistry.countsTowardCap`
  (counted branch AND `Origin.CAST`) is the single counting
  path: `countCounted`, `CapPolicy.countedNames`, and
  `CapPolicy.anyCountedReleasable` all read it. Adopted /
  registry pets are excluded from the count, from the refusal
  message's count, and from its name list. CAST summons are
  counted exactly as before (all three branches, mount still
  excluded).
- **Mana behavior unchanged:** ReservePolicy logic untouched
  (one stale header sentence updated — it claimed adopted
  records "still count toward the §3 count cap"; they no
  longer do). Adopted = 0% raw, applied, and in modifiers,
  proven by test (c).
- **Unchanged by design:** per-spell SPELL_CAP (`countSpell`),
  combat SERVER_CEILING, ALL dismiss scopes (typed dismiss
  still never touches adopted — the incident's "Nothing to
  dismiss" was correct behavior; the menu per-pet confirm
  dismiss remains the only adopted release), the rank ladder
  (Beginner 1 / Student 2 / Apprentice 3 / Master 5), rank
  thresholds, config values (config.yml comment amended to
  state the cast-only scope; no value changed).

## Changes per file

- `src/.../SummonRegistry.java` — NEW `countsTowardCap`
  predicate; `countCounted` counts CAST records in counted
  branches only; javadoc states the Nov 14 rule + trigger.
- `src/.../CapPolicy.java` — header gains the Nov 14 amendment
  block; `anyCountedReleasable` and `countedNames` read
  `countsTowardCap` (message counts/names cast summons only).
  `totalCapMessage`'s "can only leave by death" branch is
  retained defensively; it is unreachable via `check()` under
  the cast-only count because every counted record is CAST
  and therefore releasable.
- `src/.../ReservePolicy.java` — header sentence corrected
  (see above). No logic change.
- `src/.../BuildInfo.java`, `local-build.sh` — build knobs:
  date 2026-11-14, seq 02, tag c9a114. Version stays 0.1.9.
- `config.yml` — comment only: cast-only scope stated.
- `DESIGN-0.1.9-capfix.md` — NEW design note.
- `tests/.../SummonsTest.java` — 3 new tests (a/b/c above);
  six carried counted-total tests migrated `rec()` →
  `recCast()` (the `rec()` helper defaults origin ADOPTED —
  those tests always meant the cast set); the adopted-only
  TOTAL_CAP segment of `refusalMessagesNameTheirReason`
  re-pinned (adopted-only set now checks NONE; legacy text
  shape pinned directly on `totalCapMessage`);
  `pluginYmlPins019` fingerprint assertion re-pinned to the
  seq-02 prefix.

Nothing else changed: pet names + rename (the whole seq-01
feature set), menus, nameplates, AdminPolicy, lifetime,
target rules.

## What remains for the supervisor

1. **RIG — still owed** (this lane ran desk-only per task):
   everything on the seq-01 rig list (base tree BUILD-RECORD)
   PLUS the cap-fix proofs in TEST-RESULTS.md (adopted pack
   over cap + casts up to the cast cap; TOTAL_CAP message
   names cast summons only).
2. **Filing** — when the supervisor files 0.1.9, THIS seq-02
   jar supersedes the seq-01 candidate in
   `~/workspace/summons-019-build-20261113/` (same version,
   newer build ID; the MANIFEST packet must name build
   `EXC-SUMMONS-0.1.9-WRV-20261114-02-c9a114` and hash
   `ead3beec…`). Jar swap is a fully-gated entry (backup-first,
   emptiness gate). NOT filed by this lane.
3. **Same-pass surfaces at install time** — as listed in the
   seq-01 BUILD-RECORD, plus the cap rule in the Guide /
   What's New wording: "adopted pets don't count toward your
   summon limit — only summons you cast do."
4. **Player proof (acceptance, Mike)** — his adopted pack out,
   casts work up to his cast cap (3 at Apprentice).
