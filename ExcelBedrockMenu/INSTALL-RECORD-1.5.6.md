# ExcelBedrockMenu 1.5.6 (menu changes + Spawn fix): INSTALLED LIVE Oct 16, 2026, 12:25 PM CDT
| Status | State |
|---|---|
| Built | Yes (final2), JDK 25, release 21 |
| Tests | Unit tests in MenuLogicTest updated; exact pass count not re-recorded here: UNCONFIRMED |
| Test server | Not re-recorded here: UNCONFIRMED |
| Installed live | YES, 12:25 PM CDT (booted in 17 s, no plugin errors, nobody online) |
| Bedrock check | PASSED by Mike in game: menu looks good, Excel Shop label good, Spawn goes to the village, chat works |
| Java check | UNCONFIRMED (Java server_menu.yml replaced; Mike has not tested the Java menu) |
SHA-256 ExcelBedrockMenu-1.5.6.jar: 27c895fab8790b4d90587a146b3280f5895b6dad7c7b4defba33e10b7850b7e1
SHA-256 server_menu.yml installed: 0d07dc1c6647eeba35192bbb7f3f1d74abbe5930d0be58fea282bbc796fd529e
Changes: removed "Road to the Guild" (Java and Bedrock); locked guild label plain "Adventurers Guild"; shop button "Excel Shop"; Spawn uses real /spawn (overworld); version 1.5.6. Combined diff vs 1.5.4 is in this folder.
Also installed same restart: ExcelTogether 0.1.0 (see PR #31) and server.properties enforce-secure-profile=false (Bedrock chat fix).
Backups: every replaced file kept as .bak-20261016b (old 1.5.4 jar retired, one active jar).
Rollback: panel Shut Down, restore the 1.5.4 jar and server_menu.yml from .bak-20261016b, remove 1.5.6 jar, set enforce-secure-profile back only if wanted, start.
