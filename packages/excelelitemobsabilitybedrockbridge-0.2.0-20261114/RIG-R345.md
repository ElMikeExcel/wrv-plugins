# RIG — ExcelEliteMobsAbilityBedrockBridge 0.2.0 (rename of ExcelAbilityBridge 0.1.1)

Worker: board supervisor run 345, W2 — mirror rig on the PC test-server
mirror via Homebase, 2026-11-14. Candidate under test:
`ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar` — 32,339 B — SHA-256
`c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f`
(build of record: `BUILD-RECORD.md` in this folder, run 344).
Scope: `~/workspace/abilitybridge-rename-20261114/SCOPING-R344.md` §6.2.
No live-server changes. MANIFEST / parent-backlog / board-state untouched.

## VERDICT: PASS (all rig steps; one evidence limitation + one restore caveat, both stated)

## 0. Homebase probe

- Attempt 1, 2026-11-14 12:54 CST: SSH connected but the probe command
  itself failed (Unix `hostname` syntax against the Windows shell —
  worker error, not a route failure).
- Attempt 2, ~12:55 CST: `ssh -F ~/.ssh/config mike-pc` → `PC_OK`,
  `Microsoft Windows [Version 10.0.26100.9974]`. Route UP; the full rig
  ran on it. No panel fallback used or needed.

## 1. Hash pairs (desk ↔ mirror)

| Artifact | Desk SHA-256 | Mirror SHA-256 (as placed) | Match |
|---|---|---|---|
| ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar (32,339 B) | c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f | C28F95A1…EDE74F8F (upload + placed copies both hashed on PC) | YES |
| EliteMobs-10.9.14.jar (14,563,211 B) | 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd | 4A6BC4A7…C44BD | YES |
| ExcelAbilityBridge-0.1.1.jar held aside (30,606 B) | 62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55 (record) | 62076C15…23E55 | YES |
| EliteMobs-10.9.10.jar held aside | (mirror of record) 3D796000D1D1073DAB3DFEBBDEDDF2A23B6A2E93A1A02EC8515A6CA6AFDB9954 | same | YES |

## 2. Pre-rig mirror state

- Mirror: `C:\Users\mikeexcel\wrv-testserver\server` (Paper 26.3-245,
  JDK 25.0.5), plugins tree snapshot before: **9,634 files /
  696,509,135 B** (full SHA-256 census taken, `plugins-before.txt`,
  transcribed into §7 diff).
- Present before: `ExcelAbilityBridge-0.1.1.jar` + `ExcelAbilityBridge\config.yml`
  (281 B, SHA-256 3B9A1193…, `debug-log-fires: false`), `EliteMobs-10.9.10.jar`,
  `geyser.jar`, `floodgate.jar`, LuckPerms 5.5.87. Java processes: 0.
- Staging for the rig: old bridge jar and old EM jar MOVED out of
  `plugins\` to the rig scratch folder (held, hashed above); candidate
  jar placed; EM placed as `EliteMobs-10.9.14.jar` (the ONLY EliteMobs
  jar in plugins during the test). Pre-boot check: old-name jars in
  plugins = **0**, new data folder absent, old data folder present.

## 3. Boot A — clean enable, fresh data folder (R1 first half)

Server started via WMI process create (an initial Start-Process attempt
from the SSH session died with the session — 0-byte log, no java; noted,
recovered via WMI; boot logs preserved on the PC as
`wrv-testserver\boot-r345-A.log` (868 lines) and `boot-r345-B.log`
(872 lines)).

Verbatim lines (Boot A, 2026-11-14 CST):

```
[13:00:57 INFO]: [ExcelEliteMobsAbilityBedrockBridge] Loading server plugin ExcelEliteMobsAbilityBedrockBridge v0.2.0
[13:01:04 INFO]: [ExcelEliteMobsAbilityBedrockBridge] Enabling ExcelEliteMobsAbilityBedrockBridge v0.2.0
[13:01:04 INFO]: [ExcelEliteMobsAbilityBedrockBridge] EliteMobs class system (ACS) is still starting - Bedrock ability bridge standing by; it comes online automatically when ACS is up.
[13:01:08 INFO]: Done (21.277s)! For help, type "help"
[13:01:13 INFO]: [ExcelEliteMobsAbilityBedrockBridge] EliteMobs class system (ACS) is up - Bedrock ability bridge online: /abilities
```

Counts (Boot A log): enabling-new = 1, enabling-old = 0, loading-new = 1,
**SEVERE = 0** (whole log; bridge-attributable errors/exceptions = 0),
ACS-online = 1. Plugin list shows `ExcelEliteMobsAbilityBedrockBridge
(0.2.0)` exactly once and `EliteMobs (10.9.14)`; the old plugin name
appears **zero** times as a plugin (the only old-name string in the log
is ExcelWhatsNew's historical 2026-11-11 changelog-length WARN — text,
not the plugin).

RCON from console (Boot A), verbatim:

```
>>> abilities status
ExcelEliteMobsAbilityBedrockBridge status: enabled (v0.2.0)
EliteMobs link: UP - EliteMobs 10.9.14 enabled, Advanced Combat System READY.
Geyser link: Geyser-Spigot enabled, floodgate enabled - Bedrock form transport available.
Config: debug-log-fires OFF. Permissions: exceleitemobsabilitybedrockbridge.admin (default op) for this status; /abilities itself stays open to every player.
>>> abilities
The ability bridge is for players in game.
```

Data folder after Boot A (R1): NEW folder
`plugins\ExcelEliteMobsAbilityBedrockBridge\config.yml` created by the
plugin — 297 B, SHA-256 A99D7FBE87FDC0F20979D38B6213EF784D357F74C45A84D912637FFA3D3A78E1,
byte-identical to the build tree's config.yml (SHA256SUMS), header
`# ExcelEliteMobsAbilityBedrockBridge configuration.`,
`debug-log-fires: false`. OLD folder config after Boot A: 281 B,
SHA-256 3B9A1193… — **identical to pre-rig** (untouched, not read as
the active config — status reported the new folder's default OFF).

## 4. Boot B — migrated-config behavior (R1 second half, scoping §6.2)

Boot A's server stopped cleanly via RCON `stop` (java → 0). The new
folder's config was then set `debug-log-fires: true` — exactly the
state the install packet's R1 copy step produces — while the OLD
folder's config stayed `false`. Reboot (WMI):

```
[13:03:33 INFO]: [ExcelEliteMobsAbilityBedrockBridge] Enabling ExcelEliteMobsAbilityBedrockBridge v0.2.0
```

Counts (Boot B log): enabling-new = 1, enabling-old = 0, SEVERE = 0,
ACS-online = 1. RCON (saved on PC as `rcon-b-output.txt`, transcribed):

```
>>> abilities status
ExcelEliteMobsAbilityBedrockBridge status: enabled (v0.2.0)
EliteMobs link: UP - EliteMobs 10.9.14 enabled, Advanced Combat System READY.
Geyser link: Geyser-Spigot enabled, floodgate enabled - Bedrock form transport available.
Config: debug-log-fires ON. Permissions: exceleitemobsabilitybedrockbridge.admin (default op) for this status; /abilities itself stays open to every player.
```

**debug-log-fires ON with the old folder still at false proves the
renamed plugin reads ONLY its new-name folder**: a config copied into
the new folder (the packet's migration step) takes effect, and the old
folder is ignored. R1 behavior PASS.

## 5. Permission node

The node as built (byte-exact, grep-verified in the shipped tree and
jar lineage): **`exceleitemobsabilitybedrockbridge.admin`** — identical
string in plugin.yml, in `StatusCommand.PERMISSION`, in the test pins
(24 StatusCommand checks green at build), and in the live status line
of both boots (quoted verbatim in §3/§4). It is registered by Bukkit
from plugin.yml with `default: op`, and the console status command it
gates executed successfully in both boots.
LIMITATION: LuckPerms' own replies to `lp … permission info/check`
issued over RCON came back empty (LuckPerms does not return its
formatted output to an RCON sender, and nothing was echoed to the boot
log), so there is no LuckPerms-side printout to quote. A symmetric
`lp group default permission set <new node> true` → `check` → `unset`
sequence was issued in Boot B and returned without error; the set/unset
pair leaves no intended residue. Node resolution therefore rests on
the Bukkit registration + the plugin's own status surface — recorded
as PASS with this limitation stated, not papered over.
(For the filing worker, scoping R2 stands: grep live LuckPerms data
for the OLD node `excelabilitybridge.admin` on filing day.)

## 6. Commands unchanged

`/abilities` + alias `/classabilities` are declared unchanged in the
candidate plugin.yml (build-verified); on the rig, console `abilities`
→ "The ability bridge is for players in game." and `abilities status`
→ the §3/§4 blocks, both identical in shape to 0.1.1 with only the
name/version/node strings renamed. PASS.

## 7. Mirror restore

After Boot B: RCON `stop`, java → 0. Removed the candidate jar,
`EliteMobs-10.9.14.jar`, and the new data folder; moved the held jars
back. Restore verification (hashes re-taken on the PC):

- `ExcelAbilityBridge-0.1.1.jar` = 62076C15…23E55 — byte-exact vs pre-rig. ✓
- `EliteMobs-10.9.10.jar` = 3D796000…B9954 — byte-exact. ✓
- `ExcelAbilityBridge\config.yml` = 3B9A1193… — byte-exact. ✓
- New jar present: NO. New folder present: NO. EM 10.9.14 jar present: NO. ✓
- Full plugins census after: 9,637 files / 696,601,459 B vs before
  9,634 / 696,509,135. Diff = **0 rig files**; the delta is ordinary
  mirror boot churn from the two boots, enumerated in full:
  ADDED (3): `EliteMobs\content_packages\em_craftenmines_harvester_event.yml`
  (510 B, generated by EM 10.9.14's first boot), 2 timestamped mcMMO
  backup zips. CHANGED (17): EM's own config migration under 10.9.14
  (`EliteMobs\config.yml`, `ItemSettings.yml`, `customspawns\ocean_spawn.yml`,
  `nightbreak-changelogs.yml`), LuckPerms H2 store, CoreProtect DB,
  Jobs DB + blockOwnerShips, CMILib locale, Magic spells data + .bak,
  and runtime data files of ExcelExpansions / ExcelMakersMark /
  ExcelSealAlerts / ExcelShops / ExcelTrash / ExcelWandBars.
  CAVEAT, stated plainly: these 20 churn files are NOT byte-restored —
  the rig took a hash census, not a content backup, and two server
  boots (one of them EM 10.9.14's first boot on 10.9.10-era data)
  rewrite them by design. Every file the RIG placed, moved, or created
  is restored/removed byte-exact as itemized above; the churn set is
  the mirror's normal per-boot mutation, the same class every prior
  mirror rig leaves.
- PC cleanup: the rig scratch folder
  `C:\Users\mikeexcel\wrv-testserver\rig-r345\` (scripts, uploaded jars,
  censuses, RCON output) was deleted after this report was written;
  no scheduled tasks were created (server was started via WMI);
  debug Chrome and the Nightbreak session were not touched. The two
  preserved boot logs (`boot-r345-A.log`, `boot-r345-B.log`) remain in
  `wrv-testserver\` per house rig-log practice.

## 8. Step-by-step PASS/FAIL

| # | Rig step | Result |
|---|---|---|
| 1 | Homebase probe (retry pattern) | PASS (attempt 2; attempt 1 was a worker command-syntax error, route healthy) |
| 2 | Candidate + EM 10.9.14 hash pairs desk↔mirror | PASS |
| 3 | Old-name jar NOT co-resident during test (0 in plugins; held outside, hashed) | PASS |
| 4 | Boot line `Enabling ExcelEliteMobsAbilityBedrockBridge v0.2.0` (both boots, count 1 each) | PASS |
| 5 | SEVERE attributable = 0 (whole-log SEVERE = 0, both boots) | PASS |
| 6 | ACS standing-by → online transition; EM link UP on 10.9.14, ACS READY | PASS |
| 7 | Plugin list: new name once, old name zero as a plugin | PASS |
| 8 | `abilities status` verbatim new-name first line (both boots) | PASS |
| 9 | Bare `abilities` console line unchanged | PASS |
| 10 | R1 data folder: fresh default in NEW folder; old folder untouched + ignored | PASS |
| 11 | R1 migration: config set in new folder (debug true) takes effect (status ON) while old folder stays false | PASS |
| 12 | Permission node `exceleitemobsabilitybedrockbridge.admin` resolves (Bukkit-registered, named in live status, gates the status command) | PASS with the LuckPerms-RCON limitation stated in §5 |
| 13 | Commands unchanged (`/abilities`, alias, status) | PASS |
| 14 | Mirror restore: rig files byte-exact, new artifacts removed | PASS (boot-churn caveat, §7) |
| 15 | PC scaffolding cleanup | PASS (§7) |

Still owed outside the rig (unchanged by this PASS): the player-class
Bedrock `/abilities` fire proof (scoping R7 — Kailey/.coolgirlkail),
the filing leg (G1 receipts + LuckPerms old-node grep + packet +
MANIFEST stub), surfaces, and records.
