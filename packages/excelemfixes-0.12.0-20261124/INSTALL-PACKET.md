# INSTALL PACKET -- ExcelEMFixes 0.12.0 (EM-world Acrobatics ceiling)

Lane: ~/workspace/mcmmo-acrobatics-check-20261124/
Build record: ~/workspace/mcmmo-acrobatics-check-20261124/BUILD-RECORD.md
Decision: D220 (Mike approved the ceiling number Nov 24, 2026,
11:17 AM CT, after board inquiry 5 the same morning).

## What this is

One module part added to the pack's mcMMO XP Connector
(mcmmo-xp-connector), nothing else: the EM-world gain ceiling
(part 5). Acrobatics gains a player earns while standing in an
EliteMobs world now count toward their own per-player
rolling-hour ceiling of 100,000 XP. A gain that would cross the
ceiling is TRIMMED to the remaining budget (not lost whole);
once the budget is spent, further in-world gains pay 0 until
the rolling hour falls back under. Gains in ordinary worlds
are never counted and never touched, other skills are never
counted or touched, and kill awards are unaffected (Acrobatics
is never an award skill).

Why: mcMMO's Dodge pays 800 XP per point of dodged damage
(the stock rate), and EliteMobs elites hit for EliteMobs-sized
damage. On Nov 24, 2026 a player gained 90,646 Acrobatics XP
in one rolling hour in Primis doing nothing but ordinary
quests; the XP watchdog flagged the rate and the play itself
was cleared as legitimate. The Nov 23 rebalance's 60,000/hr
ceiling counts kill awards only, so this source was unpriced.
Mike's ruling: count EM-world Acrobatics under a ceiling of
its own, sized from the server's measured data -- the
heaviest legitimate hour on record is that 90,646, so the
ceiling sits about 10% above it, and below the watchdog's
120,000 all-worlds hard cap. The module documentation
(config comments + pack README) carries this reason, per the
standing module workflow rule, and both state plainly that
100,000 is Wink Reyes Valley's sizing from its own data --
another server should size the knob from its own watchdog
data, not copy the number.

Mechanics, briefly: a second WatchdogLedger persisted as
mcmmo-emceiling-ledger.txt in the plugin folder (additive new
data file), fed by the same MONITOR observed-gain feed the
watchdog uses, counting only covered skills (config list,
default ACROBATICS) in worlds the EmWorlds classifier
recognises (every em_ world incl. instanced copies, plus
GoblinKingdom_vf and its copies, plus config extra
names/prefixes; fail-open -- an unmatched world is never
touched). Enforcement is one trim step in the bridge's LOWEST
veto, after the watchdog-cap block. The first gain that
reaches the ceiling in an hour appends an EMCEILING record to
mcmmo-watchdog-flags.txt (same file and line format as the
watchdog's own flags) so the nightly audit sees it.

## Artifacts (in this folder)

- ExcelEMFixes-0.12.0.jar -- 459,581 bytes
  SHA-256 e71620292afe95b58c90650c2d65d85f1a5da8163ecb279d6de861619be4a9a2
  Deterministic double-build byte-identical (build.sh in the
  lane's build-root/).
- deploy-config.yml -- 64,032 bytes
  SHA-256 0c694bfa3d0bf9165f451c797637cbfe87767882de621f6fd55139971ba25cff
  The live config with ONLY the connector section extended
  (the em-world-ceiling block inserted after the watchdog
  block). Verified mechanically: removing the inserted
  49-line block reproduces the gated live config byte-for-byte.

## Provenance gates (STOP if either fails)

- Live plugins/ExcelEMFixes-0.11.0.jar must hash
  98269ab667d0a5fb4e85cd38b7715f855e0e890ed858fa1ed062fff0fcc200f2
  (456,032 bytes -- the 0.11.0 build installed Nov 24, 2026).
- Live plugins/ExcelEMFixes/config.yml must hash
  f076a3517a4ad38935ea7c55bbf5f612be3d51633cf814eabe4e43f56d80edd3
  (61,436 bytes -- the deploy config placed by the 0.11.0
  install).
If the jar differs, STOP and re-base. If the config differs,
do NOT place deploy-config.yml: section-merge instead --
insert only the em-world-ceiling block from the packet's
deploy config into the live config, immediately after the
watchdog block (after the "cap-hour-xp: 120000" line) inside
modules.mcmmo-xp-connector, backing the live file up first.

## Placement

1. Snapshot first (fleet standard).
2. Stop the server (verified empty, fleet standard).
3. Jar: MOVE plugins/ExcelEMFixes-0.11.0.jar aside as
   ExcelEMFixes-0.11.0.jar.bak-<install date>-pre0120
   (a move, never a copy -- two pack jars must never be
   co-resident), then place the packet jar as
   plugins/ExcelEMFixes-0.12.0.jar.
4. Config: back the live config up as
   config.yml.bak-<install date>-pre0120, then place
   deploy-config.yml as plugins/ExcelEMFixes/config.yml
   (or the section-merge above if the gate said so).
5. Start the server.

## Acceptance

- Boot log carries the exact line "ExcelEMFixes 0.12.0 ready
  with 19 modules" and the "mcMMO XP Connector ready" line.
- `emfixes reload` reports ZERO reload-failed.
- SEVERE count 0 for the boot.
- plugins/ExcelEMFixes/mcmmo-emceiling-ledger.txt appears on
  the first covered in-world gain (or the first state save);
  its absence before any such gain is normal.

## Proof (all PASS; receipts in the lane's receipts/)

- Unit suites: 1,055 checks, 0 failed (1,029 pre-existing +
  26 new EMWORLDS / EMHEADROOM checks). One intermittent
  failure of the pre-existing skill-nudge tie-break check was
  observed once across three runs -- the documented
  pre-existing flake, unrelated to this change; the two
  following runs were clean.
- Scratch rig (connector lane's rig pattern, gates-deleted
  mcMMO fork, never live), rig ceiling 2,500 unless noted:
  EMCEIL-1 two 1,000 grants in em_rig paid in full, headroom
  449 = 2,500 - paid (the ledger counts final amounts,
  early-game boost included); EMCEIL-TRIM the crossing grant
  wrote EXACTLY the 449 headroom -- the partial-set trim
  survives the pipeline, so the plan's hard-zero fallback was
  NOT taken; EMCEIL-2 a further grant wrote 0 and an
  EMCEILING record exists in mcmmo-watchdog-flags.txt;
  EMCEIL-3 a 5,000 overworld grant paid in full and the EM
  ledger did not move; EMCEIL-4 a 5,000 SWORDS grant in
  em_rig paid in full and the headroom API returns -1 for an
  uncovered skill and for a non-EM world; EMCEIL-5a after a
  plain restart the spent budget was still spent (headroom 0,
  grant 0); EMCEIL-5b with the ledger aged past the rolling
  hour, grants paid in full again; EMCEIL-6 at the shipped
  100,000: 3 x 30,000 paid in full (90,000), the 4th trimmed
  to exactly the 10,000 headroom, the 5th paid 0, EMCEILING
  flag present, and the watchdog's own 120,000 cap was never
  reached, so the zero is the ceiling's; EMCEIL-OFF with the
  ceiling disabled, in-world grants paid without limit;
  module-disabled boot: pack enables clean, no connector,
  SEVERE 0. SEVERE was 0 in every rig boot.
- Honest limit (from the proof plan): real Dodge procs cannot
  be produced headlessly; the ceiling counts written gains
  regardless of source at the same MONITOR feed the watchdog
  already proves against, so a gain's source cannot change
  the ceiling's behaviour.

## Proof owed after install

- The first real EMCEILING flag on live (or 7 quiet days):
  watched via the nightly audit's flags-file sweep, per the
  ceiling plan. A bite on ordinary play means the sizing is
  wrong and the knob moves -- config only, no rebuild.
- Upstream: this release is player-feedback-driven, so the
  loop's upstream notice to MagmaGuy naming ExcelEMFixes
  0.12.0 is owed -- AFTER live proof, per Mike's Nov 24
  testing bar (parent lane sends it; this packet does not).

## Surfaces (same pass)

See SURFACES.md in this folder: the in-game What's New TEXT
entry (id 2026-11-24-acrobatics-em-ceiling) rides as a
companion live-edit placement; the website plugin-row bump
0.11.0 -> 0.12.0 is a parent follow-up.

## Rollback

Stop the server; remove ExcelEMFixes-0.12.0.jar; move
ExcelEMFixes-0.11.0.jar.bak-<install date>-pre0120 back to
ExcelEMFixes-0.11.0.jar; restore
config.yml.bak-<install date>-pre0120 over config.yml; start.
No data migration either way: mcmmo-emceiling-ledger.txt is
additive and is simply ignored by 0.11.0 (it may stay in the
folder; if 0.12.0 is ever reinstalled it resumes the rolling
hour from it, ageing out within the hour regardless).
