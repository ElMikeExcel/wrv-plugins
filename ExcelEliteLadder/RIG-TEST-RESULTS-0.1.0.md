# ExcelEliteLadder 0.1.0 rig results (Sun Oct 25, 2026, 1:56 to 2:19 PM CDT)

Rig: a private box rig (offline mode). Paper 26.3, EliteMobs 10.9.10,
ExcelGuildGate 0.3.3, ExcelTrainingWheels 0.1.6, Via for the test bot. Seals were moved with
`guildgate grant <seal>` from the console. That writes GuildGate's own SealState, which is
exactly what GuildGateAPI.sealsBrokenCount() reads, the same as a real kill.
Test bot: one offline-mode client (mineflayer) for "player online".
Expected shas = the staged -norm files in EliteMobs' number form (expected/ folder in the staging bundle).

| # | Test | Result |
|---|---|---|
| 0 | Does EliteMobs rewrite MobCombatSettings.yml on load? | YES. Boot and `em reload` rewrite number text (0.10->0.1, 2.20->2.2, 0.50->0.5, 1.90->1.9). Values unchanged. S1-norm stayed 57ad4ab2 (already in that form); staged S4-norm became 95aface7. Handled: plugin writes that form and compares by value. |
| 1 | First start, live = S1-norm, no state | PASS. "Baseline adopted: live ... equals ladder S1-norm". |
| 2 | Seal 2, 0 online | PASS. Applied S2 within the 10 s poll, em reload, post-reload check OK. sha 9b4c3f95 = expected S2. |
| 3 | Seal 3 with 1 player online | PASS. "S3 is pending and will apply when the server is empty (1 online now)". File stayed S2 for 40 s. Status: "Pending: S3 waiting for an empty server (1 online)". |
| 4 | That player quits | PASS. Applied S3 1 s after the quit. sha 983fa757 = expected S3. |
| 5 | Seal 4, then seal 5 | PASS. S4 sha 95aface7, S5 sha 89fd7435, both = expected. Post-reload check "unchanged by EliteMobs" on every step. |
| 6 | `guildgate reset` (0 seals) | PASS. One WARN, no step-down, file stays S5 (89fd7435). Status: "none (seal count 0 is below applied S5; no step-down)". |
| 7 | Backups | PASS. 4 dated .bak files; each sha = the step before (57ad4ab2, 9b4c3f95, 983fa757, 95aface7). |
| 8 | Live = old S1 (b5cb1d7b, dungeon 1.0/1.0), seal 2 | PASS. Refused: "the OLD S1 file without the dungeon lines (91/94 = 1.0/1.0), not S1-norm". File unchanged, no backup, no reload. |
| 9 | Live = old S0 (2861a26d), seal 2 | PASS. Refused: "the OLD pre-ladder S0 file". File unchanged. |
| 10 | Applied S1 but live toPlayer hand-edited to 1.8, seal 2 | PASS. Refused: "differs from S1-norm at: scaledDamageToPlayerMultiplier=1.8 (want 1.75)". File unchanged. |
| 11 | Corrupt write (fault afterMove: file cut in half after the move) | PASS. "verify failed: line count changed; original restored". File back to 57ad4ab2. No em reload. Latch set. |
| 12 | Failed write (fault beforeMove) | PASS. "original restored". File 57ad4ab2. No em reload. Latch set. |
| 13 | Restart with the latch set | PASS. Still blocked ("a previous apply failed ... /eliteladder retry"). |
| 14 | `/eliteladder retry` | PASS. Latch cleared and S2 applied (9b4c3f95). |
| 15 | EliteMobs really uses the new values | PASS. After the S2 apply, TrainingWheels 0.1.6 read live from EliteMobs memory: "divisor LIVE scaled x1.90 / normalized x2.00". After S3: scaled x2.05. |
| 16 | Flag allow-reload-with-players-online: true, 1 online, seal 3 | PASS. Applied at once ("1 online"), em reload ran, the bot stayed connected (not kicked). Flag set back to false. |

Plain-JVM suite: 57 passed, 0 failed.
Logs stay on the box (staging folder rig-logs/).
