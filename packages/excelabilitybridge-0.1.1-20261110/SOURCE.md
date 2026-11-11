# ExcelAbilityBridge 0.1.1 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 10, 2026.
Commission: the console-admin coverage audit (Nov 9, 2026,
`~/workspace/console-admin-audit-20261109/AUDIT.md` §4) under
Mike's Nov 3, 2026 console-admin rule.

## Provenance

- Project tree: `~/workspace/abilitybridge-011-build/ExcelAbilityBridge/`
  (plugin.yml version 0.1.1, main
  com.winkreyes.excelabilitybridge.ExcelAbilityBridge,
  api-version "1.13").
- Base: the 0.1.0 tree
  `~/workspace/excelabilitybridge/src/ExcelAbilityBridge/`
  (the source of record for the live jar). Shipped 0.1.0 jar
  SHA-256
  `51f7dd303ac33115135b4415e09c8de153a93d1437650cb4cb0587c8d80740c9`
  (28,048 B) re-verified on disk Nov 10, 2026; the
  unmodified tree rebuilt in a scratch copy matches the
  shipped jar class-for-class byte-identically, with ONE
  recorded resource diff (the shipped jar's plugin.yml
  carries `author: Argus` where the tree carries
  `authors: [Iris, Mike]`). The 0.1.1 plugin.yml starts
  from the shipped jar's plugin.yml, resolving that diff in
  the shipped jar's favour. The 0.1.0 folder is untouched
  by this build.
- Dependencies (unchanged from 0.1.0): Paper API 26.2 build
  123 (compile classpath only, house build-libs);
  EliteMobs 10.9.8 jar COMPILE-ONLY, pinned by MD5 in
  build.sh (`13991443728680f1b38ac5019bd2b9f3`); no
  EliteMobs classes packaged; Geyser/Floodgate touched only
  by reflection at runtime, never linked.
- Built by `build.sh` (manual javac, JDK 25, `--release 21`,
  sorted entry list + pinned zip timestamps for
  byte-identical rebuilds).
- Ownership: all code is ours (Iris / Mike lineage). NO new
  runtime dependencies.

## What 0.1.1 changes vs 0.1.0 (the whole delta)

CONSOLE ADMIN. The audit found 0.1.0 a FAIL: /abilities was
player-only, so an operator could not inspect the bridge's
link state from the console. In short:

- NEW `StatusCommand`: Bukkit-free status logic — the
  `excelabilitybridge.admin` gate arithmetic (console
  always; otherwise the node), the exact-word status
  matcher, the denial text, and the four-line status
  formatter built purely from live facts passed in by the
  glue (EliteMobs present/version, gateway ACS readiness,
  Geyser/Floodgate presence, the debug flag).
- `AbilitiesCommand`: a `status` dispatch routed BEFORE
  the player-only gate, plus `handleStatus`. The player
  path below the dispatch is byte-for-byte 0.1.0.
- `ExcelAbilityBridge`: one package-private accessor
  (`debugLogFires()`). Nothing else.
- `plugin.yml`: version 0.1.1, usage/description name the
  status form, NEW `excelabilitybridge.admin` permission
  (default: op) declared — deliberately NOT a
  command-level permission, so every Bedrock player keeps
  their own /abilities menu.
- Tests: new `StatusCommandTestMain` (24 checks) run in
  both probe modes. NOTHING else: config.yml is
  byte-identical to 0.1.0 (no new keys), and the gateway /
  probe / forms / fire paths are byte-for-byte the 0.1.0
  ones (verified at jar level).
- Explicitly NOT built (outside the audit's TINY sizing):
  any per-player op — per-player ability state belongs to
  EliteMobs, not the bridge.

## Artifacts

- ExcelAbilityBridge-0.1.1.jar — 30,606 bytes
  SHA-256 62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55
  Deterministic double-build byte-identical. (No in-jar
  build-ID fingerprint exists
  in this lineage; version + hash identify it.)
- ExcelAbilityBridge-0.1.1-source.zip — 30,608 bytes
  SHA-256 66aa3b1682aa34674392df1280ee44c2f22ee128d390335efe0c7d93a951c6df
  (the whole ExcelAbilityBridge/ tree, build/ excluded,
  TreeCleanup-0.1.1 zip layout; timestamps pinned
  2026-11-10.)

## Verification summary

Desk 174 distinct checks, all passing (150 carried 0.1.0
checks unchanged + 24 new StatusCommand checks; the new
suite runs in both probe modes, 198 raw executions). Rig
on Paper 26.2-123 with the pinned EliteMobs 10.9.8 +
Geyser + floodgate (rig-0.1.1 in the build folder): clean
0.1.1 enable, the ACS watch transition exactly as in
0.1.0, zero SEVERE, clean stop, 7/7 behavior checks typed
on the real console stdin — all four status lines verbatim
with the EliteMobs link UP after the transition, the
bare-command console line byte-for-byte 0.1.0; a first run
also proved the STANDING BY status shape verbatim.
Limitations (the non-admin denial is harness-pinned, not
rig-observed; the DOWN status shape is unreachable on a
normal boot by the plugin's own stand-down design) are
stated in rig-0.1.1/RIG-RESULTS.md.
NOT installed — built + staged only.
