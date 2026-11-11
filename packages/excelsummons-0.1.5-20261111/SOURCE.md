# ExcelSummons 0.1.5 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 11, 2026.
Diagnosis of record: `~/workspace/goals/family-minecraft-server-build/hidden_files/archer-bow-diagnosis-20261110.md`
(layer-1 verdict: the spells config is correct at every tier;
Magic's SpawnEntity layer does not apply item_in_hand on spawn;
layer 2, this packet: the bridge-side equipment guarantee).

## Provenance

- Project tree: `~/workspace/summons-015-build/ExcelSummons/`
  (plugin.yml version 0.1.5, main
  com.winkreyes.excelsummons.ExcelSummons, api-version '1.21').
- Base: the installed 0.1.4 tree `~/workspace/summons-014-build/`
  — its jar (63,642 B, SHA-256 a34d7bdf…25a, build
  EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19) re-verified on disk
  Nov 11, 2026, and a clean-room copy of the untouched 0.1.4
  tree rebuilt that jar BYTE-IDENTICAL (69/69 tests) BEFORE the
  0.1.5 delta was applied to a copy. The 0.1.4 folder was not
  modified. (Explicitly NOT built on the 0.1.3 tree.)
- Built by `local-build.sh` (the house deterministic recipe:
  JUnit first, timestamp-pinned jar built twice and compared,
  fingerprint gate), against the same compile surface as 0.1.4
  (house lib set + Paper API 26.3 + Magic 11.2.5 API).
- Ownership: all code is ours (Iris / Mike lineage, Excel
  signature). Magic is a hard depend (integration point only,
  never shaded); no new dependency of any kind.

## What 0.1.5 changes vs 0.1.4 (the whole delta)

Design summary: DESIGN-0.1.5.md in this tree and in the source
zip. In short: one new pure policy class, `ArcherEquipment`
(the archer gear spec: bow in hand at every tier, iron helmet
base / chainmail at tiers 2+, read-back verification), and one
registration-path hook in `SummonsListener.onCast` that applies
it — `applyArcherEquipment`, in applyHarness's soft-fail shape —
when a CAST archer is registered, using the cast-time spell key
for the exact tier. Nothing else moves: caps, reserve, dismiss,
messaging, and config (byte-identical) are 0.1.4's.

Change-scope diff vs the verified 0.1.4 tree — only:
ArcherEquipment.java (new), SummonsListener.java, BuildInfo.java,
SummonsTest.java, plugin.yml, local-build.sh, DESIGN-0.1.5.md
(new). Every other file is byte-for-byte the 0.1.4 one
(diff-proven).

## Artifacts

- ExcelSummons-0.1.5.jar — 65,253 bytes
  SHA-256 5cdf16a0b4178adc7e264320996f6d7f350cb14343dfdc59ae965a0bd619f61e
  MD5 f19b51d7b75966e799897bc4ebabb4b0
  Build EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315 (fingerprint
  gate PASS). Clean-room rebuild and source-zip rebuild each
  reproduced this hash exactly (74/74 tests in all three).
- ExcelSummons-0.1.5-source.zip — 82,417 bytes
  SHA-256 9662de9e9d680528ef0a54edf392fcf27c58a2c947aa5f127fd6dca44c1c7ca4
  MD5 9655a1fa25f251e775eaa49e79d3f280
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.5.md + DESIGN-0.1.4.md + DESIGN-0.1.3.md;
  timestamps pinned 2026-11-11; zip creation verified
  deterministic across two creations.)
- Packet companions: none — no Magic config additions, no
  bridge config change (config.yml byte-identical to 0.1.4).

## Verification summary

SummonsTest 74/74 (69 carried — 2 version re-pins named in the
build record — + 5 new ArcherEquipment spec tests). Desk rig on
Paper 26.3-235 + Magic 11.2.5 (rig-0.1.5 in the build folder):
clean 0.1.5 enable, SEVERE 0, reserve-template warnings 0,
archer-equipment soft-fail lines 0, 19/19 behavior checks (the
14 carried 0.1.4 checks + phase F: base/tier-2/tier-3 equipment
on real spawned skeletons through the bridge's own
registration-path method, the archer-only guard, the non-living
soft-fail). NOT installed — built + staged only.
