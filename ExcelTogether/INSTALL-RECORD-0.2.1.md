# ExcelTogether 0.2.1 (mcMMO hand-join mirror, login adoption, full-party retry): BUILT AND SCRATCH-BOOTED Oct 16, 2026. NOT INSTALLED LIVE. Java, Bedrock and real-mcMMO checks UNCONFIRMED.

> UPDATE Oct 16, 2026 6:25 PM CDT: 0.2.1 (still NOT installed) holds Jobs and mcMMO party XP sharing WITH the receiver's own boosts stacked on top, the "+25% party bonus (with NAME)" action bar and `/excelparty status`. See SHARING-OPTIONS.md. Jar SHA256 29a6b8923f5d4328749a6ee1d5d222acb5d549eba1b7333dd99694f167dc226b. Tests 392/392 (18 PartyTest + 195 PartyFeatureTest + 79 McmmoLinkTest + 86 McmmoSyncTest + 30 ShareTest + 22 XpShareTest, the last checked against the real mcMMO 2.3.003-SNAPSHOT jar for every reflected method including the receiver-boost path). Scratch boots (logs/scratch-boot-0.2.1-share2-*.log) passed WITH and WITHOUT mcMMO. Live Jobs payout, real two-player sharing, Java and Bedrock look: UNCONFIRMED. The live ExcelTogether config.yml needs no edit (new keys default in code: share-mode full, share-percent 100, share-idle-seconds 120, share-mcmmo-xp true).

Replaces 0.2.0 (live since 5:03 PM CDT Oct 16, jar SHA c37e7f5e980669ce709fcaec2f2b769702432fce4d51fdabb4a46ea6bf661044).

| Status | ExcelTogether 0.2.1 |
|---|---|
| Built | Yes (JDK 25, --release 21, Paper API 26.2 build 133). Built twice, same SHA |
| Automated tests | 377/377 (18 PartyTest + 194 PartyFeatureTest + 79 McmmoLinkTest + 86 McmmoSyncTest) |
| Scratch boot WITH mcMMO (2.3.003-SNAPSHOT, mcMMO-together-bonus.jar SHA d58bb65b...) | Passed. "mcMMO party link: ON (mcMMO 2.3.003-SNAPSHOT, hand joins mirrored by event + poll)". A real McMMOPartyChangeEvent fired by the harness went through our live listener with no error |
| Scratch boot WITHOUT mcMMO | Passed. "mcMMO party link: OFF (mcMMO not installed)", enabled clean |
| Installed live | NO |
| Java check / Bedrock check | UNCONFIRMED |
| Real two-player hand join through the real mcMMO | UNCONFIRMED (scratch has no player) |
| GitHub / website / version map / Big Brain | NOT DONE (publish at install time, PR stays an open draft) |

## What it does
- Watches mcMMO party changes two ways: the mcMMO event McMMOPartyChangeEvent (registered by reflection, no hard dependency; fired by mcMMO BEFORE it changes anything, so we collect the old and new party members then and re-check one tick later), plus a poll every 2 seconds that covers what mcMMO fires no event for (owner change, offline kick). The server's own mcMMO build (2.3.003-WRV-XP-2) does not touch the party event code (checked its diff).
- State based: each check compares "what mcMMO says" with "what ExcelTogether holds" and fixes the difference. Running it again changes nothing.
- Hand join (/party accept, /party join, leader invite): the joiner goes into the ExcelTogether party that holds their mcMMO mates (the mcMMO leader's if several). If nobody in the mcMMO party has an ExcelTogether party, one is made from every ONLINE, free member, mcMMO leader as leader. Cap 5: players who do not fit stay in mcMMO and get one plain note (no repeats).
- Hand quit, kick, disband: the player leaves ours too (a pair ends, the last one is told). Only players seen together in both systems are removed, so someone the link never touched keeps their party.
- Hand owner change: ours follows (mcMMO is the source, one way only, never pushed back).
- mcMMO party rename: link bookkeeping follows so a later ExcelTogether leave still removes them from mcMMO.
- Moving to a different mcMMO party: a mirrored player follows into the matching ExcelTogether party.
- Loop guards: the sync only reads mcMMO and never calls our invite/accept/leave mirroring; things we remove from ours on purpose are not pulled back until mcMMO shows them leave or join again; our own accept records both players as mirrored right away.
- Soft dependency: mcMMO absent, party system off, or API different = nothing runs and 0.2.0 behavior is unchanged. Any error on the mcMMO side is swallowed and ExcelTogether keeps working.
- New files/keys: none. config.yml is identical in content to 0.2.0 (only the version comment changed), so the existing live config needs no edit.

- Adopt on login and on plugin enable: when a player logs in (or is already online at enable/reload), their mcMMO party is adopted with the same rules as a hand join (mcMMO leader leads, cap 5, one plain note if full). mcMMO may load the profile a moment after login, so it keeps checking for about a minute (a profile that never loads is skipped, no error). After a restart the party rebuilds as soon as two members are online; later members join as they log in.
- Full-party retry: a player who did not fit stays in mcMMO and is retried every 2 seconds until a seat frees (leave, kick, offline timeout). One note only, never repeated. If they log off while waiting they are dropped and not adopted while away.
- Unlinked ExcelTogether party member who deliberately joins a different mcMMO party by hand: mcMMO wins. They leave their old ExcelTogether party and join (or form) the one with their new mcMMO mates, with a plain note to them and to the old party. If there is no landing (their ExcelTogether party is full, or no mate is online to form one) they stay put with one note. A poll alone never moves an unlinked member.

## Known limits (honest)
- Offline mcMMO members are skipped when ours is first built; they are added when they log in.
- A player who stays on a full party because the cap is 5 is waiting only while online and while the mcMMO party lists them.
- Never tested with a real player on the real mcMMO. Parties are memory only by design; adoption now rebuilds them from mcMMO after a restart.
- UNCONFIRMED: that the live mcMMO jar (2.3.003-WRV-XP-2) is byte-identical to the mcMMO-together-bonus.jar used in the scratch boot; its source diff vs upstream (PartyInfoCommand, GeneralConfig, McMMOPlayer, PartyTogetherBonus) does not touch party events or PartyManager.
- Tested against a stub shaped like mcMMO and an in-memory fake; never with a real player on the real mcMMO.

## Install plan (when Mike says go; reversible, not a big update)
1. Players empty (check twice, hold if Kailey .coolgirlkail or anyone is on), run save-all, panel Shut Down.
2. plugins/: rename ExcelTogether-0.2.0.jar out of the active set (ExcelTogether-0.2.0.jar.bak-YYYYMMDD, outside plugins/*.jar), upload ExcelTogether-0.2.1.jar. Keep plugins/ExcelTogether/config.yml as is.
3. Start. Expect "ExcelTogether 0.2.1 enabled ... mcMMO party link: ON (mcMMO ..., hand joins mirrored by event + poll)". "poll only" is acceptable but report it.
4. Check after: two players, one runs /party accept by hand and shows up in /excelparty list; /party quit removes them.
Rollback: Shut Down, delete ExcelTogether-0.2.1.jar, restore the .bak as ExcelTogether-0.2.0.jar (SHA must be c37e7f5e980669ce709fcaec2f2b769702432fce4d51fdabb4a46ea6bf661044; a copy is in this bundle as ROLLBACK-ExcelTogether-0.2.0.jar), start. No data to clean up.
Files: jar, source zip, 0.2.0 to 0.2.1 diff, rollback jar, logs/ (tests, two scratch boots, scratch-only harness source), SHA256SUMS.txt.


## mcMMO Together range 32 -> 50 (separate step, needs Mike's go; do it in the same empty-server window as the 0.2.1 install)
Why: mcMMO's own Together bonus (x1.25 on the earner's skill XP) uses its own range. Ours is 50 blocks. At 33 to 50 blocks the party share still works but mcMMO's own x1.25 would not, and the shared base would be unboosted for the earner.
- File: `plugins/mcMMO/config.yml` on the server (panel File Manager).
- Exact key (source: GeneralConfig.getPartyTogetherBonusRange, `config.getDouble("Party.Together_Bonus.Range", 32.0D)`):
  ```
  Party:
      Together_Bonus:
          Range: 32.0     ->    Range: 50.0
  ```
  The scratch copy of the config has this key at 32.0. If the live file has no `Range:` line under `Together_Bonus:`, add `Range: 50.0` at the same indent as `Multiplier:` (the code default is 32.0).
- Do NOT touch `Party.Sharing.Range` (75.0, mcMMO's own XP share range) or `Multiplier` (1.25).
- Steps: (1) players empty twice, save-all, panel Shut Down. (2) Download plugins/mcMMO/config.yml and keep it as `config.yml.bak-20261016-range` OUTSIDE the plugins folder tree (for example in the Drive backup folder, not in /plugins). (3) Edit the one line, save. (4) Start. (5) Console shows no mcMMO config warning ("Party.Together_Bonus.Range should be greater than 0!" would mean a typo). (6) Check: an mcMMO party pair 40 blocks apart should see x1.25 skill XP (UNCONFIRMED until tested live).
- Rollback: restore the saved config.yml (or set Range back to 32.0), restart empty.
- The value is read at startup; do not rely on a reload command.
- Range 50 is blocks. Mike said 50 feet in one message: confirm if it should be 50 blocks (as ExcelTogether) or about 15 blocks.
