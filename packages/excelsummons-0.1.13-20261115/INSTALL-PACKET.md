# INSTALL PACKET - ExcelSummons 0.1.13 (P-SUMMONS-MANA-25)

Unit: ONE gated packet - jar + config + Magic modifiers
together (design draft section 2.5: the holds live in two
files; landing only one is a silent desync). NOT a text
live-edit. DRAFT ONLY - supervisor files to MANIFEST after
the current chain settles and G1 receipts are filled at
filing time. Shape follows the 0.1.11 packet (jar + config
+ modifiers), NOT the jar-only 0.1.12 packet (scope flag B).

## Order vs 0.1.12 (explicit)

Installs AFTER ExcelSummons 0.1.12 (GameplayEvent emitter
adoption, MANIFEST PENDING in the current chain, candidate
a21c584f...). This build is based ON the 0.1.12 tree (copy;
0.1.12 tree/packet untouched), so it carries the D64 holds,
the 0.1.11 nameplate/despawn work, and the emitter adoption
forward. Its live-jar gate is the 0.1.12 jar. If 0.1.12 has
not landed, STOP - do not install 0.1.13 over live 0.1.11.

## Candidate artifacts (this folder)

- ExcelSummons-0.1.13.jar - 116,869 B / SHA-256
  97b67906b7aad6b82c548c9ab025618fa5d7c93151331c029eb21cd824ddc1f5
  build EXC-SUMMONS-0.1.13-WRV-20261115-01-mana25;
  tests 128/128; deterministic double-build + second full
  run byte-identical.
- config.yml (candidate bridge config, +25% reserve rows) -
  6,562 B / 6c449df37666c1d28a3aebc6e04ee55412e84847962b668
  821bdcbeea2ff4479; placed over plugins/ExcelSummons/
  config.yml at the window.
- modifiers-replacement-0.1.13.yml - 3,751 B / b97cd5d14154
  efee951210cba848234d7e901160773b183f94cd2b3188f4fabe -
  REPLACE all 18 wrv_summon_reserve_* template blocks in
  Magic's modifiers.yml with these (same keys, new boosts).
- ExcelSummons-0.1.13-sources.zip - 163,820 B / SHA-256
  27eeed3027ad52569421c4ae53ded0b4a1136f8fe764e5872c6d18652dc094a0
  (46 files, source tree of record in ../ExcelSummons/).

## Scope

- Raised holds (+25% on D64, halves-up; Mike adopted the
  draft table Nov 15, 2026, ~12:28 PM CST): archer 25 /
  bruiser 31 / guardian 44 / revenant 38 / ghast 44 /
  wolf 19 / familiar 13 / cat 13 / mount 19; ceiling 75,
  floor 20, adopted 0 unchanged.
- Accepted consequences: ONE elite at a time (guardian +
  ghast 88, elite + revenant 82 - second elite refused at
  PreCast); Master wolf pack caps at 3 mana-holding wolves
  (57) - a fresh 4th cast is PreCast-refused (57 + 19 = 76
  > 75), while grandfathered 4th/5th wolves hold nothing
  after re-derivation and are never trimmed (scope flag A).
- Exact-fit fields stay legal: archer + bruiser + wolf =
  75; guardian + bruiser = 75; archer + bruiser + mount =
  75; archer + archer = 50; bruiser + bruiser = 62.
- Carried unchanged: 0.1.12 emitter adoption (the audit
  records report the NEW percents - they carry the bridge's
  applied numbers), caps, durations, cast costs, registry
  format. ReservePolicy.java carries a history-comment
  append only; no logic changed anywhere.

## Gates

- Marker discipline with re-checks before Shut Down and
  before Start; BACKUP FIRST + cap-at-6 (name prunees;
  never the Daily newest or the two newest manuals);
  emptiness list = 0/10 twice. Fully gated (jar + config
  + modifiers).
- Live-jar gate (STOP on mismatch): plugins/
  ExcelSummons-0.1.12.jar = 116,690 B / a21c584fd9982ec5
  ce83b652b19b6c9536af31baecdb92251ab42afcf1d81c83
  (the 0.1.12 candidate - proves 0.1.12 landed first).
- Live-premise receipts (G1): BLANK in RECEIPTS.md by
  design - filled same-day at filing (live 0.1.12 jar,
  live ExcelSummons config still on the D64 rows, live
  Magic modifiers.yml reserve blocks still on the D64
  boosts, guild state).

## Install steps (failsafe)

1. Hold aside the live 0.1.12 jar (rollback) and place
   plugins/ExcelSummons-0.1.13.jar.
2. Place candidate config.yml over plugins/ExcelSummons/
   config.yml (in-place .bak of the live file first).
3. Replace the 18 wrv_summon_reserve_* blocks in Magic's
   modifiers.yml from modifiers-replacement-0.1.13.yml
   (in-place .bak first); reload/restart per window plan.
4. Start; run Boot verify; write marker only after pass.

## Boot verify (script-named lines)

- verify_hash.py --file <placed pull-back jar> --size
  116869 --sha256 97b67906...21cd (full hash above).
- verify_hash.py --file <placed pull-back config> against
  the candidate config hash in RECEIPTS.md.
- counts-check on the boot log --pattern
  "EXC-SUMMONS-0.1.13-WRV-20261115-01-mana25" --expected 1;
  "Enabling ExcelSummons v0.1.13" --expected 1; SEVERE 0
  attributable to ExcelSummons; guild state unchanged.
- modifiers check: grep the live modifiers.yml pull-back
  for "wrv_summon_reserve_guardian" block boost -0.44
  (one named spot check per family tier in RECEIPTS.md).

## Same-pass surfaces (owed at install)

Guide knowledge (new hold table + one-elite-at-a-time +
wolf-pack behaviour) + What's New BOTH surfaces + website
pair. Tips check + menus check owed in the surfaces pass
(menu/list lines print applied holds dynamically - verify
no stale percent text). Sweep Guide/website for old D64
percent text (20/25/35/30/15/10 hold claims).

## Proof owed after install

- Mike: summon a Guardian, confirm the mana bar holds
  ~44%; attempt a Ghast alongside it -> ceiling refusal
  with the new numbers in the message.
- Master-rank caster (Jester lane): field cast wolves -
  3 hold mana (57%), the 4th cast is refused; dismiss
  one, cast again succeeds.
- Archer + Bruiser + Wolf field = exactly 75% held, no
  refusal.
- /excelprotect audit spot-check (needs Protect 0.1.10
  live): a reserve refusal records reason=reserve_ceiling
  with the +25% numbers, matching the chat message.
- Adopted pet out alongside cast summons: still holds 0.

Marker on install: ../INSTALLED.txt (supervisor names the
path at filing; 0.1.11 pattern = packet folder marker).
