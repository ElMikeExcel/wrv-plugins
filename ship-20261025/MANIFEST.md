# SHIP-20261025 — combined package for Wink Reyes Valley (Paper 26.3-214)

Built Sat Oct 24, 2026 (CDT). Staged only: nothing here has been installed or pushed.
Magic means **Magic (elmakers) 11.2.5**. **EliteMobs (MagmaGuy) is not touched** by any step.

## What ships
| # | Piece | Replaces on live |
|---|---|---|
| 1 | ExcelWandBars 0.2.0, plus the classic wands (part B, only if the overlay is live) | ExcelWandBars 0.1.0 if present (0.2.0 supersedes 0.1.0) |
| 2 | Magic spells.yml: Boon active_costs 1 and missile 4/5/5/6/6 (**default for today**) | live Magic + stage1-5 spells.yml |
| 2L | Lightning spells.yml slot (stages 3-5). **Final step, only after Mike decides** | the spells from row 2 |
| 3 | ExcelTrainingWheels 0.1.4 plus a lifesteal config merge (debug stays true) | 0.1.3 |
| 4 | ExcelAttunement 0.1.6 (sha 441d3f70…) plus an arena-locks config merge | 0.1.5 |
| 5 | ExcelBossTrial 0.2.7 plus its staged config (Iris: SHIP, PR #60) | 0.2.6 plus config 591a8a02… |
| 6 | ExcelBedrockMenu 1.5.17 | 1.5.16 |
| 7 | ExcelMenuJoin 1.4.2 | 1.4.0 or 1.4.1 (confirm which one) |
| 8 | ExcelLeaderboard 1.3.1 | 1.3.0 |
| 9 | ExcelRaidBuff 0.1.2-scrubbed (same version number; only comments changed) | 0.1.2 |
| 10 | /time fix: Essentials disabled-commands `time`, plus /day and /night aliases in commands.yml | merges into two live files |

**Not in this package:**
- ExcelGuide. The 0.2.3-scrubbed jar bundles a config.yml and knowledge.yml that differ from the original, so it is not behaviour-identical.
- A Guide knowledge refresh. Live already runs generation 3862146f (153 chunks, includes First Breath). Check it with `guide status` (step 13).
- The ExcelRaidBuff config comment. Optional hand-edit; the values are unchanged.
- Anything EliteMobs.

## Files (sha256)
Target paths are relative to the server root. "Replaces" names the live file, which is backed up first (see Backups).

| package file | target path | sha256 | replaces |
|---|---|---|---|
| files/ExcelWandBars-0.2.0.jar | plugins/ExcelWandBars-0.2.0.jar | 3bac9054323330dfbcb5c84ed4488121f463a03d41eda5bbb336fbaa872b70a1 | plugins/ExcelWandBars-0.1.0.jar (if present) |
| files/ExcelTrainingWheels-0.1.4.jar | plugins/ExcelTrainingWheels-0.1.4.jar | 4d9ca503546ea7ad69843f6b6e376de18ef824e3a8062b4933dfe4292f48bfda | plugins/ExcelTrainingWheels-0.1.3.jar |
| files/ExcelAttunement-0.1.6.jar | plugins/ExcelAttunement-0.1.6.jar | 441d3f707283c28f293dbce5802e94236c2b856efbe24a8e9d36c6c053e3cc2b | plugins/ExcelAttunement-0.1.5.jar (sha 69d4afc5…) |
| files/ExcelBossTrial-0.2.7.jar | plugins/ExcelBossTrial-0.2.7.jar | 7b0392d5652b34fd066eb223cdb3b4761152cb0abfef21801cb014c07d5ce4b5 | plugins/ExcelBossTrial-0.2.6.jar |
| files/ExcelBossTrial/config.yml | plugins/ExcelBossTrial/config.yml | 6a1dc9812a834e6cb59b5bee5462e20cb3c5287cb28731cad06810f203ce1ac4 | plugins/ExcelBossTrial/config.yml (expected 591a8a0258a52b9c62bd7eeb4041047a3fddf1d2eb447d0aac9707ca28f178cf) |
| files/ExcelBedrockMenu-1.5.17.jar | plugins/ExcelBedrockMenu-1.5.17.jar | 47efb1e9f9315ecf1bf4208040ab62c62680b0d06aae6991fd57388177cc7154 | plugins/ExcelBedrockMenu-1.5.16.jar |
| files/ExcelMenuJoin-1.4.2.jar | plugins/ExcelMenuJoin-1.4.2.jar | 877bf1956ed1c4178b7d390df4719eb9f326305b640ad8656bc5c1637229d976 | plugins/ExcelMenuJoin-1.4.x.jar |
| files/ExcelLeaderboard-1.3.1.jar | plugins/ExcelLeaderboard-1.3.1.jar | 3b2c1f1c11faf9e1bdadd77fc25e5f9c905f3eadb2a472d01da6919e978060ee | plugins/ExcelLeaderboard-1.3.0.jar |
| files/ExcelRaidBuff-0.1.2-scrubbed.jar | plugins/ExcelRaidBuff-0.1.2.jar | 1cee2856fb11fe4c614b5c54ea4aae2da5ba13de87dbe47239766bc818c70de3 | plugins/ExcelRaidBuff-0.1.2.jar |
| files/partB-classic-wands/plugins/Magic/wands.yml | plugins/Magic/wands.yml | 1a9accf26f66fe893e08ec5414bce443042d26c3ea6ccea9e60f4bf877f83de0 | same path (only with part B) |
| files/partB-classic-wands/plugins/ExcelMagicGate/stages/stage1/wands.yml | plugins/ExcelMagicGate/stages/stage1/wands.yml | 1a9accf26f66fe893e08ec5414bce443042d26c3ea6ccea9e60f4bf877f83de0 | same path (only with part B) |
| files/partB-classic-wands/plugins/ExcelMagicGate/stages/stage2/wands.yml | plugins/ExcelMagicGate/stages/stage2/wands.yml | 3dc1bf99ce9781614dfb83e6c71200e0a3b195e44fb1d872b254f9b79b54206c | same path (only with part B) |
| files/partB-classic-wands/plugins/ExcelMagicGate/stages/stage3/wands.yml | plugins/ExcelMagicGate/stages/stage3/wands.yml | a8afb0d8d2a5a0450851f5197067db3eabccd050930912b710b0c37c3ec157e2 | same path (only with part B) |
| files/partB-classic-wands/plugins/ExcelMagicGate/stages/stage4/wands.yml | plugins/ExcelMagicGate/stages/stage4/wands.yml | e5165a7e08e53099fb108d7676fa64f85f4a37ac716ab33de2bf310831176fe1 | same path (only with part B) |
| files/partB-classic-wands/plugins/ExcelMagicGate/stages/stage5/wands.yml | plugins/ExcelMagicGate/stages/stage5/wands.yml | fb5a8896a4fa49ef22a0cd519887b1100527f928066358e561614e5ff6b9d33e | same path (only with part B) |
| files/spells-boon-missile/plugins/Magic/spells.yml | plugins/Magic/spells.yml | c8721a1ff7d89f6f86719898310ef024d1d15995d91e61640429f34c098447a1 | same path |
| files/spells-boon-missile/plugins/ExcelMagicGate/stages/stage1/spells.yml | plugins/ExcelMagicGate/stages/stage1/spells.yml | c8721a1ff7d89f6f86719898310ef024d1d15995d91e61640429f34c098447a1 | same path |
| files/spells-boon-missile/plugins/ExcelMagicGate/stages/stage2/spells.yml | plugins/ExcelMagicGate/stages/stage2/spells.yml | 3205227a680c46af3e0a2aa3a7ac08e5699ba7697418c5dea207ccea25b07b00 | same path |
| files/spells-boon-missile/plugins/ExcelMagicGate/stages/stage3/spells.yml | plugins/ExcelMagicGate/stages/stage3/spells.yml | efa4f8eb19b192bf47aea467b5a212b43146db296c89b55b6d03d0c5d304d2e1 | same path |
| files/spells-boon-missile/plugins/ExcelMagicGate/stages/stage4/spells.yml | plugins/ExcelMagicGate/stages/stage4/spells.yml | b3a5d87fe5b5eeeccbb597fa2b441bfd50d5cdb5252eec0b4b004754727c1c42 | same path |
| files/spells-boon-missile/plugins/ExcelMagicGate/stages/stage5/spells.yml | plugins/ExcelMagicGate/stages/stage5/spells.yml | f22a9e0b5db95c9a9bd631328a0520a935e4e12b1596da6ee1587cc0ef26c14f | same path |
| lightning-slot/plugins/ExcelMagicGate/stages/stage3/spells.yml | plugins/ExcelMagicGate/stages/stage3/spells.yml | 7d43579c269b781ecc0356ebf534914fede33e2f847937d48ee42fd66d659c64 | row-2 file (final step only) |
| lightning-slot/plugins/ExcelMagicGate/stages/stage4/spells.yml | plugins/ExcelMagicGate/stages/stage4/spells.yml | e9e635d2a8bbdc11624b8ec69685932449d51924ab243463a5331bc01f333cc7 | row-2 file (final step only) |
| lightning-slot/plugins/ExcelMagicGate/stages/stage5/spells.yml | plugins/ExcelMagicGate/stages/stage5/spells.yml | 46cc3b14881035b86529ff2077103dbcad258ed03a59b3e5dc7cd3e0d6308f13 | row-2 file (final step only) |
| merge/ExcelAttunement-config-merge-arena-locks.yml | append to plugins/ExcelAttunement/config.yml | c54ff0a1cbfea6f766e6ae22ee2dfd193550f0c03b5105222cba14d80b3a3560 | merge |
| merge/ExcelTrainingWheels-config-merge-lifesteal.yml | append to plugins/ExcelTrainingWheels/config.yml | 523b8f16d203721b50135bcde4d4083e121f0624b4f9f6b8752a51e84c6eafcb | merge |
| merge/commands.yml-merge-time-aliases.yml | append under `aliases:` in commands.yml (server root) | bda7a64b81ddd97986663c0630836394b2d92b7b774ba4ba3de934576b17100c | merge |
| merge/Essentials-config-merge-disabled-commands.txt | instructions: one list item in plugins/Essentials/config.yml | 1073b8ceb508803468d88580bb37a31c905d16369c69d27daa21acffea3d4be9 | merge |

lightning-slot stage1, stage2 and Magic spells.yml are byte-identical to the row-2 files (c8721a1f… / 3205227a…).

The records/ folder holds the per-plugin install records. LIGHTNING-FIX.snapshot.md (958bec3d…) is the Lightning record as of 10:13 AM CDT. Full list: SHA256SUMS.

## Pre-checks (hash on the panel before touching anything; STOP on any mismatch)
**Magic wands decision table** (live `plugins/Magic/wands.yml`):

| live Magic/wands.yml | meaning | do |
|---|---|---|
| 1a9accf26f66fe893e08ec5414bce443042d26c3ea6ccea9e60f4bf877f83de0 | classic / noab live | jar only. **Skip part B** (the stage files already equal part B) |
| e97406d0198f2f65515a1ea112bbc9b7880f394b33f2e1f0e1e62e903ac32e9a | overlay live | jar **plus part B** (all six wands.yml). Without it, chest mode hits the cycleSpells NPE |
| anything else | unknown | **STOP**, no Magic files, re-diff |

The stage wands must match the same column:

| stage wands.yml | classic (= part B) | overlay |
|---|---|---|
| stage1 | 1a9accf2… | e97406d0… |
| stage2 | 3dc1bf99… | 5346d442… |
| stage3 | a8afb0d8… | 5bf3d4bb… |
| stage4 | e5165a7e… | c14f2483… |
| stage5 | fb5a8896… | 8bc82e75… |

stage0/wands.yml and the config.yml files are never changed.

**Spells table**: live spells.yml must be one of these columns, or STOP.

| spells.yml | noab | overlay |
|---|---|---|
| Magic + stage1 | 0596731ac867753be377ffd297108d043e695d20796f8e082e81145c2fa532ae | 4d79245102577f3dbfdc97a5db3dd8972b2e7c372060a7ce8b5095ac7763882a |
| stage2 | 39aefb0cef302996c54168e015d9823ced00d0d17c4d7a705c7c3b1b2b3e5a83 | 150dad2438980a7bdc7bf3255b8fa8d2597ee914203a50bbbf47ef40740d5bac |
| stage3 | f81d3fce3233ffb6d6f165d0cb2f85d8dd561dc41c8da6865aa6ca6a693828b4 | 301d96f441d790e48fbbd9740a9e356a5cc818d5188523b2968db7064b5748e9 |
| stage4 | 3c566d595bc9f9bcdcf85e5252d123ab88a856bfe9303142c704e57094713b93 | 782d2b6db8db78bd39447ce518e03cb5536b1638d512778dc2ff11f1ee027d7f |
| stage5 | d3806425d8825bbfd148d7f4498776139c15811fec0860eccbcc5e03ad98d654 | 4d8797318b5bccc2d34ed432d19f1be114877733ec30698cb1b61f67a3808cf4 |

If noab is live: Magic/icons.yml must be 541f284c56eae18bdaa40e2f45c894b108368d341019a0d4c184932a45bbf5f6. It holds the wrv_* icons the new spells use. Against noab, the new spells change only Boon mana 2→1, the missile lines and four icons (cure/extinguish/gills → wrv_*).

**Other pre-checks:**
- Attunement live jar sha 69d4afc53a0b7687e65a699266833d41961cf44ec199cf40e57a5c2d1c57d6b8.
- BossTrial live config 591a8a02… Also hash the live 0.2.6 jar and record it; the audit never hashed it.
- Note which MenuJoin 1.4.x is live.
- Take one `/bosstrial status` capture to compare in step 13.

## Gates
- **0 players online**: Iris's gate for BossTrial 0.2.7 and the Attunement record. Iris's other gate: no open boss fight (`/bosstrial status`: `Open encounters: 0`, dragon phase DONE/IDLE/NONE). Follow records/ExcelBossTrial-0.2.7-INSTALL.md.
- If players can't be cleared today: install everything **except** row 5, and do BossTrial at the next empty restart (its jar and config are self-contained).
- **Restart rule**, even when empty:
  1. Say the reason in chat (e.g. `say Server restart in 1 minute to add wand, lifesteal and arena updates`).
  2. Wait 1 minute.
  3. Count down 10 seconds (`say 10` … `say 1`).
  4. Stop. Full stop/start only: no /reload, no plugin managers.
- After any Magic reload, Bedrock players rejoin.

## Backups (copy before replacing; jars are moved OUT of plugins/ so they do not load)
- Suffix `.bak-20261025`. **Never overwrite an existing .bak.** If the name exists, use `.bak-20261025-2`, then `-3`, and so on. List the folder first.
- Jars go to `backups/<jar>.bak-20261025` (outside plugins/): WandBars 0.1.0 (if present), TW 0.1.3, Attunement 0.1.5, BossTrial 0.2.6, BedrockMenu 1.5.16, MenuJoin 1.4.x, Leaderboard 1.3.0, RaidBuff 0.1.2.
- Copied in place (`<file>.bak-20261025`):
  - plugins/ExcelWandBars/config.yml (if present)
  - plugins/ExcelTrainingWheels/config.yml. Leave the existing `config.yml.bak-20261024-debug` alone.
  - plugins/ExcelAttunement/config.yml
  - plugins/ExcelBossTrial/config.yml and state.yml, plus the dragon/ folder → dragon.bak-20261025/
  - plugins/Magic/spells.yml, plus wands.yml if part B
  - plugins/ExcelMagicGate/stages/stage1-5/spells.yml, plus wands.yml if part B
  - commands.yml
  - plugins/Essentials/config.yml
  - plugins/ExcelRaidBuff/config.yml (safety copy; it is not changed)

## Install steps (server stopped between steps 3 and 11)
1. Pre-checks above (wands/spells decision, jar hashes, BossTrial status capture). Decide on part B.
2. Gate check: 0 players, Open encounters: 0. Then the restart rule (reason → 1 min → 10 s).
3. Stop the server (full stop).
4. Backups (list above).
5. Jars:
   - Remove the old jars from plugins/ (they are now in backups/).
   - Upload the 9 jars from files/. Rename ExcelRaidBuff-0.1.2-scrubbed.jar to ExcelRaidBuff-0.1.2.jar.
   - Re-hash each one on the panel.
   - Each plugin must have exactly one jar.
6. Magic files:
   - Upload the six spells.yml files from files/spells-boon-missile/plugins/ to the same paths.
   - Part B only if the overlay is live: also upload the six wands.yml files from files/partB-classic-wands/plugins/.
   - Re-hash.
7. Attunement: append merge/ExcelAttunement-config-merge-arena-locks.yml to the end of plugins/ExcelAttunement/config.yml. Change no other key. Remove a stray `seal-locks:` section if one is present.
8. TW:
   - Append merge/ExcelTrainingWheels-config-merge-lifesteal.yml at top level of plugins/ExcelTrainingWheels/config.yml.
   - Keep `debug: true` and the protected list. Do NOT replace the file.
9. BossTrial: copy files/ExcelBossTrial/config.yml over plugins/ExcelBossTrial/config.yml.
   - This is a full-file copy, but it is effectively a merge. Checked: every live key and value is kept. It only adds keys that make the running defaults explicit (wither fought-free 4.0/0.25, warden range band 14 and give-up 45 s, dragon share gate, First Breath elytra signature) and fixes comments.
   - It keeps the live "her seal" line (Iris).
10. /time fix:
    - commands.yml: insert merge/commands.yml-merge-time-aliases.yml under the existing `aliases:` key, which is the last key. Appending at end of file is right if `aliases:` is last.
    - plugins/Essentials/config.yml: add `  - time` under `disabled-commands:` (see the merge .txt).
    - YAML-check both files: the panel's editor, or a viewer that reports parse errors.
11. Start the server.
12. Boot-log checks (table below). 0 SEVERE. No new "Exception".
13. In-game/console checks:
    - `/attunement status`: Arena locks ON; world_the_end dragon ALIVE (zone locked) or NONE (zone open).
    - `/bosstrial status`: must match the pre-install capture except
      (a) the version line,
      (b) the dragon-chapter `world_the_end: phase=…` line (in-memory phase, resets to NONE on every restart),
      (c) the "Announcements … until Sat Oct 24 11:50:41 PM CDT" line (Iris: not drift).
      Pools must read: wither 1500+300, elder 700+75, warden 1300+200, dragon 400/900, share gate on.
    - `/wandbars` → `ExcelWandBars 0.2.0 … wand style ON default classic`; `/wandbars style <a player>` → `classic (default) … mode=chest … matches=true`.
    - `/tw list` still shows the protected list.
    - `time set day` from console → `Set minecraft:overworld to time marker minecraft:day`; `/day` and `/night` work for an op.
    - `guide status` → generation 3862146f (153 chunks). No refresh.
    - Compass air tap opens the menu (Java). On a real Bedrock device: compass and `/menu` dialog.
    - `/leaderboard` in game shows the boards.
14. Watch the first live wither/warden fight on BossTrial 0.2.7 (Iris note 3). Tell Bedrock players to rejoin if Magic was reloaded.
15. **Lightning final step** (later, separate restart; only when both are true):
    (a) /workspace/wandbars/LIGHTNING-FIX.md still shows Check 1 PASS and Check 2 PASS, with the same hashes as lightning-slot/.
    (b) Mike has answered its "Flags for Mike". Flag 1: Lightning now does 3.5 per bolt to the ender dragon (was 0). To keep it at 0, add `ignore_types: [ender_dragon]`, which changes the files and needs a new rig run.
    Then:
    1. Back up the three stage3-5 spells.yml (`.bak-20261025-L`, never overwrite).
    2. Copy lightning-slot/plugins/ExcelMagicGate/stages/stage3-5/spells.yml in and re-hash.
    3. If the live stage is 3 or higher, also copy the matching stage file to plugins/Magic/spells.yml.
    4. Re-run the combined rig with lightning-slot before the live copy: `ctl.sh pkg_magic lightning-slot` + smoke.js. This was already run today: 26/26 PASS.
    5. Restart per the rule, or `magic load` at a seal. Bedrock players rejoin.

## Boot-log checks per plugin (rig output, same lines expected live)
| plugin | line |
|---|---|
| ExcelWandBars | `Enabling ExcelWandBars v0.2.0` · `Sneak bar flip ON (debounce 250 ms), spell book ON, skip worlds [...]` · `Wand style switch ON (default classic, 0 player choice(s) saved)` |
| ExcelTrainingWheels | `Enabling ExcelTrainingWheels v0.1.4` · `lifesteal on kill: ON (hostile +2.0 HP, elite +4.0 HP, spawner mobs excluded, action bar ON)` |
| ExcelAttunement | `Enabling ExcelAttunement v0.1.6` · `Guild ward (Magic off in the EliteMobs worlds): ON.` · `Arena locks: ON (sealed near a live dragon: glide (stopped on entry), fling, hulkleap, lift (+ targets in zone), blink; zone radius 200 around 0,0 in THE_END; unknown dragon state = locked).` A `[default rules]` tag means step 7 was missed |
| ExcelBossTrial | `Enabling ExcelBossTrial v0.2.7` · `ExcelBossTrial 0.2.7 awake: ENABLED (share gate 70% of max HP, presence 128 blocks, cooldown 6h 0m)` · no WARN about unknown config keys |
| ExcelBedrockMenu | `Enabling ExcelBedrockMenu v1.5.17` · `ExcelBedrockMenu enabled: /menu uses native Bedrock dialogs.` |
| ExcelMenuJoin | `Enabling ExcelMenuJoin v1.4.2` · `ExcelMenuJoin enabled.` |
| ExcelLeaderboard | `Enabling ExcelLeaderboard v1.3.1` · `offline stats cache reads <world>/players/stats` · `first board refresh in 3 seconds` |
| ExcelRaidBuff | `Enabling ExcelRaidBuff v0.1.2` · `ExcelRaidBuff 0.1.2 awake: ENABLED (...)`. Live has EliteMobs, so `EM seam` should read bound, not UNAVAILABLE as on the rig |
| Magic | `Loaded N spells` at the live stage: stage1 43 · stage2 81 · stage3 142 · stage4 157 · stage5 203 |
| Essentials | no new errors. `/ess commands` unchanged apart from time |

## Rollback (stop → restore → start; the restart rule applies)
- Any jar: delete the new jar from plugins/ and move `backups/<old>.bak-20261025` back to its original name in plugins/.
  - BossTrial: also restore config.yml (state.yml from 0.2.7 is readable by 0.2.6).
  - Attunement: 0.1.5 ignores `arena-locks`.
  - TW: 0.1.3 ignores `lifesteal`.
  - Quick TW off without a restart: `lifesteal.enabled: false` + `/tw reload`.
- Magic: copy each spells.yml (and part-B wands.yml) `.bak-20261025` back to Magic/ and stage1-5. Then restart or `magic load`; Bedrock players rejoin. One player's wand style: `/wandbars style <player> clear`.
- /time fix: restore commands.yml and Essentials/config.yml from `.bak-20261025`, or delete the two aliases and the `- time` line, then restart.
- Lightning: copy the three `.bak-20261025-L` stage files back.

## Verification done on the box (CDT)
**Rig** /workspace/ship1025-rig mirrors the live plugin set minus EliteMobs and the plugins not available on the box. Settings: Paper 26.3-214, offline test bots, fresh End with a live dragon.

**Rig-only differences:**
- ExcelBuildMode was removed for the bot runs. Without its private storage it fails closed and kicks new joiners. It boots the same with and without the package.
- RigWorlds-test.jar is a test helper.
- connection-throttle is off.

**Runs:**
- Baseline (old jars, noab Magic): Done 19.8 s. 22 ERROR lines, all rig-only (Essentials version notice, BuildMode private storage, ExcelMenu spawnshop menus missing). 0 SEVERE.
- Package boots: 0 SEVERE, 0 plugin failures. No ERROR/WARN line that isn't already in the baseline.
- Combined smoke (tests at /workspace/ship1025-tests, smoke.js):
  - lightning-slot + BossTrial 0.2.7: **26/26 PASS** (smoke-final-lightning.out). Covers:
    - TW lifesteal +2 HP with the debug line
    - wand defaults to classic at stages 1 and 5
    - stage counts 43/81/142/157/203, with wands/spells hashes = package at each stage
    - glide/blink/fling blocked in the End zone at no mana cost; blink works in the overworld
    - bosstrial status
    - compass air tap opens the menu
    - /leaderboard
    - 0 SEVERE/NPE
  - Boon/missile spells (today's default): 25/26 PASS. The one fail was TW, caused by rig mobs (Essentials `kill` intercepted the mob clear). The TW-only re-run on the same boot passed (12→14, LIFESTEAL line).
  - Earlier full run on BossTrial 0.2.6: 25/26; TW passed in its follow-up.
- BossTrial status 0.2.6 vs 0.2.7: identical except the version line and the in-memory dragon phase line. That line starts as NONE in 0.2.6 source too.
- /time fix, two separate boots, the same result both times:
  - Console `time set day`, `day`, `night`, `time set 6000` → vanilla "Set minecraft:overworld …".
  - Op player `/time set night`, `/day`, `/night` OK. `/etime` still shows the Essentials readout.
  - Non-op `/day` does nothing (no message); `/time set` → unknown command.
  - Bare `/time` → vanilla usage error, no longer the Essentials readout (use `/etime`).
  - Rejected option: a `time:` alias in commands.yml. It broke bare `/time` and logged a stack trace on a typo.
