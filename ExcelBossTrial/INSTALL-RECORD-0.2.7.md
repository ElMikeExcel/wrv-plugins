# ExcelBossTrial 0.2.6 -> 0.2.7 (+ corrected config) - install record

STATUS (Oct 24, 2026): STAGED ONLY, NOT INSTALLED. Held for Iris's cold review (Mike's call);
the rest of today's update ships without it. Needs Mike's go after that review. Install with 0 players online,
and NOT while any boss fight is open (`/bosstrial status` -> "Open encounters: 0",
dragon chapter phase DONE or IDLE).

## Files
| staged file | live path | SHA256 |
|---|---|---|
| ExcelBossTrial-0.2.7.jar (built from this source, see build.sh) (139,567 B) | plugins/ExcelBossTrial-0.2.7.jar | 7b0392d5652b34fd066eb223cdb3b4761152cb0abfef21801cb014c07d5ce4b5 |
| reference/config-staged-0.2.7-2026-10-24.yml | plugins/ExcelBossTrial/config.yml | 6a1dc9812a834e6cb59b5bee5462e20cb3c5287cb28731cad06810f203ce1ac4 |

Expected live before install (STOP and re-diff if different):
- plugins/ExcelBossTrial-0.2.6.jar, 134,387 B (~131.24 KB). Source rebuild hash
  486c57ed40f3d73128c4470ae0a9478565e30d5c7adfc3db307b8b5d17c6a2b5 - the LIVE jar was never
  hashed by this audit; hash it first and compare.
- plugins/ExcelBossTrial/config.yml = 591a8a0258a52b9c62bd7eeb4041047a3fddf1d2eb447d0aac9707ca28f178cf
 .

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
