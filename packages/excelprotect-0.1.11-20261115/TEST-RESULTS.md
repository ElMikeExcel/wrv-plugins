# TEST RESULTS - ExcelProtect 0.1.11 (P-PROTECT-EM-1 Option A)

Source of record: BUILD-RECORD.md in this package (desk build, board run 403) + mirror rig leg (board run 404, worker W5b, RIG.md). Live server was never touched by testing; all rig work ran on the Homebase test mirror.

## Base gate
- 0.1.10 sources rebuilt byte-identical to the live-premise jar: ExcelProtect-0.1.10.jar, 295195 B, SHA-256 dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61.
- Base suite on 0.1.10 sources: 107 found / 107 successful / 0 failed.

## Candidate suite
- ExcelProtect-0.1.11.jar: 303587 B, SHA-256 ba10bbcd84de50f199f78b1e09d354ee189f8919ba9732d654ed2a4bcc94260f (fingerprint EXC-PROTECT-0.1.11-WRV-20261115-01-0d1a11).
- Two independent builds (A and B) produced the identical hash - deterministic.
- Suite on 0.1.11: 117 found / 117 successful / 0 failed (both builds; 107 base + 10 new attempt cases: attemptCoalescerMergesWithinWindow, attemptCoalescerSplitsOnGapExpiry, attemptCoalescerSplitsOnKeyChange, attemptCoalescerClosesOnQuitAndShutdown, attemptRecordShape, attemptNeverRollbackSelectable, attemptProjectsToPlayerTimeline, attemptExporterFilters, attemptInspectSummaryShape, attemptSettingsDefaultsAndClamps).

## Mirror rig (Q4) - PROVEN 7/7
- Two boots on the Homebase mirror (Boot A attempts ON ran Q4-1..Q4-6; Boot B attempts OFF ran Q4-7). Each boot: Enabling ExcelProtect v0.1.11 x1, READY version=0.1.11 x1, SEVERE 0, clean disable / SERVER-EXIT 0. Placed jar hash EXACT vs candidate; candidate config 16002 B / c17204010170fe09d67bfeea348e89f50da071e3526bc834ee1897af69c98a9b EXACT desk-to-mirror.
- Q4-1 cancelled BREAK in EM scope: exactly one coalesced block_attempt on the attempts stream, zero block_change. Q4-2 cancelled PLACE in EM scope: one block_attempt carrying the attempted placed type, no block_change. Q4-3 uncancelled control: zero attempts, exactly one block_change each. Q4-4 cancelled break/place outside EM scope: zero records on either stream. Q4-5 non-interference: Protect never un-cancelled or re-cancelled; blocks byte-identical. Q4-6 coalesce 2000 ms: burst x3 at one position = ONE record count=3; repeat after the gap = second record count=1 (attempts file: 4 records total). Q4-7 toggle OFF: attempts file byte-identical across Boot B (1484 B before and after), zero new records.
- Evidence: rig-evidence/boot-A.log, rig-evidence/boot-B.log, rig-evidence/attempts-2026-11-15.jsonl (4 records), rig-evidence/blocks-2026-11-15.jsonl (2 records).
- Mirror restored byte-exact: Protect jar back to 0.1.8 (268894 B / 73373b90...), config back to 7991 B / 7a423378...; rig artifacts, rig world and rig stream files removed.

Q4 verdict: PROVEN (7/7 PASS).
