# RECEIPTS - ExcelSummons 0.1.13 (mana +25%) - SKELETON

## Build evidence (filled at build time, run 394 W7)

- Base gate: ExcelSummons 0.1.12 tree (run 365) copied
  to ../base-gate/ and rebuilt UNMODIFIED: 116,690 B /
  a21c584fd9982ec5ce83b652b19b6c9536af31baecdb92251ab42afcf1d81c83
  EXACT vs the run-365 candidate; 123/123 tests.
  Evidence: ../evidence/base-gate-build.log. The 0.1.12
  tree + packet re-hashed after the build: UNMUTATED.
- Candidate: ExcelSummons-0.1.13.jar 116,869 B /
  97b67906b7aad6b82c548c9ab025618fa5d7c93151331c029eb21cd824ddc1f5;
  build EXC-SUMMONS-0.1.13-WRV-20261115-01-mana25.
- Tests: 128 found / 128 successful / 0 failed (123
  carried, re-pinned to the +25% table incl. the flipped
  D64 pins wolf5=75 -> wolf3=57 and elite trio 70 -> 44;
  5 new coherence cases: single holds + adopted 0,
  4th-wolf PreCast refusal, grandfathered wolves
  re-derivation, grandfathered elite pair re-derivation,
  emitter numbers at the new percents).
- Determinism: script double-build byte-identical AND
  a second full build run reproduced 97b67906...
  exactly. Evidence: ../evidence/build-0113-a.log,
  ../evidence/build-0113-b.log, ../evidence/candidate-a.jar.
- Modifiers: modifiers-replacement-0.1.13.yml verified
  by script - YAML parses, 18/18 keys, key set identical
  to the 0.1.11 replacement file, every boost equals
  the scope section 1.2 cumulative table.
- Compile dep: ExcelProtect 0.1.10 API jar 295,195 B /
  dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61
  (gateway compile only, never bundled - 0 excelprotect
  entries in the candidate). Copy at
  ../deps/ExcelProtect-0.1.10.jar; local-build.sh now
  defaults PROTECT_API at that copy.
- Rig: NOT RUN by this build worker (scope section 4.3
  rig is a supervisor/filing-day leg in this dispatch;
  the unit suite + jar-content checks above are the
  build evidence). Owed before filing: scratch rig boot
  with Protect 0.1.10 proving "Enabling ExcelSummons
  v0.1.13", the door-bound line, SEVERE 0, placed config
  + guardian modifier (-0.44) spot checks.
- NOT provable in the VM (no client): in-game cast /
  refusal behaviour - the packet's post-install proof
  owed, stated there.

## Live-premise receipts (G1) - BLANK, filled at filing

- G1-1 live jar (must be 0.1.12): path ___ size ___
  sha256 ___ pulled ___ route ___
- G1-2 live plugins/ExcelSummons/config.yml
  (premise: still the D64 rows 20/25/35/30/35/15/10/
  10/15): size ___ sha256 ___ pulled ___
- G1-3 live Magic modifiers.yml reserve blocks
  (premise: still the D64 boosts, guardian -0.35):
  size ___ sha256 ___ pulled ___
- G1-4 guild state: ___ pulled ___

## Publication claim

- Not filed by this worker. Supervisor files the
  pointer stub after the chain settles (0.1.12
  INSTALLED first); G2 duplicate check runs at
  publication time.
