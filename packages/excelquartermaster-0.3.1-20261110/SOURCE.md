# ExcelQuartermaster 0.3.1 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 10, 2026.
Commission: the console-admin coverage audit (Nov 9, 2026,
`~/workspace/console-admin-audit-20261109/AUDIT.md` §2/§4) under
Mike's Nov 3, 2026 console-admin rule.

## Provenance

- Project tree: `~/workspace/quartermaster-031-build/ExcelQuartermaster/`
  (plugin.yml version 0.3.1, main
  com.winkreyes.excelquartermaster.ExcelQuartermaster,
  api-version '1.13').
- Base: the 0.3.0 tree
  `~/workspace/excelquartermaster-0.3.0/src/ExcelQuartermaster/`
  (the newest authoritative tree on disk — no ExcelQuartermaster
  records package exists in
  `~/workspace/records-catchup-20261103/packages/`). Shipped
  0.3.0 jar SHA-256
  `70096fc47b61f09131aa42c1f3a66fbee3c248085d3d39f476338cb01b15765a`
  (77,066 B) re-verified on disk Nov 10, 2026; the 0.3.0 folder
  is untouched by this build, and the copied tree was verified
  byte-identical before the delta was applied.
- Built by `build.sh` (the house deterministic recipe from
  0.3.0: manual javac, JDK 25 at `~/workspace/.jdk25`,
  `--release 21`, house Paper-API + kyori 4.20 classpath, jar
  entry timestamps pinned). Runtime target is the live Paper
  server; every API used is long-stable Bukkit API.
- Ownership: all code is ours (Iris / Mike lineage). NO runtime
  dependencies beyond the 0.3.0 softdepend set, all still
  reached by reflection; no third-party code is included in
  the jar.

## What 0.3.1 changes vs 0.3.0 (the whole delta)

CONSOLE ADMIN INSPECTION. The audit found 0.3.0 a FAIL: the
console could fix a player's pack state (`/qm packreset`,
0.2.2) but could not inspect it — no status at all. In short:

- NEW `PackStatus`: pure-logic formatting of one player's real
  PackPrompt record — offers used / remaining (PackPrompt's own
  arithmetic) and pack state (the stored outcome, with the
  accepted and exhausted terminal shapes spelled out, and the
  verbatim last-change timestamp). Read-only by construction.
- `ExcelQuartermaster`: a `status` dispatch in `onCommand`
  ahead of the packreset shape check; `handleStatus` resolves
  the target exactly as packreset does (online name first, then
  raw UUID for offline players) and only reads
  `packPrompt.record(uuid)` — nothing is mutated or saved. A
  bare/malformed `/qm` prints both usage lines.
- `plugin.yml`: version 0.3.1, usage and the existing
  `excelquartermaster.admin` description name the status form.
  The permission node itself is unchanged (default op; the
  console satisfies it, as packreset already relies on).
- Tests: new `PackStatusTest` (27 checks) + run-tests.sh
  registration. NOTHING else: config.yml is byte-identical to
  0.3.0 (no new keys), and PackPrompt / PackReset /
  PackStateStore and all tale, pack-offer, and wand-issue code
  are byte-for-byte the 0.3.0 ones.
- Explicitly NOT built (outside the audit's TINY sizing): any
  new mutating op, any name→UUID lookup for offline players,
  and any tale-rotation / issued-wand reporting in status.

## Artifacts

- ExcelQuartermaster-0.3.1.jar — 79,785 bytes
  SHA-256 a11aa0fcab31544ffeb92c6ab31207412e797bb8d1147eb73d34e4375d0fa2f9
  Deterministic double-build byte-identical. (No in-jar
  build-ID fingerprint exists in this lineage; version + hash
  identify it.)
- ExcelQuartermaster-0.3.1-source.zip — 101,342 bytes
  SHA-256 5b9916c14aa46ef50142a3c23d86af2521b36964f443c7fbd0af83a37c99ff79
  (the whole ExcelQuartermaster/ tree; timestamps pinned
  2026-11-10.)

## Verification summary

Desk 602/602 (575 carried 0.3.0 checks unchanged + 27 new
PackStatus checks). Rig on a minimal Paper 26.2-123 server
with ONLY the 0.3.1 jar and a seeded java-pack-state.txt
(rig-0.3.1 in the build folder): clean 0.3.1 enable, zero
SEVERE, clean stop, 12/12 behavior checks typed on the real
console stdin — status lines for DECLINED mid-count, LOADED
accepted, and DECLINED exhausted records (verbatim), the
no-record line for an unknown UUID, the bad-name rejection
("Nothing was looked up."), both usage lines on a bare /qm,
the packreset regression (RESET on a status-read target, then
status reads no-record), and the on-disk proof that the status
reads created and changed nothing. Limitations (offline
targets resolve by UUID only — packreset's own shape, matched
deliberately; no player joined, as no player surface changed)
are stated in rig-0.3.1/RIG-RESULTS.md.
NOT installed — built + staged only.
