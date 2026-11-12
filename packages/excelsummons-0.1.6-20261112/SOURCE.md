# ExcelSummons 0.1.6 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 12, 2026.
Defect of record: the unnumbered item at the end of
`~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`
("Summons reserve over-ceiling on re-derivation (Jester report,
Nov 12, 2026)") — the re-derivation applied the raw 85% sum for
an all-adopted menagerie, over the 75% ceiling and under the 20%
floor that PreCast alone enforced. Binding spec: Mike's ruling,
Nov 12, 2026, ~3:08 PM CST (adopted pets hold no mana).

## Provenance

- Project tree: `~/workspace/summons-016-build/ExcelSummons/`
  (plugin.yml version 0.1.6, main
  com.winkreyes.excelsummons.ExcelSummons, api-version '1.21').
- Base: the shipped 0.1.5 tree `~/workspace/summons-015-build/`
  — its jar (65,253 B, SHA-256 5cdf16a0…f61e, build
  EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315) re-verified on disk
  Nov 12, 2026, and a clean-room copy of the untouched 0.1.5
  tree rebuilt that jar BYTE-IDENTICAL (74/74 tests) BEFORE the
  0.1.6 delta was applied to a copy. The 0.1.5 folder was not
  modified.
- Built by `local-build.sh` (the house deterministic recipe:
  JUnit first, timestamp-pinned jar built twice and compared,
  fingerprint gate), against the same compile surface as 0.1.5
  (house lib set + Paper API 26.3 + Magic 11.2.5 API).
- Ownership: all code is ours (Iris / Mike lineage, Excel
  signature). Magic is a hard depend (integration point only,
  never shaded); no new dependency of any kind.

## What 0.1.6 changes vs 0.1.5 (the whole delta)

Design summary: DESIGN-0.1.6.md in this tree and in the source
zip. In short: (1) adopted-origin records are excluded from
every reserve derivation in `ReservePolicy` (counts, raw total,
desired modifiers) through one CAST-only funnel — adopted pets
hold no mana, permanently (Mike's ruling); (2) the sync
re-derivation (`desiredModifiers` ceiling/floor overload +
`SummonsListener.syncReserve` passing the live config values)
clamps the applied total to `min(ceiling, 100 − floor)`,
applying holds per record in registry order and skipping any
record that would pass the cap (it stays, holding nothing);
(3) the PreCast gate and the list lines now read the APPLIED
state (`appliedPercent` / `recordHolds`), so every surface
quotes the same number the mage actually carries. Nothing else
moves: cast rates, caps, dismiss, messaging, and config
(byte-identical) are 0.1.5's.

Change-scope diff vs the verified 0.1.5 tree — only:
ReservePolicy.java, SummonsListener.java, ExcelSummons.java,
BuildInfo.java, SummonsTest.java, plugin.yml, local-build.sh,
DESIGN-0.1.6.md (new). Every other file is byte-for-byte the
0.1.5 one (diff-proven).

## Artifacts

- ExcelSummons-0.1.6.jar — 66,209 bytes
  SHA-256 08da4f10a5154b28187f2b000535abb0154498b751efa19c0a3518e4e04f70f1
  MD5 937fb7c6498bdfaf26e0a7e523bbaaad
  Build EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a (fingerprint
  gate PASS). Clean-room rebuild and source-zip rebuild each
  reproduced this hash exactly (78/78 tests in all three).
- ExcelSummons-0.1.6-source.zip — 89,260 bytes
  SHA-256 f717a0b5422a53f8ef4c6982f03c4e51dfb6d878fda4a17af4048cdd239a2b3b
  MD5 906f29a5baf515e6b0b45c0dcff462ac
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.6.md + DESIGN-0.1.5.md + DESIGN-0.1.4.md +
  DESIGN-0.1.3.md; timestamps pinned 2026-11-12; zip creation
  verified deterministic across two creations.)
- Packet companions: none — no Magic config additions, no
  bridge config change (config.yml byte-identical to 0.1.5).

## Verification summary

SummonsTest 78/78 (74 carried — 9 reserve fixtures re-based to
CAST origin + 2 version re-pins, named in the build record —
+ 4 new 0.1.6 tests: the adopted-menagerie fixture at applied
0%, the over-ceiling cast clamp at exactly 75% with usable ≥
floor, mixed-origin separation, the floor cap binding below a
higher ceiling). Desk rig on Paper 26.3-235 + Magic 11.2.5
(rig-0.1.6 in the build folder): clean 0.1.6 enable, SEVERE 0,
reserve-template warnings 0, archer-equipment soft-fail lines
0, 21/21 behavior checks (the 19 carried 0.1.5 checks with
phases A–D reserve expectations corrected to the ruling + phase
G's two fixture checks against real Magic modifier state).
NOT installed — built + staged only.
