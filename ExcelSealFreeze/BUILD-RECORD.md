# BUILD RECORD — ExcelSealFreeze 0.1.0 (P-SEALFREEZE-1, freeze half)

Desk build, parent-dispatched lane, Nov 14, 2026. Desk side only: no live
contact of any kind (the one database read was the G4 pull copy, re-hashed
after the test runs — still `b9d251a0…`, untouched), no install, no MANIFEST
edit, nothing filed to the install queue.

**Deliverable:** `ExcelSealFreeze-0.1.0.jar` — **20,456 B** — SHA-256
`d3610948d7a3d1bcdd150d1a61c9e27743d7dfdb7cb86cad23a434383c7ee925` —
build ID `EXC-SEALFREEZE-0.1.0-WRV-20261114-01`. **Deterministic:** two
consecutive `build.sh` runs produced byte-identical jars.

## What it is
The freeze enforcer from FREEZE-SCOPING.md (no native EM lock covers the
freeze — 8 candidates examined, all partial or play-blocking). While armed,
it holds every EliteMobs progression column (`HighestLevelKilled`, `Score`,
`Kills`, `CurrencyV2`, `CurrencyCents`, all 11 `SkillXP_*`) at the arm-time
baseline in `plugins/EliteMobs/data/player_data.db`, and disarms itself the
moment ExcelGuildGate's `state.yml` reads `unlocked: true` (the fifth seal).

## Structure
- `core/Progression` — the clamped column set + row model; the never-touch
  list (Deaths, gambling, quest blobs, prefs) is structural: those columns
  are never even SELECTed.
- `core/ClampPolicy` — pure diff: clamp only values EXCEEDING baseline,
  TO baseline, never below (death-driven negative Score survives); unknown
  player = zero baseline; builds the parameterized UPDATE.
- `core/BaselineStore` — baseline.txt serialize/parse (malformed lines
  skipped, never guessed).
- `core/SealFlag` — conservative raw-text read of GuildGate's `unlocked`
  (top-level key only; anything else = locked) + broken-seal count.
- `core/ClampEngine` — JDBC read/clamp; a cycle's writes run in ONE
  transaction with whole-cycle rollback.
- `ExcelSealFreeze` (host) — `/sealfreeze status|arm|disarm|clampnow`
  (op), async cycle (default 60 s) + delayed join/quit single-row clamps,
  persisted armed state across restarts, arm refused when the seal already
  reads unlocked or the DB is unreachable; armed-without-baseline disarms
  itself on boot rather than clamp blind.

## Safety shape (all deliberate)
- No events cancelled, no main-thread DB work, no gameplay hook at all —
  the plugin cannot block or lag play; it only edits a database on a timer.
- SQLite access uses a busy timeout and skips the cycle when EM holds the
  database; a failed cycle logs one warning line and changes nothing.
- Own file writes fsync the FILE only, never the directory — the plugin is
  immune to the Windows directory-fsync failure class (P-FSYNC-1) and will
  boot clean on the PC mirror rig.

## Tests — 25/25 PASS (TEST-RESULTS.txt)
Policy (8): full-gain clamp set, exact UPDATE shape, below-baseline
untouched, equal untouched, null-baseline = zeros, empty diff = no SQL,
per-column independence, cents independent of the double.
SealFlag (7): locked/unlocked reads, 4-of-5 and 5-of-5 counts, nested
lookalike rejected, timestamp-only rejected, null = locked.
Baseline (2): round-trip on realistic values (incl. a negative score),
malformed lines skipped.
Engine (8) — against a THROWAWAY COPY of the real pulled database: reads
all 11 rows; real values match the G4 record (Jester HLK 22, Marvin 23);
a simulated level-30 spree (HLK/Score/Kills/SkillXP/currency gains) clamps
back in exactly 1 row / 6 columns; simulated death-driven Score losses are
NOT clamped back up; Deaths identical on every row after clamping; the
single-row (join/quit) path restores one row exactly.

## Still owed (named, not hidden)
- The integrated rig proof — RIG-ACCEPTANCE-PLAN.md (real EM writing under
  a running mirror, the Vault/currency-pickup question, unlock flip,
  fail-safe drills), gated on the P-FSYNC-1 mirror fixes like every rig.
- Runtime dependency note for the rig to confirm (same open point as
  ExcelExpansions' reader): the server-provided SQLite JDBC driver
  (`org.sqlite.JDBC`, Paper ships sqlite-jdbc in its libraries) must be
  visible to the plugin classloader on the live build; if it is not, the
  plugin logs arm/cycle failures and stays inert — fail-safe, never fatal.
- The wipe itself: WIPE-PACKET.md, executed by the supervisor's gated
  window; `/sealfreeze arm` is step 5 of that packet.
