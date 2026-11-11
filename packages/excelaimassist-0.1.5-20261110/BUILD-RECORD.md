# BUILD RECORD — ExcelAimAssist 0.1.5 (console admin)

Built Nov 10, 2026 (desk build lane for the WRV board
supervisor). Commission: the console-admin coverage audit
(`~/workspace/console-admin-audit-20261109/AUDIT.md` §4 —
ExcelAimAssist verdict FAIL, packet sized SMALL; the audit's
recommended "0.1.4" number was already taken by the live
menu-toggle build, so per the supplement in that audit folder
the packet is re-based to 0.1.5) under Mike's Nov 3, 2026
console-admin rule: every Excel plugin ships console-runnable
admin commands to inspect and fix a player's state. **Not
installed.** The install packet is `INSTALL-PACKET.md` in this
folder; the MANIFEST entry is drafted in
`MANIFEST-ENTRY-DRAFT.md` for the supervisor to file after
verification (this lane did NOT touch the MANIFEST, the
parent-backlog, board state, or MEMORY.md). Records staged at
`~/workspace/records-catchup-20261103/packages/excelaimassist-0.1.5-20261110/`;
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). The
0.1.4 tree at `~/workspace/aimassist-toggle/` is untouched —
its live jar still hashes to its record value (verified at
close-out).

## Base proof

- Base source of record:
  `~/workspace/aimassist-toggle/work/aimassist-0.1.4/` (src/,
  tests/, plugin.yml, config.yml, build.sh, run-tests.sh — the
  0.1.4 tree, whose jar is the live jar of record per
  `~/workspace/aimassist-toggle/INSTALLED.txt`, installed
  Oct 30, 2026).
- Live gate fingerprint re-verified on disk at build start and
  at close-out: `ExcelAimAssist-0.1.4.jar` — 19,401 B —
  SHA-256 `7d040d98fc063418b3635c676e8a6f3db5adf093a4e99dec28a8f1c1ac1e6a44`
  (matches INSTALLED.txt exactly).
- The 0.1.5 delta was applied to a copy of that tree in
  `~/workspace/aimassist-015-build/ExcelAimAssist/` (copy
  verified byte-identical to the 0.1.4 tree — `diff -r` clean
  on src/ and tests/, `cmp` clean on plugin.yml, config.yml,
  build.sh, run-tests.sh — before any edit); the 0.1.4 folder
  itself was never edited.
- Baseline in the new tree, before the delta: the carried
  suite ran **190/190**, matching the 0.1.4 SOURCE.md count
  exactly (the 0.1.3 suite of 178 + the 12 toggle() checks).

## Candidate

- `ExcelAimAssist-0.1.5.jar` — **22,745 B** — SHA-256
  `9e5d926b74f0ddaba9b0d9adaaacf9cd41ace56a15a97dbe44a7a28b1fbc7a50`
- Deterministic double-build byte-identical (two consecutive
  `build.sh` runs, same hash).
- Build ID: this lineage carries no in-jar build-ID
  fingerprint — the artifact is identified by version 0.1.5 +
  the SHA-256 above.
- `ExcelAimAssist-0.1.5-source.zip` — 36,076 B — SHA-256
  `278ce4e8ed37cf4803418be8c86007616e4f27cd2f2b7b72c56bd99ac196afd4`
  (the whole ExcelAimAssist/ tree; timestamps pinned
  2026-11-10).

## What shipped (the whole change, nothing else)

Player-facing behaviour change: **none.** The snap (AimMath,
TargetPicker), the config, the self-service toggle, the Server
Menu wiring and the PlaceholderAPI expansion are
behaviour-unchanged. Change-scope diff vs the 0.1.4 tree —
only these files:

- `src/.../core/AdminCommands.java` (new) — Bukkit-free home
  of the admin surface: the gate decision
  (`allowed(isConsole, hasPermission)` — console explicitly
  allowed, the ExcelCosmetics pattern; anyone else needs the
  permission), the set-value parser, and the exact reply texts
  for status, set, both not-found rejections, both usage
  lines, and the permission denial. Status formats the
  player's real ToggleStore state and is read-only by
  construction; set writes through `ToggleStore.set` — the
  store's real API, the same call the self-service on/off
  forms make.
- `src/.../ExcelAimAssist.java` — an `admin` dispatch in
  `onCommand` routed BEFORE the player-only gate, plus a
  private `handleAdmin` (gate check, status/set dispatch,
  persist-after-set via the existing `saveToggles()`, an info
  log line per set) and a target resolver (online player by
  exact name first, then a raw UUID — the house shape). Class
  javadoc gains the 0.1.5 paragraph. Everything below the new
  branch — the self-service on/off/toggle forms and their
  exact messages — is byte-for-byte the 0.1.4 code, and the
  aim/event code is untouched.
- `plugin.yml` — version 0.1.5; the aimassist usage and
  description now name the admin forms; a NEW permission
  `excelaimassist.admin` (default: op — the console satisfies
  op). The existing `excelaimassist.toggle` node is unchanged
  (default: true), and there is deliberately NO command-level
  permission gate on /aimassist: the self-service forms must
  stay open to every player, so the admin gate is enforced in
  code on the admin branch only. The harness pins all of this.
- `tests/AdminCommandsTest.java` (new, 44 checks) + its
  registration in `tests/TestMain.java`;
  `build.sh` / `run-tests.sh` version, output and rig
  knobs (run-tests.sh also puts the project dir on the test
  classpath so the new harness can read the shipped
  plugin.yml, the Quartermaster PackStatusTest pattern).
- **No change:** config.yml (byte-identical to 0.1.4 — no new
  keys; the copy inside the jar is byte-identical too),
  AimMath, TargetPicker, ToggleStore, and
  ExcelAimAssistExpansion sources (verified by the tree diff;
  at jar level only ExcelAimAssist.class changed and
  AdminCommands.class + ExcelAimAssist$Resolved.class were
  added — every other jar entry, config.yml included, is
  byte-identical to the live 0.1.4 jar).

### The commands, as built

- `/aimassist admin status <player>` — one line, e.g.
  `Aim assist status for Marvin (<uuid>): OFF (explicit choice).`
  A player who never toggled reads
  `ON (default - no explicit choice stored).` — the
  explicit/default distinction is spelled out, never hidden.
- `/aimassist admin set <player> on|off` — writes the target's
  ToggleStore entry and persists toggles.txt immediately,
  e.g. `Aim assist for Marvin (<uuid>) set to ON (was OFF).`
  (the was-state is the effective state, so setting a
  never-toggled player off reports "was ON"). The set is also
  written to the server log by the plugin.
- Malformed forms print the usage line(s); an unresolvable
  target prints the "Nothing was looked up." / "Nothing was
  changed." rejection; a non-console sender without the
  permission gets the denial naming the node. Self-service
  `/aimassist on|off|toggle` (and the menu toggle) behave
  exactly as in 0.1.4.

### State-model honesty (nothing invented)

- Both admin forms operate on the plugin's real state: the
  ToggleStore persisted in toggles.txt is precisely the state
  the self toggle, the Server Menu buttons and
  %aimassist_status% read, so an admin status can never
  disagree with what the player sees, and an admin set takes
  effect on the target's very next shot with no relog.
- The one limit of the existing model is reported, not
  papered over: target resolution is online-name-first, then
  raw UUID. An OFFLINE player can be addressed by UUID, but
  not by name — the plugin keeps no name→UUID record of its
  own, and 0.1.5 does not invent one.
- Out of scope by the audit's sizing, and NOT built: any
  change to aim behaviour, any config key, any menu or
  placeholder change, and any grant/revoke beyond the toggle
  itself (the toggle is the plugin's only per-player state).

## Tests

- **Desk: 234/234** via `run-tests.sh` — the existing
  **190-check** suite (AimMath, TargetPicker, ToggleStore incl.
  the 0.1.4 toggle() block) passes unchanged, plus the new
  **AdminCommandsTest: 44 checks** (the gate matrix — console
  allowed, permission allowed, non-admin denied; on/off
  parsing incl. malformed values; exact status replies for
  explicit ON / explicit OFF / default ON; status read-only
  incl. the store-bytes guarantee; set through the real store
  incl. persistence round-trip and other-players-untouched;
  usage / denial / not-found texts; the plugin.yml gate:
  version 0.1.5, admin node defaulting to op, toggle node
  unchanged, no command-level permission gate).
- **Deterministic double-build byte-identical** at the
  shipped hash (see Candidate).
- **Rig** (Paper 26.3-231, the 0.1.5 jar + the live
  PlaceholderAPI 2.12.3, seeded toggles, commands typed on
  the real console stdin, `rig-0.1.5/`): clean 0.1.5 enable
  with the expansion registered, SEVERE 0, clean stop,
  **19/19 behavior checks** — status for explicit OFF,
  explicit ON and default-ON records (verbatim); the
  bad-name rejections for both forms; both sets with their
  was-states and the CONSOLE log lines; the malformed-value
  and bare/unknown-admin usage forms; the on-disk proof that
  the status phase changed nothing and the final toggles.txt
  holds exactly the two set outcomes; and the console
  self-form gate line byte-for-byte the 0.1.4 one. Full table
  + limitations in `rig-0.1.5/RIG-RESULTS.md`; log:
  `rig-0.1.5/boot.log`.

## Surfaces checked (expected no-change, confirmed)

- Server Menu (ExcelMenu trio + ExcelBedrockMenu form): reads
  the same aimStatus / toggle contracts — untouched, and the
  rig proves the placeholder still registers at 0.1.5. No
  menu change owed.
- Guide knowledge/intents: operator-only commands — no player
  teaching owed; nothing added.
- Website pair, What's New BOTH surfaces, broadcaster tips:
  no player-facing claim about AimAssist admin exists to
  correct; no changes made.
- The one documentation surface owed — an operator-facing note
  that console inspection/fix for a player's aim toggle now
  exists — is this build record plus the install packet's
  console-smoke section.

## Live proof owed after install (not part of the build)

The console smoke in the install packet (status by a real
family member's NAME — the rig addressed targets by UUID —
plus a set round-trip read back in the log), and one
in-game self-toggle flip by a real player confirming the
self path end-to-end at 0.1.5. No other player-visible check
is owed — no player surface changes.
