# RECEIPTS - BB-11 ExcelSummons 0.1.10 (run 356)

All timestamps America/Chicago (CST), Sat Nov 14, 2026.

## G1 live-premise receipt (Homebase route, no fallback)

- Probes: ssh -F ~/.ssh/config mike-pc hostname -
  attempt 1 at 18:22:17 CST, attempt 2 at 18:23:01,
  attempt 3 at 18:23:48 - all returned DESKTOP-IL05DQR,
  exit 0. PASS 3/3 over ~91 s. Last successful probe:
  18:23:48 CST this run (a 4th PASS at 18:25:53 preceded
  the corrected pull script).
- Pull: rclone on the PC from remote
  shockbyte:1. Michael's Server, copyto of
  plugins/ExcelSummons-0.1.9.jar into PC staging
  muse-temp\g1-r356-bb11 (PC time 18:26:15-18:26:30 CST,
  COPY exit=0), then scp down to BB-11/pull-g1/.
  PC-side Get-FileHash == local SHA-256.
  First pull script (pull-bb11.ps1) mis-listed the
  plugins dir (no --files-only) and copied nothing;
  pull-bb11b.ps1 is the corrected script of record.
- Live path: plugins/ExcelSummons-0.1.9.jar
- Pulled copy: BB-11/pull-g1/plugins__ExcelSummons-0.1.9.jar
- Size: 102,260 B
- SHA-256: ead3beec553da8e37d255971c8e8e5f96a707835c0a8811a90df115759e6df46
- Pull timestamp: Nov 14, 2026, 18:26 CST
- Verdict: EXACT match to the 0.1.9 seq-02 candidate of
  record (summons-cap-build-20261114 BUILD-RECORD.md:
  102,260 B / ead3beec..., build
  EXC-SUMMONS-0.1.9-WRV-20261114-02-c9a114). Live IS
  0.1.9 seq-02 - the audited version was 0.1.8, so this
  unit re-based onto the seq-02 tree (see re-verdict).
- Census: pull-g1/plugins-top.txt (15,459 B /
  816d94a4193c5e77306b9b98c53e8c15553abf5f1ca5f9e2b74
  ab35ba06701c8) - exactly ONE loadable ExcelSummons
  jar live (0.1.9); older jars are .bak / .held only
  (0.1.2/0.1.4/0.1.5/0.1.6/0.1.7 .bak, 0.1.8 .held).
- Guild: pull-g1/plugins__ExcelGuildGate__state.yml =
  1,015 B / 79e4470255edc5522db555e6ee935c301a5b74776
  fed45f3aa1f45220879b4c0 - unlocked false, 4 of 5
  seals broken (warden false): guild LOCKED 4 of 5.
- PC cleanup: staging dir g1-r356-bb11 and both pull
  scripts deleted from the PC after the pull; verified
  Test-Path False / False / False.

## Row re-verdict against the live tree (re-base check)

- Audit rows (0.1.8): main summons page Java No
  (SummonsMenus.java:169-198, 264-276) + Bedrock No
  (SummonsMenus.java:324-340) - AUDIT.md ExcelSummons
  section; parts/B-summons-skillspanel.md.
- diff of SummonsMenus.java and MenuModel.java between
  the 0.1.9 seq-01 tree and the live seq-02 tree:
  IDENTICAL (no diff output). The 0.1.9 deltas (pet
  names, cap fix) did not touch the main page's control
  set: fillMain still sets only Prev/Next, Adopted,
  Info and the three Dismiss items; sendMainForm
  buttons are still dismiss x3 + adopted only.
- Verdict: 2/2 rows STILL BROKEN in live 0.1.9 seq-02,
  0 fixed, 0 gone. Scope unchanged on re-base.

## Base gate (byte-identical rebuild, before any edit)

- Unmodified copy of the live seq-02 tree rebuilt in
  BB-11/base-gate/ExcelSummons with its own
  local-build.sh: jar 102,260 B / ead3beec...f46 -
  BYTE-IDENTICAL to the G1 live pull and to the
  candidate of record; suite 107/107; internal
  double-build byte-identical; fingerprint gate PASS
  (EXC-SUMMONS-0.1.9-WRV-20261114-02-c9a114). PASS,
  no exceptions.

## Candidate verification (desk, this worker)

- Candidate jar: BB-11/packet/ExcelSummons-0.1.10.jar
  = 102,597 B / 97e442165b755fb7fac325e712f634c26074
  90bf7f0de0dd83b28510036ff7d0. plugin.yml inside
  reads version 0.1.10; excel-build.txt reads
  EXC-SUMMONS-0.1.10-WRV-20261114-01-bb1110.
- Determinism: local-build.sh internal double-build
  byte-identical AND a second full build run
  reproduced 97e44216... exactly (cmp/sha256sum).
- Tests: 109 found / 109 successful / 0 failed
  (107 carried from seq-02 + 2 new BackNav tests:
  mainFormButtonsEndWithBackToServerMenu,
  mainBackSourceAnchorsPinBothPlatforms).
- Scope diff vs the live seq-02 tree (diff -rq):
  src differs ONLY in BuildInfo.java (version),
  MenuModel.java (mainFormButtons / MAIN_BACK_*),
  SummonsMenus.java (Java slot 52 item + click,
  Bedrock Back index, openServerMenu); tests differ
  only in SummonsTest.java (version re-pin + 2 new
  tests); plugin.yml + local-build.sh version knobs;
  new DESIGN-0.1.10.md. config.yml byte-identical.
  Adopted page + dismiss confirm sources unchanged -
  their only test-visible anchors are asserted
  verbatim in the new source-anchor test.
- Source zip: BB-11/packet/ExcelSummons-0.1.10-sources.zip
  = 141,825 B / d15a3594b08cb2fe7bd842cd1e03ce8bfbb602
  96ce80eae6a2859ccf9de9fe25 (47 files).

## Rig (local, BB-11/rig/)

Scratch copy of a Paper 26.2-123 server in /tmp
(deleted after the boot; log retained), plugins =
Magic 11.2.5 (the plugin's hard depend) + candidate
jar hash-verified in place (97e44216...) + the LIVE
0.1.9 config staged:

- boot-0110.log = 29,297 B / 0aab9093796a07ced363a6e
  ad61fa5ba3eb7b1374734b4f7469e3ea5d5d835d9
- "Enabling ExcelSummons v0.1.10"; enable line
  "ExcelSummons 0.1.10 enabled (build
  EXC-SUMMONS-0.1.10-WRV-20261114-01-bb1110); registry
  holds 0 summon(s)."
- Done (37.656s); SEVERE count 0 (grep -c); clean
  disable on shutdown ("ExcelSummons disabled;
  registry saved"). The only ERROR lines are Paper
  version-fetch / yggdrasil sandbox-network noise
  (the BB-07 smoke class) plus the harness's own
  piped /stop timing artifact; none attributable to
  ExcelSummons.
- Mirror walk NOT run by this desk worker: a tap-walk
  needs real Java + Bedrock clients; it is the
  packet's post-install proof owed (main page x
  platform), the wave-1/2 residual class.
