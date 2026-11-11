# ExcelBuildMode 1.3.4 — combined rebuild (design + recovery record)

An Excel plugin note for Wink Reyes Valley. November 10, 2026.
Commissioned by board-supervisor decision D5 (Nov 10, 2026) after the
live-jar identification in `~/workspace/buildmode-133-build/LIVE-JAR-IDENTIFICATION-20261110.md`.

## The collision this rebuild resolves

Two different builds were both numbered 1.3.3:

1. **The October "reviewed" build** — LIVE on the server since the
   Oct 23–25, 2026 batch (jar 90,044 B, SHA-256 3662dcb7…). Built on
   1.3.2; adds `excelbuildmode.reviewed`. No source or build record
   for it survived on disk.
2. **The Nov 10, 2026 console-admin build** — built from the 1.3.2
   tree in `~/workspace/buildmode-133-build/`; adds
   `excelbuildmode.admin` and the `/buildmode admin …` surface. It
   contains NO reviewed code: installing it over the live jar would
   have silently regressed the reviewed-builder flow (the flow the
   server's eligible builder uses). The install failsafe's live-jar
   gate caught exactly this on Nov 10, 2026 and held the packet.

1.3.4 is the combined rebuild: the console-admin tree PLUS the
reviewed delta, recovered from the preserved live jar, at a new
version number so neither 1.3.3 artifact can be confused with it.

## The recovered delta (summary; full record in the build folder)

`REVIEWED-DELTA-RECOVERY.md` (build folder root) carries the method
and the evidence: a 48-entry byte diff isolates the delta to
`plugin.yml`, `paper/BuildPolicy.class`, and `paper/ExcelBuildMode.class`
(every other class byte-identical to genuine 1.3.2), and normalized
`javap` disassembly pins the semantics exactly:

- `BuildPolicy.REVIEWED_PERMISSION = "excelbuildmode.reviewed"`.
- `BuildPolicy.refusesOutsideWorldEdit(worldEdit, reviewed, hasOutside)`
  = `worldEdit && !reviewed && hasOutside` — the entry refusal in
  `ExcelBuildMode.enter` runs through it, so a reviewed holder is not
  refused for standing WorldEdit; a non-reviewed holder still is.
- `BuildPolicy.probesResidualWorldEdit(worldEdit, reviewed)`
  = `worldEdit && !reviewed` — the exit residual-permission probe in
  `ExcelBuildMode.removeAttachment` runs through it, so a reviewed
  builder's expected standing WorldEdit does not fail the exit; a
  non-reviewed builder's residual WorldEdit still does.
- plugin.yml: the reviewed permission block (default false), carried
  verbatim from the preserved jar.

Faithfulness proof: a control build applying ONLY the recovered delta
to the genuine 1.3.2 tree compiles to classes whose normalized javap
output matches the preserved jar's two differing classes
instruction-for-instruction (see BUILD-RECORD.md).

## What 1.3.4 is, whole

1.3.2 + the reviewed exemption (recovered) + the console admin
surface (PILOT-1.3.3.md, unchanged). plugin.yml declares version
1.3.4 and BOTH permission nodes: `excelbuildmode.reviewed`
(default false) and `excelbuildmode.admin` (default op). config.yml
is untouched since 1.3.2. The startup banner gains a 1.3.4 line
naming the combined rebuild; the reviewed build's own banner was the
plain 1.3.2 text, so no historical banner line is lost.

## Proof shape

- Eight suites via build.sh: the six carried 1.3.2 suites at their
  totals, AdminTests (console admin; version pin moved to 1.3.4),
  and NEW ReviewedTests pinning the recovered truth tables (entry:
  refusal still applies to non-reviewed holders; reviewed holders
  skip. Exit: probe still runs for non-reviewed holders; reviewed
  holders skip), the plugin.yml pins for both nodes, and the
  probe-set/session-attachment exclusion of the reviewed node.
- Deterministic double-build + rebuild from the shipped source zip.
- Control-build bytecode proof of the recovery (above).
- Rig boot on Paper 26.2-123 + WorldEdit 7.4.5 (`rig-1.3.4/`): clean
  1.3.4 enable + banner, zero SEVERE, the 1.3.3 console exercise
  re-run (7/7). Rig limitation, stated plainly: the rig has no real
  client, so the reviewed entry/exit behavior with a live player is
  proven by the harness truth tables + the bytecode match + the
  install packet's post-install player proof — not by a rig session.
