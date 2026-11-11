# ExcelSummons 0.1.4 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 10, 2026.
Design of record: `~/workspace/wolf-summon-diagnosis-20261110/
FIX-PACKET-DESIGN.md` §§2–4, with two supervisor-directed
amendments (Mike, Nov 10, 2026: refusals-explain-themselves
10:34 AM; rank-scaled cap 10:35 AM, ladder locked 10:37 AM).

## Provenance

- Project tree: `~/workspace/summons-014-build/ExcelSummons/`
  (plugin.yml version 0.1.4, main
  com.winkreyes.excelsummons.ExcelSummons, api-version '1.21').
- Base: the shipped 0.1.3 tree `~/workspace/summons-013-build/`
  — its jar (54,707 B, SHA-256 d6dc49d0…c852, build
  EXC-SUMMONS-0.1.3-WRV-20261110-04-c9f312) re-verified on disk
  Nov 10, 2026, and a clean-room copy of the untouched 0.1.3
  tree rebuilt that jar BYTE-IDENTICAL (53/53 tests) BEFORE the
  0.1.4 delta was applied to a copy. The 0.1.3 folder was not
  modified.
- Built by `local-build.sh` (the house deterministic recipe:
  JUnit first, timestamp-pinned jar built twice and compared,
  fingerprint gate), against the same compile surface as 0.1.3
  (house lib set + Paper API 26.3 + Magic 11.2.5 API).
- Ownership: all code is ours (Iris / Mike lineage, Excel
  signature). Magic is a hard depend (integration point only,
  never shaded); ExcelTogether access stays reflection-only via
  the 0.1.0 TogetherPartyProvider pattern, untouched.
  ExcelMagicGate's earned.yml is READ only (never written) —
  MagicGate stays the file's only writer.

## What 0.1.4 changes vs 0.1.3 (the whole delta)

Design summary: DESIGN-0.1.4.md in this tree and in the source
zip. In short: companion + mount mana reserve (the §2.1
seven-site widening; combatCounts renamed reserveCounts);
SummonRecord origin (CAST/ADOPTED, legacy = ADOPTED) + dismiss
pets scope in AdminPolicy, the listener, and both command
surfaces; the unified counted total made rank-scaled
(RankCap mirrors MagicGate's locked thresholds; CapPolicy
takes the config cap map; TOTAL_CAP replaces COMBAT_CAP);
§4 refusal messages with live numbers (rank, limit, count,
current summons; reserve ceiling/floor with percents);
SummonRegistry.countCounted; config.yml (summon-cap-by-rank
replaces per-caster-combat-cap; +4 reserve rows); plugin.yml
(version, dismiss-pets usage). §3.4 reconciliation deliberately
NOT wired (build record gives the reasoning).

Change-scope diff vs the verified 0.1.3 tree — only:
AdminPolicy.java, BuildInfo.java, CapPolicy.java,
ExcelSummons.java, RankCap.java (new), ReservePolicy.java,
SummonRecord.java, SummonRegistry.java, SummonsListener.java,
SummonsTest.java, config.yml, plugin.yml, local-build.sh,
DESIGN-0.1.4.md (new). Every other file is byte-for-byte the
0.1.3 one (diff-proven).

## Artifacts

- ExcelSummons-0.1.4.jar — 63,642 bytes
  SHA-256 a34d7bdf54c9017eb3e4290d2fa89c3685e0adb3c9f40eef53f959f6322cc25a
  MD5 5865685388942018f9d4de01e1731d1d
  Build EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19 (fingerprint
  gate PASS). Clean-room rebuild and source-zip rebuild each
  reproduced this hash exactly (69/69 tests in all three).
- ExcelSummons-0.1.4-source.zip — 76,420 bytes
  SHA-256 06e40034205ae8f8851bd09befe5353910b348009a3261a384315ccf1f40f391
  MD5 1b41db25757546fd635dea394a23caba
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.4.md + DESIGN-0.1.3.md; timestamps pinned
  2026-11-10.)
- Packet companions (staged config, installed with the jar):
  modifiers-addition-0.1.4.yml (+11 reserve templates),
  messages-addition-0.1.4.yml (per-spell refusal overrides for
  the 9 summon families + 3 tier keys).

## Verification summary

SummonsTest 69/69 (53 carried — 5 re-pinned/replaced, named in
the build record — + 16 new). Desk rig on Paper 26.3-235 +
Magic 11.2.5 (rig-0.1.4 in the build folder): clean 0.1.4
enable, SEVERE 0, reserve-template warnings 0, 14/14 behavior
checks (rank seam against a real-shape earned.yml, companion
reserve apply/release on cast-capture-equivalent registry
state, dismiss pets vs adopted survival, death release,
grandfathered over-cap set with exact wolf5 holds, mount
exemption, TOTAL_CAP message shape, §3.4 unwired pin).
NOT installed — built + staged only.
