# INSTALL: ExcelClaimFly 0.1.0 (Build Flight pass), NOT DONE

Staged Oct 25 2026, about 3:15 PM CDT. Spec approved by Mike Oct 25. Final player texts still need Mike's OK (see the report).
Nothing here is live. No panel step has been taken.

Four parts, ONE PART PER BOOT/RELOAD, in this order. Check each part before you start the next.
- Part A: the plugin jar (needs a restart).
- Part B: the Spawn Shop button (2 ExcelMenu files, `excelmenu reload`). Only after Part A is live: the button runs `buildfly buy`.
- Part C: the rotation tip (ExcelTips tips.yml, `tips reload`). It stays silent until ExcelClaimFly is running (needs-plugin).
- Part D: the What's New entry (ExcelWhatsNew changelog.yml, `whatsnew reload`).
  RECOMMENDED HOLD until Thu Oct 29 12:00 AM CDT or later. ExcelWhatsNew shows only the top entry. Before Oct 29 this
  entry would push the Bedrock port notice into "Older news" for players who have not pressed OK yet.
  Go earlier only if Mike accepts that tradeoff. The Bedrock-only port tip keeps showing either way.

## Preconditions (all parts)
1. Player gate: console `list` must read 0 players on TWO fresh readings. Recheck right before each upload. If anyone is on, hold.
   (Parts B, C and D are config-only reloads. The rules allow those with players online, with a dated backup.)
2. Live versions must match the rig: Paper 26.3, Vault 1.7.3, EssentialsX economy, LuckPerms 5.5.87,
   GriefPrevention 16.18.7, Geyser 2.12.0 + floodgate 2.2.5, ExcelMenu 0.1.5, ExcelTips 0.1.0, ExcelWhatsNew 0.1.0.
   If any differ, stop and re-test on the rig first.
3. Check every staged file against SHA256SUMS in this folder (`sha256sum -c SHA256SUMS`).
4. Drift guard (rule: compositions anchor on the LIVE file hash). Right before each upload, pull the live file read-only and hash it:
   | Live file | Must be (base the staged file was built on) |
   |---|---|
   | plugins/ExcelMenu/menus/spawnshop.yml | e0d5176f5826f896c9ff5728bf995e7d86711237dd7fe59c09c4f981ded61afd |
   | plugins/ExcelMenu/menus/spawnshop_travel_animals.yml | ac45cc8cc4872571186b81911b88ec931d261077996e516a67b0bb561d248627 |
   | plugins/ExcelTips/tips.yml | ef893938b4ba4a3e96380f981b578b4a8aea7e54fa13e1c7c70c22897b83a5d7 (= the Bedrock port warning staged file) |
   | plugins/ExcelWhatsNew/changelog.yml | c4968b0b0968a706a8b443d5e0bba944eadd62475a458d1ff19819a726a8ae8a (= the Bedrock port warning staged file) |
   tips.yml and changelog.yml are composed ON TOP of the Bedrock port warning packet. If that packet is not live yet,
   or anything else changed these files (TW 0.1.7 also stages tips.yml), the hash will differ.
   If it differs: do NOT upload the staged file. Re-apply the same diff (menus.diff, tips.yml.diff, changelog.yml.diff)
   onto the LIVE file on the box, re-test on the rig, and re-hash.
5. Part A only: confirm there is no ExcelClaimFly jar or plugins/ExcelClaimFly folder on the server yet.

## Backups (dated .bak; never overwrite an existing .bak, add -2 if the name exists)
- Part A: nothing is replaced (new jar). Take the usual panel backup first. Keep at most 2 panel backups: replace the manual one.
- Part B: plugins/ExcelMenu/menus/spawnshop.yml.bak-20261025-buildflight
          plugins/ExcelMenu/menus/spawnshop_travel_animals.yml.bak-20261025-buildflight
- Part C: plugins/ExcelTips/tips.yml.bak-20261025-buildflight
- Part D: plugins/ExcelWhatsNew/changelog.yml.bak-20261025-buildflight

## Upload targets (upload under a temp name, rename into place, then check the sha256 or the exact size)
| Staged file | Live path | sha256 |
|---|---|---|
| ExcelClaimFly-0.1.0.jar | plugins/ExcelClaimFly-0.1.0.jar | a5e6de66a6bb0f981ab4f6f1e4fc5a02391c2306cd78085744d3daf7a838556b |
| staged/ExcelMenu/menus/spawnshop.yml | plugins/ExcelMenu/menus/spawnshop.yml | 9625a81fe84cbbeb670aae8dd4f970d7b8592d850f5c6c140a51fb0735b9165e |
| staged/ExcelMenu/menus/spawnshop_travel_animals.yml | plugins/ExcelMenu/menus/spawnshop_travel_animals.yml | fe8da4c32357d6ff50397e9e440c63a3da037f595b7e6824b657f7c755cf7bd9 |
| staged/ExcelTips/tips.yml | plugins/ExcelTips/tips.yml | f69e72832f1c65e4688d770bf4a443948db38803b325bc3a7e30d7c0802baeb6 |
| staged/ExcelWhatsNew/changelog.yml | plugins/ExcelWhatsNew/changelog.yml | e2124322de151ae3ef7a147e10440e0290872b8319c93e7ea99284ea7188bed9 |
No config.yml upload: the jar writes its default config.yml (the approved texts) on first start.

## Part A: jar (restart)
1. `list` -> 0 players (two reads). Write down `guildgate status`.
2. Upload the jar, check the sha.
3. Panel Shut Down (never console `stop`), then Start. Or let the nightly restart pick it up.
4. Boot must show:
   - `[ExcelClaimFly] ExcelClaimFly enabled: price $1,000, 30 min pass, cap 120 min, combat pause ON, owner only, Bedrock forms on.`
   - No ERROR lines from ExcelClaimFly.
5. Console `buildfly info <any online admin>` is optional. `buildfly reload` must answer with the same summary.
6. `guildgate status` -> same as before. Shut Down again only if the next part needs a boot (B, C and D do not).

## Part B: Spawn Shop button (`excelmenu reload`)
1. Drift guard (both menu hashes above), backups, upload, check the shas.
2. `excelmenu reload` -> the self-check must list BOTH `spawnshop` and `spawnshop_travel_animals` BY NAME (a count proves nothing).
3. Open /shop -> Travel & Animals. A feather "Build Flight Pass" sits in slot 5, balance in 6, Back in 7.
   Tapping it opens the review screen with no charge. Do NOT tap Buy on the live server (that spends real money).

## Part C: tip (`tips reload`)
1. Drift guard, backup, upload, check the sha.
2. `tips reload` -> "ExcelTips reloaded: 24 tips." (23 before; the count depends on the live base).
3. `tips check` -> no warning line for build-flight.

## Part D: What's New (`whatsnew reload`); see the HOLD note above
1. If installing after Oct 25, change `date:` and `posted:` of the 2026-10-25-build-flight entry to the install day (keep the id), re-hash, and update SHA256SUMS.
   Then drift guard, backup, upload, check the sha.
2. `whatsnew reload` -> "ExcelWhatsNew reloaded: 4 entries, newest 2026-10-25-build-flight, no warnings."
3. `whatsnew check` -> no warnings.

Rig proof of the same sequence: RIG-TEST-RESULTS.md (Java 53/0, Bedrock 17/0, tip + What's New 12/0, unit 35/0).

## Records (same pass, read each one back)
- Big Brain (append only, local plus Drive, byte-verified): what changed, hashes before/after, boot and reload outputs, rollback names.
  Mark as UNCONFIRMED: real Bedrock device and touch, Bedrock no-fall-damage landing (the fall damage check ran with Java bots only), live economy.
- Route Sheet: update the Build Flight row by RAW range update, then read it back.
- Daily log line. Version/rollback map: ExcelClaimFly 0.1.0 new, plus the .bak names above.
- GitHub: wrv-plugins draft PR (ExcelClaimFly/ folder). Keep it a draft.
- Website: draft PR on the content/build-flight-pass-staged branch. Before merge, replace both `{{INSTALL_WHEN}}` tokens with the real install date.
  Publish only when the pass is live. Then regenerate the Guide knowledge from the LIVE ai-knowledge.json (knowledge-only update, no restart).

## Never
No raw server IP, passwords or tokens in any file. No player names, gamertags or UUIDs in any record, README or public text.
No test purchases on the live server. Do not kick anyone.
