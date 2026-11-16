# RECEIPTS - ExcelProtect 0.1.11 EM Attempts (DRAFT packet companion)
# FILLED COPY - board supervisor run 405, worker W3, 2026-11-15.
# This is a COPY with the G1 slots filled; the original
# ~/workspace/protect-0111-build-20261115/RECEIPTS.md was NOT edited.
# The supervisor applies this to the original after verification.
# Build-side facts below are copied verbatim from the original.

Build-side facts (this build, Nov 15, 2026, board supervisor run 403 worker):

- Base gate: unmodified 0.1.10 sources rebuilt byte-identical -
  295195 B / SHA-256 dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61,
  suite 107/107. Evidence: evidence/base-gate-build.log in the build folder.
- Candidate: ExcelProtect-0.1.11.jar - 303587 B / SHA-256
  ba10bbcd84de50f199f78b1e09d354ee189f8919ba9732d654ed2a4bcc94260f.
- Build ID: EXC-PROTECT-0.1.11-WRV-20261115-01-0d1a11 (verified inside
  the jar: excel-build.txt + plugin.yml version 0.1.11).
- Deterministic double-build: builds A and B byte-identical (same hash
  above). Evidence: evidence/build-0111-a.log, evidence/build-0111-b.log,
  evidence/candidate-a.jar.
- Suite: 117 found / 117 successful / 0 failed on both builds
  (107 base + 10 new attempt cases).
- Source deposit (for the records leg): excelprotect-0.1.11-source.zip
  210763 B / SHA-256 ef13e67fcdf7aa6418aad56b1b301d3a42ef2d316b2acc38dc8d1d137b6db262.
- Full build tree kept at build-tree/ in the build folder.
- Rig: NOT RUN in the build task (U7 is a separate leg, owed before
  filing). Q4 (MONITOR / ignoreCancelled=false observation order vs
  EliteMobs' cancellation) is therefore NOT yet proven or refuted.
  [Run-405 update: superseded - rig PASSED Q4 7/7 in run 404,
  ~/workspace/board-r404/protect-rig2/RIG.md, on the hash-exact
  candidate. The rig gate is satisfied.]

## G1 live-premise receipts (filing day fills - FILLED run 405, 2026-11-15)

Route for all pulls: Homebase (exact invocation
`ssh -F ~/.ssh/config -o BatchMode=yes -o ConnectTimeout=20 mike-pc`),
probe PASS attempt 1 at 2026-11-15 17:14:34 CST (DESKTOP-IL05DQR /
PROBE_OK / shockbyte: remote present). On-PC rclone copyto
(shockbyte:) into PC staging muse-temp/r405-stage/, hashed on PC
(Get-FileHash SHA-256), SCP'd to live-pulls/ in
~/workspace/board-r405/protect-g1/ and re-hashed locally. PC and
local hashes agree on all pulled files. All 3 copyto exits: 0.
Pull window: 2026-11-15 17:15:49-17:16:01 CST. No fallback was used.
PC cleanup: staging folder deleted after the pull, verified gone
(stage exists: False, 17:16:35 CST).

- PULL-01 live jar: plugins/ExcelProtect-0.1.10.jar expected 295195 B /
  dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61
  (per the Nov 15 install record; a filing-day same-day pull MUST
  confirm before this packet files - STOP on any mismatch).
  Receipt: plugins/ExcelProtect-0.1.10.jar - 295195 B / SHA-256
  dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61,
  pulled 2026-11-15 17:15:49-17:16:01 CST, Homebase/rclone; local
  copy live-pulls/ExcelProtect-0.1.10.jar re-hashed EXACT.
  Verdict: EXACT vs premise - no STOP.
- PULL-02 live config: plugins/ExcelProtect/config.yml (the candidate
  config is the 0.1.10 config + the log.attempts section + attempts in
  writer.shed-order only; filing-day diff decides replace vs merge).
  Receipt: plugins/ExcelProtect/config.yml - 14885 B / SHA-256
  a0ba0318169ecef6f7d9ffcd7e77caf2caa0e259577f2e795d1a1877a76886c2,
  pulled 2026-11-15 17:15:49-17:16:01 CST, Homebase/rclone; local
  copy live-pulls/live-config.yml re-hashed EXACT.
  Filing-day diff (live vs candidate build-tree/config.yml 16002 B /
  c17204010170fe09d67bfeea348e89f50da071e3526bc834ee1897af69c98a9b):
  ONLY the two intended changes - (1) writer.shed-order gains
  `attempts` after `blocks`; (2) the log.attempts section is added.
  No other drift. Diff file: live-pulls/config-diff-live-vs-candidate.txt.
  Decision: REPLACE with the candidate config (merge would produce
  the identical file; replace is safe on this diff).
- PULL-03 live plugin census (no second ExcelProtect jar present).
  Receipt: rclone lsf of plugins/ at 2026-11-15 17:16:25 CST,
  Homebase. ExcelProtect entries: folder ExcelProtect/ plus exactly
  ONE loadable jar, ExcelProtect-0.1.10.jar; all other ExcelProtect
  files are inert .held/.bak copies (0.1.0 .held-20261104-pre011,
  0.1.1 .held-20261105-pre012, 0.1.2 .held-20261105-pre013,
  0.1.3 .held-20261105-pre014, 0.1.4 .held-20261106-pre015,
  0.1.5 .held-20261106-pre016, 0.1.6 .bak-20261106-pre017,
  0.1.7 .bak-20261108-prebosspair, 0.1.8 .bak-20261114-pre019,
  0.1.9 .bak-20261115-pre0110). Active-jar count (regex
  ^ExcelProtect-[0-9].*\.jar$) = 1. Census file:
  live-pulls/plugin-census-excelprotect.txt.
  Verdict: EXACT vs premise - no second active jar.
- PULL-04 guild/seal state line at filing.
  Receipt: plugins/ExcelGuildGate/state.yml - 1080 B / SHA-256
  8a3bdc653e6e5f29b1db02a2b0a126ff561965a36d0b694e52bd69bd94866e17,
  pulled 2026-11-15 17:15:49-17:16:01 CST, Homebase/rclone; local
  copy live-pulls/guildgate-state.yml re-hashed EXACT.
  State line: `unlocked: true`; 5 of 5 seals broken
  (ender_dragon, wither, elder_guardian, raid, warden - all
  broken: true; warden killer Jester_Turtle, warden timestamp =
  unlockTimestamp). Guild UNLOCKED 5/5 at filing.
