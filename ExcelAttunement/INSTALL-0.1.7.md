# INSTALL: ExcelAttunement 0.1.7 stance scroll fix (standalone, Oct 26, 2026)

**What:** a one-jar swap. Live `plugins/ExcelAttunement-0.1.6.jar` (441d3f70) is replaced by `ExcelAttunement-0.1.7.jar` (4b5d8837).
- Nothing else changes: no config edit, no Magic files, no other plugin.
- Approved by Mike to ship BY ITSELF at the next empty moment (Oct 26, about 12:58 AM CDT).
- It is NOT part of the Mend, Tap bar or trash batches.

**Applies with:** a full server restart (jar swap). `/magic load`, `/reload` or plugman are NOT valid for this.

Working dir on the box: `/workspace/batch-next/stance-scroll-fix-20261026` (call it `$D`). SFTP is down (the box gets out on 443 only), so all live work goes through the Shockbyte panel in the browser: File Manager, Console, and the Shut Down / Start buttons.

## Hard rules (all apply)
- Stop ONLY with the panel **Shut Down** button. Never type `stop` in the console.
- Clock gate: no Start or Shut Down click at :02-:06 or :32-:36 past the hour.
- Do not start at or after 1:40 AM CDT. Iris's 2 AM job owns 2-5 AM. If it is past that, report "HELD-CLOCK" and let the parent decide. Do not duplicate Iris's jobs.
- Never kick anyone. `enforce-whitelist` stays false. Never open, download or overwrite `plugins/ExcelGuide/config.yml`.
- Do not copy the numeric server IP into notes or chat (write `<ip>`). Never print rcon or password lines.
- Never overwrite an existing `.bak`. If the name is taken, add `-2`, then `-3`, and so on.
- Upload rule: upload as `NAME.uploading`, rename to `NAME`, download it back, and the sha256 must match. On a mismatch, STOP and roll back (below).
- Never delete a live file. The only exception is a failed `.uploading` temp file.

## 0. Preflight (box)
```
cd $D && sha256sum -c SHA256SUMS && date
```
Every line must be OK. Create `$D/NOTES-run.md` and write the times in CDT there.

## 1. Drift guard (read-only, server can stay online)
- File Manager, `plugins/`: the only ExcelAttunement jar must be `ExcelAttunement-0.1.6.jar`.
- Download it to `$D/live/ExcelAttunement-0.1.6.live.jar` and run `sha256sum` on it. It must be `441d3f707283c28f293dbce5802e94236c2b856efbe24a8e9d36c6c053e3cc2b` (69,475 B).
- If it differs, or there is a different or second ExcelAttunement jar: STOP. Write "HELD-DRIFT" and report.
- Console `plugins`: write down the count (baseline for step 7).

## 2. Whitelist on with only <owner account>
1. Console `whitelist list`. Write the exact output in NOTES-run.md (names stay in the private notes only).
2. Write down whether the whitelist is ON or OFF now:
   - Download `server.properties`. Do not open it in the panel editor.
   - On the box, run `grep -E '^(white-list|enforce-whitelist)=' live/server.properties`.
   - Print only those two lines. `enforce-whitelist` must be `false`; if it is not, STOP and report.
   - Note: the 12:37 AM cutover may have left the whitelist ON for Mike's test. The original server state is OFF.
3. If `<owner account>` is not on the list: `whitelist add <owner account>` and note `ADDED_MIKE=yes`.
   If OTHER names are on the list, do not remove them; note them. (`whitelist on` with enforce false kicks no one.)
4. `whitelist on`. Confirm the console reply.

## 3. Player gate (the next empty moment)
- Console `list` twice, at least 60 s apart. Both must show 0 players. Then go to step 4 (`save-all` first and wait for "Saved the game").
- If anyone is on, do not restart: Mike's go is for an EMPTY moment. Leave the server Online, write `WAITING-PLAYERS <time>`, and retry later (whitelist may stay on with only Mike while waiting, or turn it back off; see step 8).
- Restart rule if the parent or Mike explicitly orders it with players on:
  1. `say Quick restart in 1 minute: fixing the stance lock so you can always scroll to every hotbar slot.`
  2. Wait 60 s, then `say Restarting in 10 seconds. Back in about a minute.` and count 10 s.
  3. `save-all` and wait for "Saved the game".

## 4. Shut Down
Click **Shut Down** (clock gate) and wait for Offline.

## 5. Backup, upload, readback (server offline, File Manager)
1. **Backup:** rename `plugins/ExcelAttunement-0.1.6.jar` to `plugins/ExcelAttunement-0.1.6.jar.bak-20261026-stance`. That name is not a `.jar`, so Paper will not load it. If the name exists, add `-2`. Check its size equals the live jar downloaded in step 1 and write the name in NOTES-run.md.
   - Do not touch `plugins/ExcelAttunement/` (config.yml stays as is) or the older `ExcelAttunement-0.1.5.jar.bak-20261025`.
2. **Upload** `$D/ExcelAttunement-0.1.7.jar` as `plugins/ExcelAttunement-0.1.7.jar.uploading`, then rename it to `plugins/ExcelAttunement-0.1.7.jar`.
3. **Readback:** download `plugins/ExcelAttunement-0.1.7.jar` to `$D/live/readback-ExcelAttunement-0.1.7.jar` and run `sha256sum` on it. It must be `4b5d8837eca4039e0d3f63a33fdd849a94e245718cbe769c51725c0d71665dcc` (70,788 B).
   If it does not match: delete only the bad upload, rename the .bak back to `ExcelAttunement-0.1.6.jar`, Start, and report.
4. Confirm `plugins/` now has exactly one ExcelAttunement `*.jar`: `ExcelAttunement-0.1.7.jar`.

## 6. Start
Click **Start** (clock gate) and wait for `Done (`.

## 7. Boot checks
Copy the console to `$D/logs/boot.log` and mask IPs: `sed -E -i 's/[0-9]{1,3}(\.[0-9]{1,3}){3}/<ip>/g' logs/boot.log`. Then all of these must pass:
- `[ExcelAttunement] Enabling ExcelAttunement v0.1.7`
- `Stance lock: weapon-lock ON, wand-lock ON, actionbar ON (live ON), sounds ON.` (same as the 0.1.6 boot, because the config is unchanged)
- The guild ward and arena-lock lines are the same as the last boot.
- No SEVERE and no new ERROR or WARN mentioning ExcelAttunement. The `plugins` count equals step 1.
- Console `attunement status` must show `... wand-lock ON, scroll hops 0. Stanced players: none`.
If anything fails: Rollback.

## 8. Whitelist restore
- Back to the state recorded in step 2 per the update rule. The original server state is OFF.
  - If it was ON only because Mike is still testing the cutover, ask the parent before turning it off.
- Run `whitelist off`. If `ADDED_MIKE=yes`, also run `whitelist remove <owner account>`.
- Run `whitelist list` and confirm it matches step 2. enforce stays false.

## 9. Records and in-game check owed
- NOTES-run.md: times (CDT), live sha, .bak name, readback sha, boot lines, whitelist restored.
- Then the parent records it in Big Brain, the Route Sheet and the daily log, marks the draft PR "installed live", and does the website change log entry (WEBSITE-CHANGELOG-DRAFT.md).
- Owed in game: a Java player hits a mob with a sword (Fighter stance), then scrolls past the wand to a potion. It should hop over the wand. If a Bedrock controller player is around, the bumpers behave the same.

## Rollback (one restart, same rules)
1. Player gate and Shut Down as above.
2. Rename `plugins/ExcelAttunement-0.1.7.jar` to `plugins/ExcelAttunement-0.1.7.jar.failed-20261026`.
3. Rename `plugins/ExcelAttunement-0.1.6.jar.bak-20261026-stance` back to `plugins/ExcelAttunement-0.1.6.jar` (sha 441d3f70; a copy is also in `$D/rollback/`).
4. Start. The boot must show `Enabling ExcelAttunement v0.1.6`.
- Fast off without a jar swap: in `plugins/ExcelAttunement/config.yml` set `wand-lock: false` and `weapon-lock: false`, then restart. That removes the hotbar lock completely; the cast block and zero-damage backstops stay. Only with Mike's OK.
