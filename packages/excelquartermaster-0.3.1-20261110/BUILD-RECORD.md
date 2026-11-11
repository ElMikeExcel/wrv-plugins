# BUILD RECORD — ExcelQuartermaster 0.3.1 (console admin)

Built Nov 10, 2026 (desk build lane for the WRV board supervisor).
Commission: the console-admin coverage audit
(`~/workspace/console-admin-audit-20261109/AUDIT.md` §2/§4 —
ExcelQuartermaster verdict FAIL, packet sized TINY) under Mike's
Nov 3, 2026 console-admin rule: every Excel plugin ships
console-runnable admin commands to inspect and fix a player's
state. **Not installed.** The install packet is
`INSTALL-PACKET.md` in this folder; the MANIFEST entry is drafted
in `MANIFEST-ENTRY-DRAFT.md` for the supervisor to file after
verification (this lane did NOT touch the MANIFEST, the
parent-backlog, board state, or MEMORY.md). Records staged at
`~/workspace/records-catchup-20261103/packages/excelquartermaster-0.3.1-20261110/`;
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). The
0.3.0 tree at `~/workspace/excelquartermaster-0.3.0/` is
untouched — the 0.3.0 jar still hashes to its record value
(verified at close-out).

## Base proof

- Base source of record: `~/workspace/excelquartermaster-0.3.0/src/ExcelQuartermaster/`
  (the 0.3.0 tree; its SOURCE.md records the live jar and the
  INSTALLED.txt in that folder records the Oct 21, 2026 install).
  No records package for ExcelQuartermaster exists under
  `~/workspace/records-catchup-20261103/packages/` (checked — the
  plugin is absent from that set), so the build folder is the
  newest authoritative tree, matching the audit's own sourcing.
- Live gate fingerprint re-verified on disk at build start and
  at close-out: `ExcelQuartermaster-0.3.0.jar` — 77,066 B —
  SHA-256 `70096fc47b61f09131aa42c1f3a66fbee3c248085d3d39f476338cb01b15765a`
  (matches the 0.3.0 SOURCE.md).
- The 0.3.1 delta was applied to a copy of that tree in
  `~/workspace/quartermaster-031-build/ExcelQuartermaster/`
  (copy verified byte-identical to the 0.3.0 tree before any
  edit); the 0.3.0 folder itself was never edited.
- Baseline in the new tree, before the delta: the carried suite
  ran **575/575** (14 harnesses), matching the 0.3.0 SOURCE.md
  count exactly.

## Candidate

- `ExcelQuartermaster-0.3.1.jar` — **79,785 B** — SHA-256
  `a11aa0fcab31544ffeb92c6ab31207412e797bb8d1147eb73d34e4375d0fa2f9`
- Deterministic double-build byte-identical (two consecutive
  `build.sh` runs, same hash).
- Build ID: this lineage carries no in-jar build-ID fingerprint
  (0.3.0 has none) — the artifact is identified by version 0.3.1
  + the SHA-256 above.
- `ExcelQuartermaster-0.3.1-source.zip` — 101,342 B — SHA-256
  `5b9916c14aa46ef50142a3c23d86af2521b36964f443c7fbd0af83a37c99ff79`
  (the whole ExcelQuartermaster/ tree; timestamps pinned
  2026-11-10).

## What shipped (the whole change, nothing else)

Player-facing behaviour change: **none.** Tales, the Java pack
offer flow, the sneak-trade fix, Bedrock forms, and the wand
issue flow are behaviour-unchanged. Change-scope diff vs the
0.3.0 tree — only these files:

- `src/.../PackStatus.java` (new) — Bukkit-free formatter for
  the status reply: offers used (PackPrompt's own
  `offersUsed` / `offersRemaining` / `MAX_OFFERS` arithmetic),
  pack state (the record's outcome, with the accepted and
  exhausted terminal shapes spelled out), and the record's
  last-change timestamp, verbatim. Read-only by construction:
  it formats a record the caller already read; it holds no
  prompt reference and creates nothing.
- `src/.../ExcelQuartermaster.java` — a `status` dispatch in
  `onCommand` before the packreset shape check, plus a private
  `handleStatus` whose target resolution is packreset's,
  unchanged (online player name via `getPlayerExact` first,
  then a raw UUID for offline players; otherwise the parallel
  rejection line "…Nothing was looked up."). The handler only
  reads `packPrompt.record(uuid)` and sends the formatted line;
  nothing is saved. A bare/malformed `/qm` now prints both
  usage lines (packreset's, unchanged, plus status's). Class
  javadoc gains the 0.3.1 paragraph. The packreset body is
  byte-for-byte the 0.3.0 logic.
- `plugin.yml` — version 0.3.1; usage names both forms;
  the existing `excelquartermaster.admin` permission
  description now names status too. The permission itself is
  unchanged (same node, default: op — in Bukkit/Paper the
  console satisfies it, exactly as packreset already relies
  on; no new node was needed or added).
- `tests/.../PackStatusTest.java` (new, 27 checks) + one
  registration block in `run-tests.sh`; `build.sh` version /
  output / date knobs.
- **No change:** config.yml (byte-identical, no new keys),
  PackPrompt, PackReset, PackStateStore, and every other
  0.3.0 source and test file.

### The command, as built

- `/qm status <player>` — one line, e.g.
  `Pack status for <name> (<uuid>): offers used 2 of 5, 3 remaining; pack state: DECLINED; last change 1000.`
  Accepted reads `LOADED (accepted - has the voice pack; terminal)`;
  an exhausted non-accepted record names the packreset way back;
  a player with no record gets the no-record line (0 of 5,
  next join offers 1 of 5).
- The audit's sizing was followed exactly: ONE read-only
  subcommand beside the existing console-capable `packreset`
  (the applicable fix, already present). No grant/revoke/reset
  was added — none was missing.

### State-model honesty (nothing invented)

- Both required fields come from the plugin's real state: the
  `PackPrompt` record persisted in `java-pack-state.txt` is
  precisely "offers used" (its attempts count) and "pack state"
  (its outcome). Nothing had to be invented or approximated.
- Two limits of the existing model are reported, not papered
  over: (1) target resolution is packreset's — an OFFLINE
  player can only be addressed by raw UUID, not by name (the
  plugin keeps no name→UUID record of its own, and 0.3.1 does
  not invent one); (2) the reply's "remaining" for an accepted
  player is 0 because that is PackPrompt's own rule (an
  accepted player needs no more chances) — the reply marks the
  state terminal so the 0 is not misread as "exhausted".
- Out of scope by the audit's sizing, and NOT reported by
  status: the player's tale-rotation position and issued-wand
  ledger state. Those are separate state files with their own
  surfaces; bundling them in would exceed the TINY packet.

## Tests

- **Desk: 602/602** via `run-tests.sh` — the existing
  **575-check** suite (14 harnesses, incl. PackResetTest 51)
  passes unchanged, plus the new **PackStatusTest: 27 checks**
  (exact reply text for every state shape — no record, SENT
  mid-count / exhausted, DECLINED mid-count / exhausted, FAILED
  exhausted, LOADED accepted; state wording; blank/null name
  and null-UUID fallbacks; the read-only guarantee incl. a
  persist/restore round trip; the plugin.yml gate: version
  0.3.1, status usage form, the admin node defaulting to op).
- **Deterministic double-build byte-identical** at the shipped
  hash (see Candidate).
- **Rig** (minimal Paper 26.2-123, only the 0.3.1 jar, seeded
  pack state, commands typed on the real console stdin,
  `rig-0.3.1/`): clean 0.3.1 enable, SEVERE 0, clean stop,
  **12/12 behavior checks** — status for DECLINED mid-count,
  LOADED accepted, and DECLINED exhausted records; the
  no-record line for an unknown UUID; the bad-name rejection;
  both usage lines on a bare `/qm`; the packreset regression
  (RESET on a status-read target, then status reads no-record);
  and the on-disk proof that status created and changed
  nothing. Full table + limitations in
  `rig-0.3.1/RIG-RESULTS.md`; log: `rig-0.3.1/boot.log`.

## Surfaces checked (expected no-change, confirmed)

- Guide knowledge/intents: operator-only command — no player
  teaching owed; nothing added.
- Website pair, What's New BOTH surfaces, broadcaster tips,
  in-game menus (ExcelMenu / ExcelBedrockMenu): no player-facing
  claim about QM admin exists to correct; no changes made.
- The one documentation surface owed — an operator-facing note
  that console inspection for QM pack state now exists — is this
  build record plus the install packet's console-smoke section.

## Live proof owed after install (not part of the build)

One console `qm status <player>` against a real family member
(or their UUID), read back in the server log. No player-visible
check is owed — no player surface changes.
