# ExcelChunks 0.1.1 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 10, 2026.
Commission: the console-admin coverage audit (Nov 9, 2026,
`~/workspace/console-admin-audit-20261109/AUDIT.md` §4) under
Mike's Nov 3, 2026 console-admin rule.

## Provenance

- Project tree: `~/workspace/chunks-011-build/ExcelChunks/`
  (plugin.yml version 0.1.1, main
  com.winkreyes.excelchunks.ExcelChunks, api-version '1.13').
- Base: the 0.1.0 tree `~/workspace/excelchunks/src/ExcelChunks/`
  (the source of record for the live jar; INSTALLED-0.1.0.txt
  in that folder records the Oct 23, 2026 live install).
  Shipped 0.1.0 jar SHA-256
  `cf4f08c835ba4ec1314b0b12460367e98d6f857dd4bd37ad16e6faaee6ce2948`
  (7,150 B) re-verified on disk Nov 10, 2026; the unmodified
  tree rebuilt in a scratch copy reproduces that jar
  byte-identically, and the 0.1.0 folder is untouched by
  this build (the copied tree was verified byte-identical
  before the delta was applied).
- Built by `build.sh` (the house deterministic recipe from
  0.1.0: manual javac, JDK 25 at `~/workspace/.jdk25`,
  `--release 21`, house Paper-API + kyori 4.20 classpath, jar
  entry timestamps pinned). Runtime target is the live Paper
  server; every API used is long-stable Bukkit API.
- Ownership: all code is ours (Iris / Mike lineage). NO
  runtime dependencies beyond the 0.1.0 set (Paper itself).

## What 0.1.1 changes vs 0.1.0 (the whole delta)

CONSOLE ADMIN. The audit found 0.1.0 a FAIL: /chunks was
self-only, so an operator could not even inspect the
plugin's state from the console. In short:

- NEW `core/StatusCommand`: Bukkit-free status logic — the
  `excelchunks.admin` gate arithmetic (console always;
  otherwise the node), the exact-word status matcher, the
  denial text, and the three-line status formatter built
  from the plugin's real constants.
- `ExcelChunks`: a `status` dispatch in `onCommand` routed
  BEFORE the player-only gate, plus `handleStatus`. The
  self resync below the dispatch is byte-for-byte 0.1.0.
- `plugin.yml`: version 0.1.1, usage/description name the
  status form, NEW `excelchunks.admin` permission
  (default: op) declared. The command-level
  `permission: excelchunks.use` is unchanged.
- Tests: new `StatusCommandTest` (29 checks) + TestMain
  registration. NOTHING else: CooldownStore / ItemSelection
  and all resync behaviour are byte-for-byte the 0.1.0
  ones; there is still no config file and no persistence.
- Explicitly NOT built (outside the audit's TINY sizing):
  any per-player op — there is no persistent per-player
  state; the in-memory 10s cooldown is the only per-player
  state and expires on its own.

## Artifacts

- ExcelChunks-0.1.1.jar — 8,784 bytes
  SHA-256 8d9b47e3dfaa453322915fd216ddcf4820f15c3c33e6418f76a7cc7c19b33877
  Deterministic double-build byte-identical. (No in-jar
  build-ID fingerprint exists in this lineage; version +
  hash identify it.)
- ExcelChunks-0.1.1-source.zip — 14,091 bytes
  SHA-256 a5545334aa4ffe900fd0a2365d7e548066941a4ced6b9fcfc9c99b5a3298e4cc
  (the whole ExcelChunks/ tree, build/ excluded as in the
  0.1.0 zip; timestamps pinned 2026-11-10.)

## Verification summary

Desk 89/89 (60 carried 0.1.0 checks unchanged + 29 new
StatusCommand checks). Rig on Paper 26.2-123 with
Geyser-Spigot 2.11.3 + floodgate 2.2.5 (the 0.1.0 full-stack
rig stack, rig-0.1.1 in the build folder): clean 0.1.1
enable, zero SEVERE, clean stop, 6/6 behavior checks typed
on the real console stdin — all three status lines
verbatim, the bare-command console refusal byte-for-byte
the 0.1.0 line, the plugin list at 0.1.1. Limitations (the
non-admin denial is harness-pinned, not rig-observed — the
rig has no player sender) are stated in
rig-0.1.1/RIG-RESULTS.md.
NOT installed — built + staged only.
