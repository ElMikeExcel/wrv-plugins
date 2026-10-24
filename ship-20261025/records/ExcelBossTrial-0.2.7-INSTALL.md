# ExcelBossTrial 0.2.6 -> 0.2.7 (+ corrected config) - install record

STATUS: STAGED ONLY. Nothing installed. Needs Mike's go. Install with 0 players online,
and NOT while any boss fight is open (`/bosstrial status` -> "Open encounters: 0",
dragon chapter phase DONE or IDLE).

## Files
| staged file | live path | SHA256 |
|---|---|---|
| build/ExcelBossTrial-0.2.7.jar (139,567 B) | plugins/ExcelBossTrial-0.2.7.jar | 7b0392d5652b34fd066eb223cdb3b4761152cb0abfef21801cb014c07d5ce4b5 |
| config/config.yml | plugins/ExcelBossTrial/config.yml | 6a1dc9812a834e6cb59b5bee5462e20cb3c5287cb28731cad06810f203ce1ac4 |

Expected live before install (STOP and re-diff if different):
- plugins/ExcelBossTrial-0.2.6.jar, 134,387 B (~131.24 KB). Source rebuild hash
  486c57ed40f3d73128c4470ae0a9478565e30d5c7adfc3db307b8b5d17c6a2b5 - the LIVE jar was never
  hashed by this audit; hash it first and compare.
- plugins/ExcelBossTrial/config.yml = 591a8a0258a52b9c62bd7eeb4041047a3fddf1d2eb447d0aac9707ca28f178cf
  (copy made from /workspace/live-bosstrial-config.yml).

## Backups to take and KEEP (never overwrite an existing .bak; if the name exists add -2)
1. plugins/ExcelBossTrial-0.2.6.jar -> move OUT of plugins/ to backups/ExcelBossTrial-0.2.6.jar.bak-20261024
   (do not leave two BossTrial jars in plugins/).
2. plugins/ExcelBossTrial/config.yml -> plugins/ExcelBossTrial/config.yml.bak-20261024
3. plugins/ExcelBossTrial/state.yml -> plugins/ExcelBossTrial/state.yml.bak-20261024 (format unchanged; safety copy)
4. plugins/ExcelBossTrial/dragon/ (folder) -> plugins/ExcelBossTrial/dragon.bak-20261024/

## Steps
1. Stop server (full stop; no /reload, no plugin managers).
2. Take backups 1-4.
3. Copy the 0.2.7 jar and config.yml in; verify both SHA256s on the server.
4. Start. Boot log must show `ExcelBossTrial 0.2.7` enabled, no WARN about unknown config keys.
5. `/bosstrial status` must match pre-install: the same pools (wither 1500+300, elder 700+75, warden 1300+200,
   dragon 400/900), share gate on, cooldowns, monument counts, signatures paid, Announcements until
   Sat Oct 24 11:50:41 PM CDT.

## Rollback
Stop -> delete plugins/ExcelBossTrial-0.2.7.jar -> restore ExcelBossTrial-0.2.6.jar.bak-20261024 as
plugins/ExcelBossTrial-0.2.6.jar and config.yml.bak-20261024 as config.yml -> start.
state.yml from 0.2.7 is readable by 0.2.6 (no new keys written).

The config can ship without the jar (it only makes running defaults explicit and fixes comments):
the values are identical to what 0.2.6 runs now.

## Test-file name hygiene (Oct 24)
- fix/tests/.../LedgerGrowthTest.java is the renamed (neutral) version. It is excluded from
  ExcelBossTrial-0.2.7.diff on purpose; copy it whole from fix/ when publishing the source.
- Keep LedgerGrowthTest.java.pre-rename.bak and orig/ out of any repo (they hold the old wording).
- Round 2 (Oct 24, 10:07 AM CDT): a player gamertag and a family nickname were also in comments of
  PlacementRegistry.java and WardenGiveUp.java and in SealHardeningTest, ProvenanceLoreTest and
  BedIgnitionLogTest. Replaced with neutral names in fix/ (originals in pre-scrub-bak/). These five files are
  also excluded from the diff (orig and fix were otherwise identical for them); ship them whole from fix/.
- Check: the jar rebuilds to the same sha256 7b0392d5...ce4b5 (comment-only main-source change), and all
  30 suites pass with identical counts, 1023 passed 0 failed (fix-tests-round2.log).
