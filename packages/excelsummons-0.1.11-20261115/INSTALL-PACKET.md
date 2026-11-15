# INSTALL PACKET - ExcelSummons 0.1.11 (P-SUMMONS-TUNING-1)

Unit: ONE gated packet - jar + config + Magic modifiers
together (design draft section 2.5: the holds live in two
files; landing only one is a silent desync). NOT a text
live-edit. DRAFT ONLY - supervisor files to MANIFEST after
G1 receipts are filled at filing time.

## Order vs BB-11 (explicit)

Installs AFTER ExcelSummons 0.1.10 (BB-11 back-button fixes,
MANIFEST PENDING, candidate 97e44216...). This build is based
ON the BB-11 0.1.10 tree (copy; BB-11 tree/packet untouched),
so it carries the back-button fixes forward. Its live-jar
gate is the 0.1.10 jar. If BB-11 has not landed, STOP - do
not install 0.1.11 over live 0.1.9.

## Candidate artifacts (this folder)

- ExcelSummons-0.1.11.jar - 107,064 B / SHA-256
  3d9b22a0ed3878912832760bd9df9e1f9979be7339fd7b49f805610643f7661a
  build EXC-SUMMONS-0.1.11-WRV-20261114-01-tune11;
  tests 116/116; deterministic double-build + second full
  run byte-identical; local rig boot PASS SEVERE 0.
- config.yml (candidate bridge config, D64 reserve rows) -
  placed over plugins/ExcelSummons/config.yml at the window.
- modifiers-replacement-0.1.11.yml - REPLACE all 18
  wrv_summon_reserve_* template blocks in Magic's
  modifiers.yml with these (same keys, new boosts).
- ExcelSummons-0.1.11-sources.zip - 146,198 B / SHA-256
  c2233881141f4722b33a00260659b07a16ffb6e336563c9e971f121bc988fadb
  (40 files, source tree of record in ../ExcelSummons/).

## Scope

- Raised holds (D64): archer 20 / bruiser 25 / guardian 35 /
  revenant 30 / ghast 35 / wolf 15 / familiar 10 / cat 10 /
  mount 15; ceiling 75, floor 20, adopted 0 unchanged.
- Nameplate: "Archer (MikeExcel) - 20/20" owner segment on
  both platforms (server-side custom name, as today); final
  15s countdown segment on timed summons.
- Despawn: reason enum EXPIRED / DISMISSED / DIED / UNLOADED;
  T-30 (durations >= 150s) + T-15 fade warnings; ONE removal
  funnel sends the owner line; scope dismissals aggregate to
  one summary line. Mount toggle sends the standard
  DISMISSED line (old flavor line retired, draft veto 3).
- NOT changed: caps, durations, cast costs, dismiss scopes,
  rename, registry data format, BB-11 back-button behaviour.

## Gates

- Marker discipline with re-checks before Shut Down and
  before Start; BACKUP FIRST + cap-at-6 (name prunees; never
  the Daily newest or the two newest manuals); emptiness
  list = 0/10 twice. Fully gated (jar + config + modifiers).
- Live-jar gate (STOP on mismatch): plugins/
  ExcelSummons-0.1.10.jar = 102,597 B / 97e442165b755fb7
  fac325e712f634c2607490bf7f0de0dd83b28510036ff7d0
  (the BB-11 candidate - proves BB-11 landed first).
- Live-premise receipts (G1): BLANK in RECEIPTS.md by
  design - filled same-day at filing (live 0.1.10 jar,
  live ExcelSummons config, live Magic modifiers.yml
  reserve blocks, guild state).

## Install steps (failsafe)

1. Hold aside the live 0.1.10 jar (rollback) and place
   plugins/ExcelSummons-0.1.11.jar.
2. Place candidate config.yml over plugins/ExcelSummons/
   config.yml (in-place .bak of the live file first).
3. Replace the 18 wrv_summon_reserve_* blocks in Magic's
   modifiers.yml from modifiers-replacement-0.1.11.yml
   (in-place .bak first); reload/restart per window plan.
4. Start; run Boot verify; write marker only after pass.

## Boot verify (script-named lines)

- verify_hash.py --file <placed pull-back jar> --size
  107064 --sha256 3d9b22a0...61a (full hash above).
- verify_hash.py --file <placed pull-back config> against
  the candidate config hash in RECEIPTS.md.
- counts-check on the boot log --pattern
  "EXC-SUMMONS-0.1.11-WRV-20261114-01-tune11" --expected 1;
  "Enabling ExcelSummons v0.1.11" --expected 1; SEVERE 0
  attributable to ExcelSummons; guild state unchanged.
- modifiers check: grep the live modifiers.yml pull-back
  for "wrv_summon_reserve_guardian" block boost -0.35
  (one named spot check per family tier in RECEIPTS.md).

## Same-pass surfaces (owed at install)

Guide knowledge + What's New BOTH surfaces + website pair:
new hold table, owner nameplates, fade warnings + despawn
lines. Tips check + menus check owed in the surfaces pass.
Old-percent text anywhere (Guide/website) must be swept.

## Proof owed after install

- Two players field the same family at once: plates
  distinguishable, Java + Bedrock screenshots (draft 3.3).
- Tier-1 Archer stopwatch: T-15 chat warning, plate
  countdown, EXPIRED line with 20% released (draft 4.4).
- One trigger per other reason: menu dismiss (Bedrock) +
  command dismiss (Java), a summon slain, an expiry inside
  an unloaded chunk (UNLOADED line).
- Mike's glance: two Archers out now hold 40% (was 20%).

Marker on install: ../INSTALLED.txt (supervisor names the
path at filing; BB-11 pattern = packet folder marker).
