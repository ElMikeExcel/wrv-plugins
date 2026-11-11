# ExcelMenuJoin 1.4.4 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 10, 2026.
Commission: the console-admin coverage audit (Nov 9, 2026,
`~/workspace/console-admin-audit-20261109/AUDIT.md` §2/§4) under
Mike's Nov 3, 2026 console-admin rule. The audit recommended
the number "1.4.2" off its 1.4.1 source tree; that number (and
a 1.4.3 after it) had already shipped, so the packet is
re-based to 1.4.4 on the shipped 1.4.3 tree — see the
BUILD-RECORD.md re-base correction.

## Provenance

- Project tree: `~/workspace/menujoin-142-build/ExcelMenuJoin/`
  (plugin.yml version 1.4.4, main
  com.winkreyesvalley.excelmenujoin.ExcelMenuJoin,
  api-version '1.13').
- Base: the shipped 1.4.3 packet tree
  `~/workspace/moon-packets-audit/inspect/menujoin143/ExcelMenuJoin-1.4.3/`
  (the source of record, pinned by the Nov 3, 2026
  source-coverage index: the requal rebuild of this tree
  reproduces every shipped class byte-identically). Shipped
  1.4.3 jar SHA-256
  `178febcce9da81127e25dce07b99a985db63a91dbbfde5d7a434283794f9bfd7`
  (11,678 B) re-verified on disk Nov 10, 2026, and the packet's
  own SHA256SUMS verify clean; installed live Oct 30, 2026
  (`~/workspace/requal/menujoin143/INSTALLED.txt` + the Oct 30
  daily log). The base folder is untouched by this build, and
  the copied tree was verified byte-identical before the
  delta was applied.
- Built by `build.sh` (the house deterministic recipe: manual
  javac, JDK 25 at `~/workspace/.jdk25`, `--release 21`,
  excelshops build-libs Paper API + kyori 4.20 classpath,
  PlaceholderAPI 2.12.3 compile-time only, jar entry
  timestamps pinned, sorted entry list). The 1.4.3 packet's
  own builder was Moon's build-and-test.py; it is not carried
  — the house recipe reproduces three of the shipped jar's
  four unchanged classes byte-identically (the fourth, the
  JoinTicket record, differs only through the JDK patch
  stamp, 25.0.4.1 vs 25.0.4 — disclosed in BUILD-RECORD.md).
  Runtime target is the live Paper server; every API used is
  long-stable Bukkit API.
- Ownership: all code is ours (Iris / Mike lineage, incl.
  Moon's 1.4.3 packet). NO runtime dependencies beyond the
  1.4.3 softdepend set (PlaceholderAPI, ExcelPouch — both
  still softdepend and reached only when present); no
  third-party code is included in the jar.

## What 1.4.4 changes vs 1.4.3 (the whole delta)

CONSOLE ADMIN. The audit found ExcelMenuJoin a FAIL (read off
1.4.1; verified still true at 1.4.3): /excelmenujoin and
/compass were both self-only and player-gated, so the console
could neither inspect nor fix another player's join-menu
state, nor hand them a replacement compass. In short:

- NEW `AdminCommands` (Bukkit-free): the gate decision
  (console explicitly allowed; any other sender needs
  `excelmenujoin.admin`) and the exact reply texts. Status
  formats the player's real state (the PDC auto-open flag,
  inverted and spelled out as ON/OFF, plus live-inventory
  compass presence) and mutates nothing.
- `ExcelMenuJoin`: an `admin` dispatch in the toggle command
  ahead of the player-only gate; `handleAdmin` checks the
  gate, resolves the target to an ONLINE player by exact
  name only, and acts through the plugin's existing methods —
  status via isDisabled/hasMenuCompass, toggle via
  setDisabled (the self toggle's own dual-key write), compass
  via the single giveMenuCompass path (so the 1.4.0 Utility
  Pouch no-duplicate rule covers admin gives too). Toggle and
  compass outcomes are logged. The self-service forms, the
  1.4.2 CompassTap air-tap fix and the 1.4.3 session-bound
  join / preprocess-policy code are byte-for-byte the 1.4.3
  ones.
- `plugin.yml`: version 1.4.4, usage/description name the
  admin forms, new `excelmenujoin.admin` (default op). The
  command-level `excelmenujoin.toggle` permission and both
  player nodes are unchanged; the admin gate lives in code so
  the self forms stay open to every player.
- Tests: new `AdminCommandsTest` (23 checks) + 7 new
  PluginYmlGuardTest wiring checks + TestMain registration;
  house build.sh / run-tests.sh. NOTHING else: this plugin
  has no config file (none in 1.4.3, none added), and
  CompassTap / ExcelMenuJoinExpansion / PouchCompassCheck are
  byte-for-byte the 1.4.3 sources (and, for the three plain
  classes, byte-identical classes in the jar too).
- Explicitly NOT built (outside the audit's SMALL sizing):
  any offline/UUID target form (the state lives on the
  player; no name→UUID record exists or is invented), any
  give/join/tap behaviour change, and any menu/placeholder
  change.

## Artifacts

- ExcelMenuJoin-1.4.4.jar — 14,080 bytes
  SHA-256 4df80550fa4ecf653534c32c5eacd597ad4f24fd0fd8d1af0956cf71ee7c5068
  Deterministic double-build byte-identical. (No in-jar
  build-ID fingerprint exists in this lineage; version + hash
  identify it.)
- ExcelMenuJoin-1.4.4-source.zip — 20,624 bytes
  SHA-256 29e63d8cf7bb89e0944945f55afd7dfba47f3a1f3459721ddac8b64f74f05fc4
  (the whole ExcelMenuJoin/ tree; timestamps pinned
  2026-11-10.)

## Verification summary

Desk 51/51 (21 carried 1.4.3 checks — the pouch suite, the
no-depend guard set and CompassTap unchanged — + 7 new guard
wiring checks + 23 new AdminCommands checks). Rig on a
minimal Paper 26.2-123 server with the 1.4.4 jar + the live
PlaceholderAPI 2.12.3 and a fake-player driver (rig-1.4.4 in
the build folder): clean 1.4.4 enable with the menujoin
expansion registered, zero SEVERE, clean stop, 27/27 behavior
checks dispatched through the real console sender — status
in both flag states, both toggle directions with independent
PDC verification in both namespaces, the compass give and
the no-duplicate refusal, all three bad-name rejections, the
usage forms, the console self-form gate lines verbatim from
1.4.3, the 1.4.3 session-bound join give, and the player's
own toggle + /compass regressions. Limitations (targets are
online-by-name only — the state model, stated in the build
record; the menu-open chain was not in this minimal rig —
it is untouched 1.4.2/1.4.3 code proven in the 1.4.1
full-stack rig and Moon's 1.4.3 native qualification; the
pouch-parked give is desk-pinned against the same give path;
the non-op denial is harness-pinned, not rig-observed) are
stated in rig-1.4.4/RIG-RESULTS.md.
NOT installed — built + staged only.
