# BUILD RECORD — ExcelChunks 0.1.1 (console admin)

Built Nov 10, 2026 (desk build lane for the WRV board supervisor).
Commission: the console-admin coverage audit
(`~/workspace/console-admin-audit-20261109/AUDIT.md` §4 —
ExcelChunks 0.1.0 verdict FAIL, packet sized TINY) under Mike's
Nov 3, 2026 console-admin rule: every Excel plugin ships
console-runnable admin commands. **Not installed.** The install
packet is `INSTALL-PACKET.md` in this folder; the MANIFEST entry
is drafted in `MANIFEST-ENTRY-DRAFT.md` for the supervisor to
file after verification (this lane did NOT touch the MANIFEST,
the parent-backlog, board state, or MEMORY.md). Records staged
at `~/workspace/records-catchup-20261103/packages/excelchunks-0.1.1-20261110/`;
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). The
0.1.0 tree at `~/workspace/excelchunks/` is untouched — the
0.1.0 jar still hashes to its record value (verified at
close-out).

## Version re-base check (the supervisor's standing warning)

The audit's target for this packet was 0.1.1, and for once the
label is right: the live version is **0.1.0** (installed +
boot-verified Oct 23, 2026, per
`~/workspace/excelchunks/INSTALLED-0.1.0.txt` and the
MANIFEST-ARCHIVE installed entry), no 0.1.1 build existed
anywhere on disk, so 0.1.1 = live + one minor, no re-base
needed.

## Base proof

- Base source of record: `~/workspace/excelchunks/src/ExcelChunks/`
  (the 0.1.0 tree; `~/workspace/excelchunks/SOURCE.md` records
  the artifacts).
- Live gate fingerprint re-verified on disk: `ExcelChunks-0.1.0.jar`
  — 7,150 B — SHA-256
  `cf4f08c835ba4ec1314b0b12460367e98d6f857dd4bd37ad16e6faaee6ce2948`
  (matches the folder's SOURCE.md and the MANIFEST-ARCHIVE
  installed entry).
- The unmodified 0.1.0 tree, rebuilt in a scratch copy for
  this packet, reproduces the live jar **byte-identically**
  (same size, same SHA-256 above) and its carried suite runs
  **60/60** — the strongest form of the base proof.
- The 0.1.1 delta was applied to a copy of that tree in
  `~/workspace/chunks-011-build/ExcelChunks/` (copy verified
  byte-identical to the 0.1.0 tree with `diff -r` before any
  edit); the 0.1.0 folder itself was never edited.
- Baseline in the new tree, before the delta: the carried
  suite ran **60/60**, matching the 0.1.0 SOURCE record count
  exactly.

## Candidate

- `ExcelChunks-0.1.1.jar` — **8,784 B** — SHA-256
  `8d9b47e3dfaa453322915fd216ddcf4820f15c3c33e6418f76a7cc7c19b33877`
- Deterministic double-build byte-identical (two consecutive
  `build.sh` runs, same hash).
- Build ID: this lineage carries no in-jar build-ID fingerprint
  — the artifact is identified by version 0.1.1 + the SHA-256
  above.
- `ExcelChunks-0.1.1-source.zip` — 14,091 B — SHA-256
  `a5545334aa4ffe900fd0a2365d7e548066941a4ced6b9fcfc9c99b5a3298e4cc`
  (the whole ExcelChunks/ tree, build/ excluded as in the
  0.1.0 zip; timestamps pinned 2026-11-10).
- Jar-level delta vs live 0.1.0: only `ExcelChunks.class`
  changed and `core/StatusCommand.class` was added; every
  other class and resource entry except plugin.yml is
  byte-identical to the live jar.

## What shipped (the whole change, nothing else)

Player-facing behaviour change: **none.** The resync (same-spot
teleport + item hide/show cycle), the cooldown, and the
console refusal for the bare command are behaviour-unchanged.
Change-scope diff vs the 0.1.0 tree — only these files:

- `src/.../core/StatusCommand.java` (new) — Bukkit-free status
  logic: the permission constant, the gate arithmetic
  (`isAdmin`: console always, otherwise the node), the exact
  status-subcommand matcher, the denial text, and the status
  formatter, which builds its three lines from the plugin's
  real constants passed in by the glue (version, radius,
  cooldown, hide/show delay) so the text cannot drift from
  the behaviour.
- `src/.../ExcelChunks.java` — a `status` dispatch in
  `onCommand` routed BEFORE the player-only gate, plus
  `handleStatus` (gate check, then the three read-only
  lines). Class javadoc gains the 0.1.1 paragraph. The self
  resync below the dispatch is byte-for-byte the 0.1.0 logic.
- `plugin.yml` — version 0.1.1; the command description and
  usage name the status form; NEW `excelchunks.admin`
  permission (default: op). The command-level
  `permission: excelchunks.use` (default true) is UNCHANGED —
  every player keeps /chunks, and the admin node is checked
  inside the status branch only.
- `tests/StatusCommandTest.java` (new, 29 checks) + one
  registration line in `tests/TestMain.java`; `build.sh`
  version / output / date knobs.
- **No change:** CooldownStore, ItemSelection, and every
  other 0.1.0 source and test file. There is still no config
  file and no persistence — nothing to migrate.

### The command, as built

- `/chunks status` — three lines, e.g.
  `ExcelChunks status: enabled (v0.1.1)` /
  `View resync: ON - same-spot teleport + dropped-item hide/show cycle; item radius 96 blocks, cooldown 10s per player, hide/show delay 2 ticks.` /
  `Config: no config file - radius and cooldown are built-in constants. Permissions: excelchunks.use (default true) for /chunks; excelchunks.admin (default op) for this status.`
- The audit's sizing was followed exactly: ONE global status
  beside the unchanged self resync. No per-player op exists
  because there is no persistent per-player state to fix —
  the only per-player state is the in-memory 10-second
  cooldown, which expires on its own faster than an operator
  could act on it.

### State-model honesty (nothing invented)

- The status runs on the plugin's real constants — the same
  RADIUS_BLOCKS / COOLDOWN_MILLIS / SHOW_DELAY_TICKS fields
  the enable line prints and the resync path uses, passed
  into the formatter by the glue.
- Status is read-only by construction: StatusCommand holds
  no reference to the cooldown store or any player, so a
  status read can create, change, or clear nothing.

## Tests

- **Desk: 89/89** via `run-tests.sh` — the existing
  **60-check** suite (CooldownStore 33 + ItemSelection 27)
  passes unchanged, plus the new **StatusCommandTest: 29
  checks** (gate arithmetic: console allowed, node-holder
  allowed, non-admin denied; the exact-word matcher incl.
  junk/null rejection; the exact status texts + the
  formatter-follows-inputs pins; the plugin.yml gate:
  version 0.1.1, the admin node defaulting to op, the
  command permission unchanged, the usage naming the
  status form).
- **Deterministic double-build byte-identical** at the
  shipped hash (see Candidate).
- **Rig** (Paper 26.2-123 + Geyser-Spigot 2.11.3 + floodgate
  2.2.5, the 0.1.0 full-stack rig stack, commands typed on
  the real console stdin, `rig-0.1.1/`): clean 0.1.1 enable
  with the 0.1.0-shaped enable line, SEVERE 0, clean stop,
  **6/6 behavior checks** — all three status lines verbatim
  from console, the bare-command console refusal byte-for-
  byte the 0.1.0 line, and the plugin list at 0.1.1. Full
  table + limitations in `rig-0.1.1/RIG-RESULTS.md`; log:
  `rig-0.1.1/boot.log`.

## Surfaces checked (expected no-change, confirmed)

- Guide knowledge/intents: operator-only command — no player
  teaching owed; nothing added.
- Website pair, What's New BOTH surfaces, broadcaster tips,
  in-game menus (ExcelMenu / ExcelBedrockMenu): no
  player-facing claim about Chunks admin exists to correct;
  no changes made.
- The one documentation surface owed — an operator-facing
  note that console status for ExcelChunks now exists — is
  this build record plus the install packet's console-smoke
  section.

## Live proof owed after install (not part of the build)

One console `chunks status` read back in the server log. No
player-visible check is owed — no player surface changes.
The 0.1.0 player proof still owed separately (Kailey's first
live /chunks on a genuinely desynced pile, then Rylie's) is
NOT discharged by this packet.
