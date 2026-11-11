# ExcelTreeCleanup 0.1.1 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 10, 2026.
Commission: the console-admin coverage audit (Nov 9, 2026,
`~/workspace/console-admin-audit-20261109/AUDIT.md` §4) under
Mike's Nov 3, 2026 console-admin rule.

## Provenance

- Project tree: `~/workspace/treecleanup-011-build/ExcelTreeCleanup/`
  (plugin.yml version 0.1.1, main
  com.winkreyes.exceltreecleanup.ExcelTreeCleanup,
  api-version '1.13').
- Base: the 0.1.0 tree
  `~/workspace/exceltreecleanup/src/ExcelTreeCleanup/` (the
  source of record for the live jar; INSTALLED.txt in that
  folder records the Oct 20, 2026 live install). Shipped 0.1.0
  jar SHA-256
  `0507acefca25ec83858d25ab9dfbd17c47f230fd708939a86b357080bb883423`
  (20,515 B) re-verified on disk Nov 10, 2026; the 0.1.0 folder
  is untouched by this build, and the copied tree was verified
  byte-identical before the delta was applied.
- Built by `build.sh` (the house deterministic recipe from
  0.1.0: manual javac, JDK 25 at `~/workspace/.jdk25`,
  `--release 21`, house Paper-API + kyori 4.20 classpath, jar
  entry timestamps pinned). Runtime target is the live Paper
  server; every API used is long-stable Bukkit API.
- Ownership: all code is ours (Iris / Mike lineage). NO runtime
  dependencies beyond the 0.1.0 set (Paper itself;
  GriefPrevention stays a soft-depend reached by reflection).

## What 0.1.1 changes vs 0.1.0 (the whole delta)

CONSOLE ADMIN. The audit found 0.1.0 a FAIL: /treecleanup
on|off was self-only and player-gated, so an operator could
neither inspect nor fix another player's toggle. In short:

- NEW `core/AdminCommand`: Bukkit-free admin logic — the
  `exceltreecleanup.admin` gate arithmetic (console always;
  otherwise the node), the on|off parser, the exact
  usage/denial texts, the status formatter over ToggleStore's
  real `isEnabled`/`hasExplicit` (effective state +
  explicit-vs-default), and `applySet` writing through
  ToggleStore's real `set`.
- `ExcelTreeCleanup`: an `admin` dispatch in `onCommand`
  routed BEFORE the player-only gate; `handleAdmin` +
  `resolveAdminTarget` (online exact name first, then raw
  UUID — the house shape; no name→UUID record is invented).
  Set persists via the self toggle's own saveToggles() and
  leaves an audit line in the log.
- `plugin.yml`: version 0.1.1, usage/description name the
  admin forms, NEW `exceltreecleanup.admin` permission
  (default: op) declared — deliberately NOT a command-level
  permission, so non-op players keep their own toggle.
- Tests: new `AdminCommandTest` (38 checks) + TestMain
  registration. NOTHING else: config.yml is byte-identical to
  0.1.0 (no new keys), and RemnantAnalyzer / TriggerGate /
  ToggleStore / ClaimGateway and all cleanup behaviour are
  byte-for-byte the 0.1.0 ones.
- Explicitly NOT built (outside the audit's SMALL sizing):
  any offline name→UUID lookup, any change to cleanup
  behaviour, any new config key.

## Artifacts

- ExcelTreeCleanup-0.1.1.jar — 24,389 bytes
  SHA-256 66e5f4a431fa920fc9f7a9eb582d045f0486b9bc2356555e0d8b422778dc21a7
  Deterministic double-build byte-identical. (No in-jar
  build-ID fingerprint exists in this lineage; version + hash
  identify it.)
- ExcelTreeCleanup-0.1.1-source.zip — 28,526 bytes
  SHA-256 54cd3b497ae7a11e9d04e4a08be828f67d3d0d0da5e10ade8976e56aa758da78
  (the whole ExcelTreeCleanup/ tree, build/ excluded as in the
  0.1.0 zip; timestamps pinned 2026-11-10.)

## Verification summary

Desk 122/122 (84 carried 0.1.0 checks unchanged + 38 new
AdminCommand checks). Rig on Paper 26.2-123 with
GriefPrevention 16.18.7 and the 0.1.0 TcRigDriver fake players
(rig-0.1.1 in the build folder): clean 0.1.1 enable (seeded
toggle loaded, claim checks ON), zero SEVERE, clean stop,
20/20 behavior checks typed on the real console stdin —
status for explicit-OFF / default-ON records (verbatim),
admin set by UUID and by online player name with audit lines,
the bad-name rejection, every malformed-usage form changing
nothing, the unchanged console guidance for the bare self
command, the driver's R6 self-toggle case re-proven end to
end, and the on-disk proof that status reads create nothing.
Rig run 1 rejected the first jar's plugin.yml (unquoted
description containing ": ") — fixed by double-quoting, pinned
by a new harness check, rebuilt; run 2 is the shipped jar.
Limitations (offline targets resolve by UUID only; the
non-admin denial is harness-pinned, not rig-observed) are
stated in rig-0.1.1/RIG-RESULTS.md.
NOT installed — built + staged only.
