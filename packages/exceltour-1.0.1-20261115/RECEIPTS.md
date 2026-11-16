# RECEIPTS - BB-09 ExcelTour 1.0.1

## G1 live-premise receipt - NOT OBTAINED (gate OPEN)

Lane attempted: Homebase (ssh -F ~/.ssh/config, host mike-pc /
DESKTOP-IL05DQR, per the task brief). Probes, all
`ssh mike-pc hostname`, all exit 255 with
"kex_exchange_identification: read: Connection reset by peer /
Connection reset by 198.19.0.1 port 3128":

- Probe 1: Sat Nov 14, 2026, 17:47:40 CST
- Probe 2: Sat Nov 14, 2026, 17:48:33 CST
- Probe 3: Sat Nov 14, 2026, 17:49:26 CST
  (the required 3-probe / ~2-minute retry pattern, completed)
- Probe 4: Sat Nov 14, 2026, 17:52:48 CST - same failure
- Probe 5: Sat Nov 14, 2026, 18:00:47 CST - same failure

No SSH session was ever established; no bytes were transferred;
no PC-side files were created (nothing to clean up). Per the
Nov 11 alert-clarity rule: these failures are evidence about the
relay path only; the PC's own state is unknowable while they
fail. The last successful Homebase probe is NOT known to this
worker (the wave-1 G1 dispatch the same evening also failed all
33 of its probes, 17:23-17:33 CST).

- Live path (expected): plugins/ExcelTour-1.0.0.jar
- Live size: NOT OBTAINED. Live SHA-256: NOT OBTAINED.
- FALLBACK (panel) receipt: NOT TAKEN - this desk worker has no
  panel/browser route. The supervisor must obtain the byte-exact
  G1 receipt before filing, either by a Homebase retry when the
  relay recovers or by a labelled FALLBACK (panel) receipt
  (panel File Manager download + local SHA-256).
- Consequence: whether live still runs 1.0.0 CANNOT be confirmed
  from this run. The build bases on the audited 1.0.0 tree, whose
  byte-identical rebuild matches the jar of record (below). If
  the G1 pull later shows a different live jar, this unit must
  be re-based and its 3 rows re-checked before filing.

## Base-gate rebuild receipt (disk lineage, NOT a live premise)

- Tree: exceltour-build-20261111/ExcelTour, copied unmodified to
  BB-09/build-1.0.1/ExcelTour; `diff -r` of src and tests vs the
  audited tree: identical.
- Rebuild (original build.sh, unmodified copy): produced
  ExcelTour-1.0.0.jar - 50,656 B / SHA-256
  4af7c455d98ead43603f75fe007444983c75345d70f0109c9149dc785c78dd72
  EXACT match to the audited 1.0.0 jar of record
  (exceltour-build-20261111/ExcelTour-1.0.0.jar, same size/hash).
  Suite on the unmodified copy: TourTest 42/0. Double-build
  byte-identical; fingerprint gate passed.

## Candidate receipt

- ExcelTour-1.0.1.jar - 50,908 B / SHA-256
  ab94d4a12b33b8acb45b60599ef3b27eac4f0b86805456930719ba90e2c0e317
  at BB-09/build-1.0.1/ExcelTour-1.0.1.jar. Two full build.sh
  runs, identical hash; build ID
  EXC-TOUR-1.0.1-WRV-20261114-01-bb09.
- ExcelTour-1.0.1-source.zip - 39,081 B / SHA-256
  b695249069e1ed29804bf7a48ee11c9e24afe06fa10133e91af32cb6b8a4c3e6

## Rig receipts

- Fresh-state rig boot log: BB-09/rig/boot-101.log - 31,719 B /
  SHA-256 03f3a6c8ce5d35fcb5f3c3bd96e584a88377b3cfdc8e3803a83582993e1c95ca
  (Done (39.974s), SEVERE 0, RIGOBS 4 PASS / 1 FAIL - see
  TEST-REPORT.md and BB-09-RESULT.md for the control verdict).
- Stale-state first boot log: BB-09/rig/boot-101-stale-state.log
  (same 4/1 tally; kept for the record).
- Control boot (1.0.0 jar, identical fresh rig): BB-09/rig/
  boot-100-control.log - 96,100 B / SHA-256
  f8b7547e993e5f0e09357856ceb888334884c3f5f31fc37813e42f7b5c9fe881
  (Done (63.556s), SEVERE 0, RIGOBS 4 PASS / 1 FAIL - the same
  arrival step as the 1.0.1 rig: the failure pre-exists the
  delta and is a rig/environment property, not a regression).
- Probe 6: Sat Nov 14, 2026, 18:07:49 CST - same failure
  (final retry before this unit closed; 6 probes total,
  17:47-18:07 CST, all exit 255, relay reset).

## Addendum - G1 receipt OBTAINED (retry worker, 18:10-18:14 CST)

Supersedes the NOT OBTAINED head of this file. Probes
18:10:44 / 18:11:26 / 18:12:08 CST: PASS 3/3, DESKTOP-IL05DQR,
exit 0. Pull (rclone copyto via Homebase + scp, 18:14 CST):
live plugins/ExcelTour-1.0.0.jar = pull-g1/ExcelTour-1.0.0.jar
= 50,656 B / SHA-256
4af7c455d98ead43603f75fe007444983c75345d70f0109c9149dc785c78dd72
- EXACT match to the jar of record above. Census: exactly one
active Tour jar live. Full evidence: ../BB-09-G1-RESULT.md
and packet/RECEIPTS.md. PC staging cleaned (Test-Path False).
