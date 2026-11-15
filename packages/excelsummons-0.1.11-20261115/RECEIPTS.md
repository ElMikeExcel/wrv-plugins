# RECEIPTS - ExcelSummons 0.1.11 (P-SUMMONS-TUNING-1) - SKELETON

## Build evidence (filled at build time, run 364)

- Base gate: BB-11 0.1.10 tree copied to ../base-gate/ and
  rebuilt UNMODIFIED: 102,597 B / 97e442165b755fb7fac325
  e712f634c2607490bf7f0de0dd83b28510036ff7d0 EXACT vs the
  filed BB-11 candidate; 109/109 tests. BB-11's own base
  gate on live 0.1.9 seq-02 (ead3beec...) is in its packet.
- Candidate: ExcelSummons-0.1.11.jar 107,064 B /
  3d9b22a0ed3878912832760bd9df9e1f9979be7339fd7b49f805610643f7661a;
  build EXC-SUMMONS-0.1.11-WRV-20261114-01-tune11.
- Tests: 116 found / 116 successful / 0 failed (109 carried
  re-pinned to D64 + 7 new 0.1.11 tests).
- Determinism: script double-build byte-identical AND a
  second full build run reproduced 3d9b22a0... exactly.
- Rig: local Paper 26.2-123 + Magic 11.2.5 scratch rig,
  candidate hash-verified in place, candidate config
  staged: "Enabling ExcelSummons v0.1.11", enable line
  with the 0.1.11 build id, Done, SEVERE 0, clean disable.
  Log: ../rig/boot-0111.log.
- Modifiers: packet modifiers-replacement-0.1.11.yml -
  18 templates, YAML-parsed, boosts = -(D64 % x count).

## Live-premise receipts (G1) - BLANK, filled at filing

- G1-1 live jar (must be BB-11 0.1.10): path ___ size ___
  sha256 ___ pulled ___ route ___
- G1-2 live plugins/ExcelSummons/config.yml: size ___
  sha256 ___ pulled ___
- G1-3 live Magic modifiers.yml reserve blocks (18 keys,
  old boosts): sha256 ___ pulled ___
- G1-4 guild state: ___ pulled ___
- Candidate config hash at filing: size ___ sha256 ___

## Publication claim

- Not filed by this worker. Supervisor files the pointer
  stub; G2 duplicate check runs at publication time.
