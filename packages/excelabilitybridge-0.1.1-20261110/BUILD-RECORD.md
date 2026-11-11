# BUILD RECORD — ExcelAbilityBridge 0.1.1 (console admin)

Built Nov 10, 2026 (desk build lane for the WRV board supervisor).
Commission: the console-admin coverage audit
(`~/workspace/console-admin-audit-20261109/AUDIT.md` §4 —
ExcelAbilityBridge 0.1.0 verdict FAIL, packet sized TINY) under
Mike's Nov 3, 2026 console-admin rule: every Excel plugin ships
console-runnable admin commands. **Not installed.** The install
packet is `INSTALL-PACKET.md` in this folder; the MANIFEST entry
is drafted in `MANIFEST-ENTRY-DRAFT.md` for the supervisor to
file after verification (this lane did NOT touch the MANIFEST,
the parent-backlog, board state, or MEMORY.md). Records staged
at `~/workspace/records-catchup-20261103/packages/excelabilitybridge-0.1.1-20261110/`;
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). The
0.1.0 tree at `~/workspace/excelabilitybridge/` is untouched —
the 0.1.0 jar still hashes to its record value (verified at
close-out).

## Version re-base check (the supervisor's standing warning)

The audit's target for this packet was 0.1.1, and the label is
right: the live version is **0.1.0** (the only AbilityBridge
artifact on disk, recorded in `~/workspace/excelabilitybridge/
SOURCE.md` + `SHA256SUMS.txt`, built Oct 19, 2026; no later
AbilityBridge entry exists in the MANIFEST or its archive),
no 0.1.1 build existed anywhere on disk, so 0.1.1 = live +
one minor, no re-base needed.

## Base proof

- Base source of record: `~/workspace/excelabilitybridge/src/ExcelAbilityBridge/`
  (the 0.1.0 tree; `~/workspace/excelabilitybridge/SOURCE.md`
  records the artifacts).
- Live gate fingerprint re-verified on disk:
  `ExcelAbilityBridge-0.1.0.jar` — 28,048 B — SHA-256
  `51f7dd303ac33115135b4415e09c8de153a93d1437650cb4cb0587c8d80740c9`
  (matches the folder's SHA256SUMS.txt and SOURCE record).
- The unmodified 0.1.0 tree, rebuilt in a scratch copy for
  this packet, matches the live jar in the fleet's base-proof
  sense: **every class file is byte-identical** to the
  shipped jar, and its carried suite runs **150/150** (65 +
  65 in the two probe modes + 20 EliteMobs mapping checks).
  The rebuilt jar as a whole is NOT byte-identical, for one
  recorded resource diff: the shipped jar's plugin.yml
  carries `author: Argus` (684 B) where the source tree
  carries `authors: [Iris, Mike]` (692 B). The shipped jar
  was evidently built from a tree whose plugin.yml author
  line was edited after the source zip was made. **The
  0.1.1 tree resolves this in favour of the shipped jar:**
  its plugin.yml starts from the live jar's plugin.yml
  (author: Argus), so the 0.1.1 jar's delta vs live contains
  no unrelated author-line change.
- The 0.1.1 delta was applied to a copy of the 0.1.0 tree in
  `~/workspace/abilitybridge-011-build/ExcelAbilityBridge/`
  (copy verified byte-identical to the 0.1.0 tree with
  `diff -r` before any edit — the plugin.yml author line
  being the one deliberate, documented exception above);
  the 0.1.0 folder itself was never edited.
- Baseline in the new tree, before the delta: the carried
  suite ran **150/150**, matching the 0.1.0 SOURCE record
  count exactly.

## Candidate

- `ExcelAbilityBridge-0.1.1.jar` — **30,606 B** — SHA-256
  `62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55`
- Deterministic double-build byte-identical: five
  consecutive builds of this tree (the build session's two,
  two to scratch outputs, and the shipped-path build) all
  produced the hash above, and the shipped jar's entry
  inventory was verified class-by-class against the live
  jar (only the intended classes differ — see below).
- Build ID: this lineage carries no in-jar build-ID fingerprint
  — the artifact is identified by version 0.1.1 + the SHA-256
  above.
- `ExcelAbilityBridge-0.1.1-source.zip` — 30,608 B — SHA-256
  `66aa3b1682aa34674392df1280ee44c2f22ee128d390335efe0c7d93a951c6df`
  (the whole ExcelAbilityBridge/ tree, build/ excluded,
  following the TreeCleanup 0.1.1 zip layout; timestamps
  pinned 2026-11-10).
- Jar-level delta vs live 0.1.0: only `AbilitiesCommand.class`
  and `ExcelAbilityBridge.class` changed and
  `StatusCommand.class` was added; every other class is
  byte-identical to the live jar, config.yml is
  byte-identical, and plugin.yml differs only as listed
  below.

## What shipped (the whole change, nothing else)

Player-facing behaviour change: **none.** The form, the fire
path, the fail-soft shapes, and the ACS watch are
behaviour-unchanged. Change-scope diff vs the 0.1.0 tree —
only these files:

- `src/.../StatusCommand.java` (new) — Bukkit-free status
  logic: the permission constant, the gate arithmetic
  (`isAdmin`: console always, otherwise the node), the
  exact status-subcommand matcher, the denial text, and
  the status formatter, which builds its four lines purely
  from live facts passed in by the glue (EliteMobs
  present/version, gateway ACS readiness, Geyser/Floodgate
  presence, the debug flag) so the text can never claim a
  link state the glue did not observe.
- `src/.../AbilitiesCommand.java` — a `status` dispatch
  routed BEFORE the player-only gate, plus `handleStatus`
  (gate check, fact gathering from the plugin manager +
  the gateway seam, then the read-only lines). The player
  path below the dispatch is byte-for-byte the 0.1.0 logic.
- `src/.../ExcelAbilityBridge.java` — one package-private
  accessor, `debugLogFires()`, for the status surface.
  Nothing else in the main class changed.
- `plugin.yml` — based on the LIVE jar's plugin.yml (see
  Base proof): version 0.1.1; the command description and
  usage name the status form; NEW
  `excelabilitybridge.admin` permission (default: op).
  Deliberately NO command-level `permission:` key —
  /abilities is every Bedrock player's own menu, and a
  command-level node would lock non-op players out of it;
  the node is checked inside the status branch only.
- `tests/StatusCommandTestMain.java` (new, 24 checks) +
  two invocation lines in `run-tests.sh` (the suite runs
  in both probe modes, like the carried 65-check suite);
  `build.sh` version / output / date knobs.
- **No change:** config.yml (byte-identical, no new keys),
  the gateway/probe/forms/labels sources, and every other
  0.1.0 source and test file.

### The command, as built

- `/abilities status` — four lines, e.g.
  `ExcelAbilityBridge status: enabled (v0.1.1)` /
  `EliteMobs link: UP - EliteMobs 10.9.8 enabled, Advanced Combat System READY.` /
  `Geyser link: Geyser-Spigot enabled, floodgate enabled - Bedrock form transport available.` /
  `Config: debug-log-fires OFF. Permissions: excelabilitybridge.admin (default op) for this status; /abilities itself stays open to every player.`
  Before ACS initializes, the EliteMobs line honestly reads
  STANDING BY instead of UP (rig-proven, both shapes).
- The audit's sizing was followed exactly: ONE global status
  beside the unchanged player command. No per-player op
  exists because per-player ability state belongs to
  EliteMobs, not the bridge.

### State-model honesty (nothing invented)

- The status runs on observed state only: EliteMobs
  presence/version from the plugin manager, ACS readiness
  through the bridge's own `AcsGateway.moduleReady()` — the
  same probe the player path trusts — and Geyser/Floodgate
  presence from the plugin manager.
- One shape is unreachable by design and says so: if
  EliteMobs is absent the plugin disables itself (0.1.0
  fail-soft), so the DOWN status line can never be observed
  on a live boot. It exists so the formatter is total, it
  is harness-pinned, and its own text states the stand-down.
- Status is read-only by construction: it probes and fires
  nothing.

## Tests

- **Desk: 174 distinct checks, all passing** via
  `run-tests.sh` — the existing **150-check** suite (65 +
  65 probe-mode runs + 20 EliteMobs mapping checks) passes
  unchanged, plus the new **StatusCommandTestMain: 24
  checks** (gate arithmetic: console allowed, node-holder
  allowed, non-admin denied; the exact-word matcher; the
  exact status texts for the UP / STANDING BY / DOWN link
  shapes, complete/incomplete Geyser shapes, unknown-version
  honesty, and the config flag following its input; the
  plugin.yml gate: version 0.1.1, the admin node defaulting
  to op, the usage naming the status form, and NO
  command-level permission). The new suite runs in both
  probe modes, so raw executions total 198.
- **Deterministic double-build byte-identical** at the
  shipped hash (see Candidate).
- **Rig** (Paper 26.2-123 + the pinned EliteMobs 10.9.8 +
  Geyser + floodgate, commands typed on the real console
  stdin, `rig-0.1.1/`): clean 0.1.1 enable, the ACS watch
  transition exactly as in 0.1.0, SEVERE 0, clean stop,
  **7/7 behavior checks** — all four status lines verbatim
  from console with the EliteMobs link reading UP after the
  transition, and the bare-command console line byte-for-
  byte the 0.1.0 one. A first run additionally proved the
  STANDING BY status shape verbatim (status sent before
  ACS finished initializing). Full table + limitations in
  `rig-0.1.1/RIG-RESULTS.md`; log: `rig-0.1.1/boot.log`.

## Surfaces checked (expected no-change, confirmed)

- Guide knowledge/intents: operator-only command — no player
  teaching owed; nothing added.
- Website pair, What's New BOTH surfaces, broadcaster tips,
  in-game menus (ExcelMenu / ExcelBedrockMenu): no
  player-facing claim about AbilityBridge admin exists to
  correct; no changes made.
- The one documentation surface owed — an operator-facing
  note that console status for AbilityBridge now exists —
  is this build record plus the install packet's
  console-smoke section.

## Live proof owed after install (not part of the build)

One console `abilities status` read back in the server log
(ideally after the "bridge online" line, so the EliteMobs
link reads UP). No player-visible check is owed — no player
surface changes. The 0.1.0 player proofs (a real Bedrock
ability fire on the live server) remain whatever they were
before this packet; 0.1.1 neither discharges nor changes
them.
