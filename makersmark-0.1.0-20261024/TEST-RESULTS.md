# ExcelMakersMark 0.1.0 test results (2026-10-24)

Jar: ExcelMakersMark-0.1.0.jar
sha256 7490006ecc46dc2951a38b76ee1da6f4993c25ae702f4517986d70721e3290ad
Test server: Paper test rig with EliteMobs 10.9.10, Magic 11.2.5, EssentialsX, Geyser/Floodgate, PlaceholderAPI. Never run on live.

## NOT tested / NOT verified (read first)
- Bedrock in the Nether: NOT VERIFIED. The test Bedrock client never finishes loading into the Nether (dimension change never acknowledged, "Checksum mismatch" desyncs). Server-side counting is the same code that passes on Java there.
- Bedrock in the EliteMobs dungeon world (em_id_test_dungeon): NOT VERIFIED, same test-client reason.
- Real EliteMobs dungeons: not tested. Live only has the Adventurers Guild world; a stand-in dungeon world (em_id_test_dungeon) with a summoned EliteMobs test boss was used.
- A real Bedrock phone or tablet: not tested (test client only).
- Real players with real gear history: not tested.

## Final runs on jar 7490006e
| Run | Result |
|---|---|
| TIPS3 first-use tip repeats until OK (fresh names) | 9/9 PASS |
| R3b core signing/anvil/smithing (t.js) | 28/28 PASS |
| R4b core signing/anvil/smithing (t.js) | 28/28 PASS |
| R3b multi-world (t7worlds) | 59/64; the 5 fails were all em_id_test_dungeon during a 53 s whole-rig freeze (5:57:08-5:58:01 PM CDT), no plugin errors |
| R3c em_id_test_dungeon rerun | 22/22 PASS |
| R4b multi-world (t7worlds): overworld, Nether, End, em_adventurers_guild, em_id_test_dungeon | 64/64 PASS |
| R3b Bedrock (t5bed) | 14/14 PASS |
| R4 t2 / t3 / t6mend / bed | 31/31, 6/6, 2/2, 14/14 PASS |
| R4 persist: restart, rollback (remove jar), reinstall | 3/8/5/2 PASS |
| Bedrock other worlds (jar 2630719, gameplay code identical to final) | overworld 6/6, End 6/6, em_adventurers_guild 4/4 PASS |
| Combined boot with tonight's batch (7 batch jars + MakersMark) | 0 SEVERE, 18/18 plugins enabled, PlaceholderAPI makersmark registered, compass menu with Maker's mark entry (slot 23) loaded |

Jar sha256 checked before and after the final runs: unchanged.

## Notes
- Earlier failures were rig/test-client problems, each fixed in the test only: chipped anvil opening the wrong window (t.js), EliteMobs turning a test husk into an elite that killed a 20 HP test bot (t7worlds: 1000 HP bots, extra hits), shared Bedrock test UUID left in the Nether (playerdata reset).
- Combined boot: the only ExcelMenu errors are the spawnshop menus, because the test copy is missing spawnshop_safe_blocks.yml. Same errors appear in every earlier rig boot, not related to MakersMark.
- Bugs found and fixed during testing: EliteMobs named-boss trophies never matched (name now remembered at spawn and first hit); ExcelTips hand-over stopped the tip after 3 shows (hand-over now off by default, built-in tip repeats until OK).
