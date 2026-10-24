# ExcelBossTrial 0.2.7 - test evidence (all times CDT, Sat Oct 24 2026)

Build: JDK 25 javac --release 21. Reproducible: two clean builds give the same
7b0392d5652b34fd066eb223cdb3b4761152cb0abfef21801cb014c07d5ce4b5.
The 0.2.6 baseline commit on this branch rebuilds byte-identically to the 0.2.6 rig jar (486c57ed...,
134,387 B = the 131.24 KB live size).

Unit suites:
- baseline 0.2.6: 29 suites, 993 passed, 0 failed.
- 0.2.7: 30 suites, 1023 passed, 0 failed. The new suite is EncounterRetentionTest (30 checks).

Box rig: Paper (box test server), LIVE BossTrial config (later the staged config), ExcelGuildGate 0.3.3,

| test | jar | result |
|---|---|---|
| A2: plain dragon kill, live config | 0.2.6 | CREDIT; "First Breath" elytra (mending 1, unbreaking 3, lore "Earned by <name>") to each of 2 champions |
| Elder walk-away (elderC) | 0.2.6 | honest solo kill DENIED at 43.7% + 6 h cooldown (bug) |
| Dragon wipe/leave (dragonB) | 0.2.6 | phase-2 fight reset to phase 1 at 400 HP (bug) |
| B2: same as dragonB | 0.2.7 | held ("Nobody at the arena"), resumed, CREDIT, First Breath x2 |
| C2: same as elderC | 0.2.7 | elder progress 103.7%, no cooldown |
| D: restart mid-elder + mid-phase-2 dragon | 0.2.7 final | elder re-open set aside 420 HP -> progress 109.3%; dragon restored PHASE2 and held at boot, set aside 642 HP -> CREDIT 110.6% (9:33-9:36 AM) |
| N: plain dragon kill + plain elder kill, STAGED config | 0.2.7 final | CREDIT 103.4%, SIGNATURE paid, First Breath x2 (mending 1, unbreaking 3); elder progress 101.3%; no phantom re-open after the kill (9:37 AM) |
| Staged config reload | 0.2.7 | `/bosstrial reload` and status are identical to the live config, with no warnings (9:30 AM) |

The corpse guard was added after an N run at 9:31 AM on the earlier 0.2.7 build. Hits during the dragon's death
animation logged "re-open ... 1299 HP set aside". The same script on the final build produced no such line.

Not rig-tested: a 0.2.6 restart in the middle of an elder/wither/warden fight (inferred from the code); wither and warden kills on 0.2.7
(the same ledger path as the elder; covered by unit tests); Bedrock clients.

Re-checked Oct 24, 2026 10:31 AM CDT from this exact tree before publishing: jar rebuilds to
7b0392d5...ce4b5, the baseline commit rebuilds to 486c57ed...c6a2b5, and all 30 suites pass
(1023 passed, 0 failed). Rig logs are not published.
