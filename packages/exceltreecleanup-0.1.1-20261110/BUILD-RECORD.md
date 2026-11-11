# BUILD RECORD — ExcelTreeCleanup 0.1.1 (console admin)

Built Nov 10, 2026 (desk build lane for the WRV board supervisor).
Commission: the console-admin coverage audit
(`~/workspace/console-admin-audit-20261109/AUDIT.md` §4 —
ExcelTreeCleanup 0.1.0 verdict FAIL, packet sized SMALL, "Same
shape as AimAssist") under Mike's Nov 3, 2026 console-admin
rule: every Excel plugin ships console-runnable admin commands
to inspect and fix a player's state. **Not installed.** The
install packet is `INSTALL-PACKET.md` in this folder; the
MANIFEST entry is drafted in `MANIFEST-ENTRY-DRAFT.md` for the
supervisor to file after verification (this lane did NOT touch
the MANIFEST, the parent-backlog, board state, or MEMORY.md).
Records staged at
`~/workspace/records-catchup-20261103/packages/exceltreecleanup-0.1.1-20261110/`;
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). The
0.1.0 tree at `~/workspace/exceltreecleanup/` is untouched — the
0.1.0 jar still hashes to its record value (verified at
close-out).

## Base proof

- Base source of record: `~/workspace/exceltreecleanup/src/ExcelTreeCleanup/`
  (the 0.1.0 tree; its `ExcelTreeCleanup-0.1.0-SOURCE.md` records
  the artifacts, and `INSTALLED.txt` in that folder records the
  live install on Oct 20, 2026).
- Live gate fingerprint re-verified on disk at build start and
  at close-out: `ExcelTreeCleanup-0.1.0.jar` — 20,515 B —
  SHA-256 `0507acefca25ec83858d25ab9dfbd17c47f230fd708939a86b357080bb883423`
  (matches both the folder's SHA256SUMS and the SOURCE record).
- The 0.1.1 delta was applied to a copy of that tree in
  `~/workspace/treecleanup-011-build/ExcelTreeCleanup/` (copy
  verified byte-identical to the 0.1.0 tree with `diff -r`
  before any edit); the 0.1.0 folder itself was never edited.
- Baseline in the new tree, before the delta: the carried suite
  ran **84/84**, matching the 0.1.0 SOURCE record count exactly.

## Candidate

- `ExcelTreeCleanup-0.1.1.jar` — **24,389 B** — SHA-256
  `66e5f4a431fa920fc9f7a9eb582d045f0486b9bc2356555e0d8b422778dc21a7`
- Deterministic double-build byte-identical (two consecutive
  `build.sh` runs, same hash).
- Build ID: this lineage carries no in-jar build-ID fingerprint
  (0.1.0 has none) — the artifact is identified by version 0.1.1
  + the SHA-256 above.
- `ExcelTreeCleanup-0.1.1-source.zip` — 28,526 B — SHA-256
  `54cd3b497ae7a11e9d04e4a08be828f67d3d0d0da5e10ade8976e56aa758da78`
  (the whole ExcelTreeCleanup/ tree, build/ excluded as in the
  0.1.0 zip; timestamps pinned 2026-11-10).

## What shipped (the whole change, nothing else)

Player-facing behaviour change: **none.** The cleanup trigger,
the analyzer, the claim seam, and the self toggle are
behaviour-unchanged. Change-scope diff vs the 0.1.0 tree —
only these files:

- `src/.../core/AdminCommand.java` (new) — Bukkit-free admin
  logic: the permission constant, the gate arithmetic
  (`isAdmin`: console always, otherwise the node), the on|off
  value parser, the exact usage/denial texts, the status
  formatter (effective state + explicit-vs-default, read from
  ToggleStore's own `isEnabled`/`hasExplicit`), and `applySet`,
  the only mutating entry, which writes through
  `ToggleStore.set`.
- `src/.../ExcelTreeCleanup.java` — an `admin` dispatch in
  `onCommand` routed BEFORE the player-only gate, plus
  `handleAdmin` (gate check, status/set dispatch, the house
  target resolution — online exact name via `getPlayerExact`,
  then a raw UUID — and an audit log line per set) and the
  `resolveAdminTarget` helper. Status only reads the store;
  set persists via the same `saveToggles()` the self toggle
  uses. Class javadoc gains the 0.1.1 paragraph. The
  self-toggle body below the dispatch is byte-for-byte the
  0.1.0 logic.
- `plugin.yml` — version 0.1.1; description and usage name the
  admin forms; NEW `permissions:` block declaring
  `exceltreecleanup.admin` (default: op). Deliberately NO
  command-level `permission:` key — /treecleanup is every
  player's own toggle, and a command-level node would lock
  non-op players out of it; the node is checked inside the
  admin branch only. (The description is double-quoted: see
  the rig note below.)
- `tests/AdminCommandTest.java` (new, 38 checks) + one
  registration line in `tests/TestMain.java`; `build.sh`
  version / output / date knobs.
- **No change:** config.yml (byte-identical, no new keys),
  RemnantAnalyzer, TriggerGate, ToggleStore, ClaimGateway,
  and every other 0.1.0 source and test file.

### The commands, as built

- `/treecleanup admin status <player>` — one line, e.g.
  `TreeCleanup status for Marvin (<uuid>): OFF (explicit choice stored).`
  A never-toggled player reads `ON (default - no explicit
  choice stored).` — the reply distinguishes a chosen state
  from the default, which is the operator's actual question.
- `/treecleanup admin set <player> on|off` — stores an explicit
  choice for the target through ToggleStore.set, persists
  toggles.txt immediately, replies `TreeCleanup for <name>
  (<uuid>) set ON|OFF.`, and logs
  `treecleanup admin: <operator> set <name> (<uuid>) to ON|OFF`.
- The audit's sizing was followed exactly: ONE admin family
  (status + set) beside the unchanged self toggle. No other
  subcommand was added.

### State-model honesty (nothing invented)

- Both ops run on the plugin's real state: the ToggleStore
  persisted in toggles.txt is precisely the per-player toggle
  the trigger gate reads. Nothing had to be invented or
  approximated, and `applySet` uses the store's real `set`
  API — an admin set is indistinguishable in the file from
  the player's own choice (by design: it IS the same state).
- One limit of the existing model is reported, not papered
  over: target resolution is the house shape (online exact
  name first, then raw UUID). An OFFLINE player can only be
  addressed by raw UUID, not by name — the plugin keeps no
  name→UUID record of its own, and 0.1.1 does not invent one.
  A name that is neither an online player nor a UUID gets the
  rejection line ("…Nothing was looked up." / "…Nothing was
  changed.") and nothing is read or written.

## Tests

- **Desk: 122/122** via `run-tests.sh` — the existing
  **84-check** suite (RemnantAnalyzer / TriggerGate /
  ToggleStore) passes unchanged, plus the new
  **AdminCommandTest: 38 checks** (gate arithmetic: console
  allowed, node-holder allowed, non-admin denied; exact status
  texts for default-ON / explicit-ON / explicit-OFF; the
  read-only guarantee incl. no record created by a read; set
  applying through the store + the serialize/parse round trip;
  the on|off parser incl. junk/empty/missing rejection;
  malformed-usage texts; the plugin.yml gate: version 0.1.1,
  the admin node defaulting to op, both usage forms, NO
  command-level permission, and the description double-quoted).
- **Deterministic double-build byte-identical** at the shipped
  hash (see Candidate).
- **Rig** (Paper 26.2-123 + GriefPrevention 16.18.7 + the
  0.1.0 TcRigDriver fake players, seeded toggles.txt, commands
  typed on the real console stdin, `rig-0.1.1/`): clean 0.1.1
  enable with the seeded toggle loaded and claim checks ON,
  SEVERE 0, clean stop, **20/20 behavior checks** — status for
  explicit-OFF and default-ON records, set by UUID and by
  online name with audit lines, the bad-name rejection, all
  malformed-usage forms changing nothing, the unchanged
  console guidance for the self command, the driver's R6
  self-toggle case re-proven end-to-end (off leaves a remnant
  standing, on cleans the identical staging), and the on-disk
  proof that status reads create nothing. Full table +
  limitations in `rig-0.1.1/RIG-RESULTS.md`; log:
  `rig-0.1.1/boot.log`.
- **Rig run 1 caught a real defect** (recorded in the rig
  results, not hidden): the first 0.1.1 jar's plugin.yml was
  rejected by Bukkit (`Invalid plugin.yml` — the new
  description was an unquoted plain scalar containing `": "`).
  The description was double-quoted, a harness check pins the
  quoting, the jar was rebuilt, and run 2 above is against the
  shipped hash.

## Surfaces checked (expected no-change, confirmed)

- Guide knowledge/intents: operator-only commands — no player
  teaching owed; nothing added.
- Website pair, What's New BOTH surfaces, broadcaster tips,
  in-game menus (ExcelMenu / ExcelBedrockMenu): no player-facing
  claim about TreeCleanup admin exists to correct; no changes
  made.
- The one documentation surface owed — an operator-facing note
  that console inspection/fix for TreeCleanup toggles now
  exists — is this build record plus the install packet's
  console-smoke section.

## Live proof owed after install (not part of the build)

One console `treecleanup admin status <player>` against a real
family member (or their UUID), plus one `admin set` only if a
real need exists, read back in the server log. No player-visible
check is owed — no player surface changes. (A non-op player's
in-game admin denial is observable only in live play; it is
harness-pinned here and may be spot-checked opportunistically.)
