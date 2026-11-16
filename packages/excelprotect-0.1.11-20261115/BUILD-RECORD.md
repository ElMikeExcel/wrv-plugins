# BUILD RECORD - ExcelProtect 0.1.11 "EM Attempts" (P-PROTECT-EM-1, Option A)

Board supervisor run 403 build worker. BUILT, tested, deterministic.
NOT filed, NOT installed, MANIFEST.md untouched, live server untouched.
Rig (U7) NOT run in this task by instruction - see per-unit results.

## Candidate (structured evidence)

- Jar: ~/workspace/protect-0111-build-20261115/ExcelProtect-0.1.11.jar
  (also at build-tree/ExcelProtect-0.1.11.jar in the same folder)
  303587 B / SHA-256 ba10bbcd84de50f199f78b1e09d354ee189f8919ba9732d654ed2a4bcc94260f
- Build ID: EXC-PROTECT-0.1.11-WRV-20261115-01-0d1a11 (verified inside
  the jar: excel-build.txt; plugin.yml version 0.1.11; YAML parse PASS)
- Source zip: ~/workspace/protect-0111-build-20261115/excelprotect-0.1.11-source.zip
  210763 B / SHA-256 ef13e67fcdf7aa6418aad56b1b301d3a42ef2d316b2acc38dc8d1d137b6db262
- Build folder: ~/workspace/protect-0111-build-20261115/
  (build-tree/ = full 0.1.11 tree, evidence/, work/base-gate/,
  INSTALL-PACKET.md draft, RECEIPTS.md)
- Decisions applied: D71 (Option A only) + D72 binding calls -
  Q1 configurable scope defaulting to em_ prefix + explicit config
  list override; Q2 coalesce default 2000 ms, clamp 250-30000;
  Q3 separate canonical stream "attempts"; Q5 block = attempted
  placed type (event.getBlock()); Q6 NO held-item field;
  Q7 NO alerting, NO new command.

## Base gate (U1)

- PASS. Unmodified 0.1.10 sources (copied from
  ~/workspace/board-r364/protect-watch-build/build/, never edited in
  place) rebuilt byte-identical: 295195 B / SHA-256
  dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61,
  suite 107/107, fingerprint gate PASS.
  Evidence: protect-0111-build-20261115/evidence/base-gate-build.log

## Tests

- Suite: 117 found / 117 successful / 0 failed, on BOTH deterministic
  builds (107 base cases unchanged + 10 new cases, all in
  ProtectTest.java): attemptCoalescerMergesWithinWindow,
  attemptCoalescerSplitsOnGapExpiry, attemptCoalescerSplitsOnKeyChange
  (player/position/action/block/world), attemptCoalescerClosesOnQuitAndShutdown,
  attemptRecordShape (top-level player/player_uuid, cancelled=true,
  no before/after/sign_before/sign_after), attemptNeverRollbackSelectable
  (chooseBlocks + the rollback query shape), attemptProjectsToPlayerTimeline
  (role actor from the top-level pair; unknown UUID projects to nobody),
  attemptExporterFilters (accepts-level + scan-level over day files),
  attemptInspectSummaryShape (one-liner + x<count> when count>1 +
  attempts section immediately after blocks),
  attemptSettingsDefaultsAndClamps (toggle ON, 2000 ms, 250-30000
  clamp, prefix default covers em_primis / em_adventurers_guild /
  em_id_primis_wolf_den_1 and not world; list adds a world; empty
  prefix scopes by list alone).

## Per-unit results (scoping units U1-U8)

- U1 base gate: PASS (above).
- U2 pure core N1 BlockAttempts.java + tests 1-5: DONE. Coalescer keyed
  by (player UUID, world, x, y, z, action, block); feed merges inside
  the gap and closes+splits on expiry; tick closes expired; closePlayer
  (quit) and closeAll (shutdown); closed reasons window/quit/shutdown;
  render carries exactly the scoping field list (type block_attempt,
  top-level player/player_uuid, action, block, cancelled=true, count,
  first_ts_ms, last_ts_ms, closed) and no prior-state fields.
- U3 glue N2 BlockAttemptWatch.java + M1 ExcelProtect.java + M2
  Settings.java + M5 config.yml: DONE. Two handlers only
  (BlockBreakEvent, BlockPlaceEvent), MONITOR / ignoreCancelled=false,
  cancelled-only + EM-scope-only, read-only, full Throwable guard;
  quit closes the player's attempts; 20-tick timer drives tick();
  onDisable closes with "shutdown"; gated on log.attempts.enabled;
  READY/status version literals moved to 0.1.11; attempts log line in
  house style; inspect-at scan list gains "attempts".
- U4 read side M3 Exporter.java + M4 InspectReport.java + M1d: DONE.
  Exporter.STREAMS appends "attempts"; SECTION_ORDER gains "attempts"
  immediately after "blocks"; summarize gains the block_attempt case.
- U5 version/config/tooling M6 plugin.yml + M7 local-build.sh + full
  suite green: DONE. Version 0.1.11, description gains the 0.1.11 EM
  block attempts clause, jar name / fingerprint marker
  (EXC-PROTECT-0.1.11-WRV-20261115-01-0d1a11) / pinned build date
  (2026-11-15) rotated; config gains log.attempts (enabled,
  coalesce-ms 2000, world-prefix "em_", worlds []) and attempts in
  writer.shed-order immediately after blocks.
- U6 deterministic double-build + fingerprint + source zip + record:
  DONE. Builds A and B byte-identical (ba10bbcd...), cmp EXACT;
  fingerprint gate PASS on both; source zip above.
- U7 mirror rig: NOT RUN - excluded from this task by instruction
  (Q4 is rig-provable later). Nothing in the unit work refuted the
  MONITOR / ignoreCancelled=false assumption, so no blocker is
  raised; Q4 remains UNPROVEN until the rig leg runs, and the packet
  draft states the rig leg is owed before filing.
- U8 install packet draft: DONE (draft only, not filed).
  ~/workspace/protect-0111-build-20261115/INSTALL-PACKET.md -
  5980 B, lint 5980 B / max line 95 / non-ASCII 0 (targets met);
  companion RECEIPTS.md 2079 B with G1 slots PULL-01..04 blank by
  design for filing-day same-day pulls.

## Exclusion verification (diff vs the 0.1.10 tree of record)

- Changed: exactly the scoping list - NEW BlockAttempts.java,
  BlockAttemptWatch.java; MODIFIED ExcelProtect.java, Settings.java,
  Exporter.java, InspectReport.java, config.yml, plugin.yml,
  local-build.sh, ProtectTest.java (tests only added, none edited or
  removed in substance - the 107 base cases pass unchanged).
- Verified byte-UNCHANGED vs the 0.1.10 tree: Blocks.java,
  Rollback.java, Timelines.java, Ledger.java, Containers.java,
  Entities.java, AuditWatch.java, AuditLog.java, Json.java,
  DurableWriter.java, EliteProbe.java, Watch.java (SHA-256 compared
  file by file). No EliteMobs file touched. Option B not present:
  no block_change is ever written for an attempt, Blocks handlers
  keep ignoreCancelled=true verbatim.

## Owed after this build (not done here, by instruction)

- U7 mirror rig (boot + EM attempt proofs + Q4) before filing.
- Filing to MANIFEST (supervisor/failsafe lane), install, post-install
  player proof (Mike), same-pass surfaces, records publication -
  all named in the packet draft.
