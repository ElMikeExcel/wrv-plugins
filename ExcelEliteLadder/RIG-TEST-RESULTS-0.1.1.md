# ExcelEliteLadder 0.1.1 rig results (Sun Oct 25, 2026, 11:49 to 11:52 PM CDT)

Rig: /workspace/eliteladder-rig (own copy, port 25937, offline mode), same stack as 0.1.0:
Paper 26.3, EliteMobs 10.9.10, ExcelGuildGate 0.3.3, ExcelTrainingWheels 0.1.6. The 0.1.0 jar
was removed and the 0.1.1 jar put in its place (the upgrade path). Seals moved with
`guildgate grant <seal>` from the console. No SEVERE or ERROR lines in any of the three boots.

| # | Test | Result |
|---|---|---|
| U1 | Upgrade at the live state: file S1-norm (57ad4ab2), state appliedStep 1, 1 seal | PASS. "enabled: applied step S1", "seals observed: 1 of 5". Status: Applied S1, Live file S1-norm, Pending none. File unchanged (57ad4ab2), no backup, no em reload. |
| U2 | File S4-norm EM form (95aface7), applied S4, seals to 5, 0 online | PASS. "EliteLadder applied S5 (from S4, seals 5, 0 online): spawn 0.12, cap 120, toElite 0.45, toPlayer 2.2, dungeon 0.5/2.0". em reload ran. Post-reload check: values match, sha 49763c61... unchanged by EliteMobs. Backup ...-from-S4.bak written. |
| U3 | File = 0.1.0-era S5 (spawn 0.11, 89fd7435), applied S5, seals 5 | PASS (expected refusal). Status: "Live file: OFF LADDER (... differs from S5-norm at: eliteMobsSpawnPercentage=0.11 (want 0.12))", Pending none. File unchanged, no reload. Confirms: install 0.1.1 before Seal 5, or fix by hand. |

Plain-JVM suite: 63 passed, 0 failed.
Logs: rig-logs/ in the release folder (U1, U2, U3).
