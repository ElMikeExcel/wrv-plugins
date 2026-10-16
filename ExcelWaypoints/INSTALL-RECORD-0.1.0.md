# ExcelWaypoints 0.1.0 (WRV-017 Home Waypoints): BUILT, NOT INSTALLED. Oct 16, 2026
| Status | State |
|---|---|
| Built | Yes, Oct 16, 2026 (JDK 25, release 21, Paper API 26.2 build 133), reproducible (two builds, same SHA) |
| Automated tests | 225/225 (see TEST-REPORT-0.1.0.md) |
| Scratch boot (test server) | Passed twice on Paper 26.2 build 133: full plugin set, and minimal set with a corrupt places.yml |
| Installed live | NO. Not approved, not installed. Server untouched. |
| Java check | UNCONFIRMED (no real player has used the chat panel) |
| Bedrock check | UNCONFIRMED (forms verified against real Cumulus classes, never seen on a phone; longest list button 42 chars) |
SHA-256 ExcelWaypoints-0.1.0.jar: b68472f177eb390f28809c60524a42247a707f4afb3e83bbb799802eaaad6768
SHA-256 ExcelWaypoints-0.1.0-source.zip: cc74b658abadf88e7384c5dc3efa659f6b3839ebf83ec9c794cf651b78b89bc6
Drive (when published): Minecraft WRV, Build files (jar) and Source (zip). NOT uploaded yet.
What it does: players save up to 5 named places (House, Farm, Mine, Other), see distance and direction, and follow a compass. Java gets a clickable chat panel (/places). Bedrock gets forms. Places are private until the owner shows them to family. Family can see only shared places. "Ask to visit" runs the normal Essentials /tpa request and the owner must accept.
Mike decisions applied (Oct 16, 2026 3:36 PM): view and compass only, visits via existing /tpa accept; menu icon plus compass, no placed block; only owner-shared places visible to family.
Not included: any teleport, /home, placed marker blocks, map markers, a button in ExcelBedrockMenu (would be a 1.5.7 bump of a live plugin, needs Mike's go).
Dependencies (plain words): none required. Essentials optional for Ask to visit. Geyser/Floodgate optional for Bedrock forms. Data: plugins/ExcelWaypoints/places.yml (small file, atomic writes, corrupt copy kept).
Defaults chosen in build (change only if Mike says): guild world em_adventurers_guild cannot save places (config blocked-worlds); duplicate name refused; 30 s visit-request cooldown; chat name capture 30 s; delete confirm 30 s; places private by default.
Install (when Mike approves): server empty (check players twice, Kailey .coolgirlkail rule), save-all, panel Shut Down, drop ExcelWaypoints-0.1.0.jar into plugins, start. New plugin, so no old jar to back up.
Rollback: panel Shut Down, delete ExcelWaypoints-0.1.0.jar from plugins (leave plugins/ExcelWaypoints/ data), start. Nothing else is changed, so rollback is complete.
Unconfirmed: real Java and Bedrock use, interaction with live /tpa on the live server (checked only that tpa is looked up at click time), phone button length.
