# INSTALL PACKET - ExcelEliteMobsAbilityBedrockBridge 0.2.0
# (swap-with-rename of ExcelAbilityBridge 0.1.1 - GATED)

Packet: ~/workspace/abilitybridge-rename-build-20261114/filing-r345/INSTALL-PACKET.md
Prepared: run 345 W6, Sat Nov 14, 2026 (filing prep only - NOT filed)
Lint: 9293 B / max line 104 / non-ASCII 0
Shape: SWAP-WITH-RENAME. This is NOT a fresh install and NOT a
same-name 1-for-1 swap. The old jar is held aside (never deleted,
never co-resident with the new jar), the new jar is placed, and
the old data folder's config is copied into the NEW data folder
while the old folder is held aside untouched.
Scope source: ~/workspace/abilitybridge-rename-20261114/SCOPING-R344.md
(risks R1-R7). Build: run 344 (BUILD-RECORD.md, same build folder).
Rig: run 345 W2 mirror rig PASS (RIG-R345.md, same build folder).

## Candidate

- Artifact: ~/workspace/abilitybridge-rename-build-20261114/ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar
- Size / SHA-256: 32,339 B /
  c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f
- Deterministic double-build byte-identical (3 builds, one hash).
- Tests: 192 distinct checks, 0 failures (incl. the EM 10.9.14
  co-change: 16 AbilityFailureReason mappings, was 10).
- Source zip: ExcelEliteMobsAbilityBedrockBridge-0.2.0-source.zip
  32,510 B / 0cab9815167a0c0cc8f234f0cd8199be6fece9f35a63a1b195445d70a295c75a
- Rig: mirror rig PASS (RIG-R345.md, 11,757 B /
  4855e3e4351ed874583da6ea8bb5fb709fc5195737fdae8fe89664235895b941):
  boot line "Enabling ExcelEliteMobsAbilityBedrockBridge v0.2.0",
  SEVERE 0 both boots, new data folder created fresh, old folder
  ignored, migrated config (debug true in new folder) reported ON,
  permission node excelelitemobsabilitybedrockbridge.admin
  verified in-jar + in live status output. Rig caveat: no
  LuckPerms-side printout obtainable via RCON (see R2 below -
  closed by Receipt 4 instead).

## Live-premise receipts (G1 - all pulled Nov 14, 2026, this prep)

Full receipts: RECEIPTS.md in this folder - 6,592 B / SHA-256
ac008957c9dbc33de2fafb5943387cba194de129c6b559d9fc7b6acf52f164c2
(pulled copies in filing-r345/pull-g1/). Lane: Homebase PC route,
probe PASS attempt 1 (~13:10 CST, DESKTOP-IL05DQR); no fallback.

- Live jar plugins/ExcelAbilityBridge-0.1.1.jar = 30,606 B /
  62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55
  (Receipt 1; exact match to the 0.1.1 record of record).
- Live folder plugins/ExcelAbilityBridge/ holds exactly one
  file, config.yml = 281 B /
  3b9a11930dd1a5511b717dbcf59621c371cc6e4eae771c0f37d7312e55ab5a08,
  value debug-log-fires: false (Receipt 2).
- Live plugins listing: exactly ONE AbilityBridge jar (the
  0.1.1 above); ZERO ExcelEliteMobsAbilityBedrockBridge jars
  or folders live (Receipt 3, script greps over the census).
- Live LuckPerms (R2): storage-method h2; grep -a -i
  "excelabilitybridge" over pulled copies of the H2 store,
  config.yml, contexts.json, and the Nov 13 export = 0 hits in
  every file; exact node excelabilitybridge.admin = 0 hits.
  NO explicit grant of the old node exists (Receipt 4).
- Guild: plugins/ExcelGuildGate/state.yml = 1,015 B /
  79e4470255edc5522db555e6ee935c301a5b74776fed45f3aa1f45220879b4c0,
  unlocked: false, 4 of 5 seals broken - guild LOCKED (Receipt 5).

## Live-jar gate (STOP on mismatch)

At the window, BEFORE any file move, re-pull and require:
plugins/ExcelAbilityBridge-0.1.1.jar = 30,606 B /
62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55
AND plugins/ExcelAbilityBridge/config.yml = 281 B /
3b9a11930dd1a5511b717dbcf59621c371cc6e4eae771c0f37d7312e55ab5a08
AND no ExcelEliteMobsAbilityBedrockBridge jar or folder present.
Any mismatch = STOP, hold the packet, report; do not improvise.

## Scope

Changes: live plugin identity ExcelAbilityBridge 0.1.1 ->
ExcelEliteMobsAbilityBedrockBridge 0.2.0 (jar, plugin name,
package, data folder name, permission node
excelabilitybridge.admin -> excelelitemobsabilitybedrockbridge.admin,
console status line). Includes the EM 10.9.14 failure-reason
co-change (16 mappings).
Does NOT change: the /abilities command + /classabilities alias,
softdepend [EliteMobs, Geyser-Spigot, floodgate], the config key
debug-log-fires or its live value (false, carried over by copy),
any player data (the plugin holds none - config only, verified
in source and by the folder census), any other plugin, or the
old data folder's contents (held aside, never deleted).

## Gates (standing discipline - not waivable)

- Marker: check WINDOW-IN-PROGRESS.txt before opening; create
  it at window start, delete at finish; re-check for a foreign
  marker immediately before Shut Down and again before Start.
- BACKUP FIRST: one fresh Shockbyte backup opens the window;
  cap-at-6 (prune oldest manual backups, name prunees; never
  delete the Daily Backup's newest copy or the two newest
  manual restore points).
- Emptiness: console `list` = 0/10 twice before the swap.

## Swap steps (window, in order)

1. Pass the live-jar gate above (fresh re-pull, hashes compared
   by script: sha256sum output diffed against this packet).
2. Hold the old jar aside: rename
   plugins/ExcelAbilityBridge-0.1.1.jar ->
   plugins/ExcelAbilityBridge-0.1.1.jar.held-20261114
   (same folder, .held suffix - not loadable, not deleted).
3. Place the candidate jar as
   plugins/ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar and
   verify its placed SHA-256 = c28f95a1... (full hash above).
4. Create plugins/ExcelEliteMobsAbilityBedrockBridge/ and copy
   the live config into it:
   plugins/ExcelAbilityBridge/config.yml ->
   plugins/ExcelEliteMobsAbilityBedrockBridge/config.yml
   then byte-edit ONLY the header comment line to
   "# ExcelEliteMobsAbilityBedrockBridge configuration."
   (the placed file then equals the candidate's bundled
   config: 297 B / a99d7fbe87fdc0f20979d38b6213ef784d357f74c45a84d912637ffa3d3a78e1,
   per RIG-R345.md Boot A). The debug-log-fires VALUE is copied
   verbatim (false at gate time); if the gate config differs
   from Receipt 2, STOP instead (gate above already enforces).
5. Leave plugins/ExcelAbilityBridge/ in place, untouched
   (rollback + audit). Its removal is a separate later cleanup,
   never this window.
6. Verify pre-boot by script: plugins listing shows the old jar
   ONLY as the .held file, the new jar present once, both data
   folders present. Old and new jars must never be co-resident
   as loadable .jar files (R3).
7. Start the server; run Boot verify.

## Boot verify (script-named checks over the boot log)

- grep -c "Enabling ExcelEliteMobsAbilityBedrockBridge v0.2.0"
  latest.log = 1
- grep -c "Enabling ExcelAbilityBridge v0.1.1" latest.log = 0
- Line present verbatim: "EliteMobs class system (ACS) is up -
  Bedrock ability bridge online: /abilities" (after the
  standing-by line)
- SEVERE count attributable to the bridge = 0 (whole-log
  SEVERE reviewed; rig baseline was 0)
- Plugin list: ExcelEliteMobsAbilityBedrockBridge (0.2.0) once;
  ExcelAbilityBridge zero times as a plugin
- Console `abilities status` first line verbatim:
  "ExcelEliteMobsAbilityBedrockBridge status: enabled (v0.2.0)"
  and its Config line shows debug-log-fires OFF (the migrated
  value); Permissions line names
  excelelitemobsabilitybedrockbridge.admin
- Console bare `abilities` ->
  "The ability bridge is for players in game."
- Guild line: "guild LOCKED <n> of 5" read from the boot log
  (4 of 5 at filing prep; re-read live, do not assume)
- Marker on install:
  ~/workspace/abilitybridge-rename-build-20261114/INSTALLED.txt

## Rollback (any boot-verify failure)

Stop the server; remove the placed new jar; rename
ExcelAbilityBridge-0.1.1.jar.held-20261114 back to
ExcelAbilityBridge-0.1.1.jar; remove the new data folder
plugins/ExcelEliteMobsAbilityBedrockBridge/ (created this
window; the old folder was never touched, so the old config
is intact); restore the window backup instead if anything
else diverged. Start; verify the 0.1.1 boot lines return and
`abilities status` shows v0.1.1. Fully reversible: the plugin
holds no state beyond the copied config (R4).

## Same-pass surfaces (owed with the install - later workers)

- NEW rename entry: What's New BOTH surfaces (website section
  + in-game ExcelWhatsNew changelog), Guide knowledge regen,
  website pair (version cell old -> new). Historical Nov 11
  0.1.1 entries stay as written (house practice).
- Broadcaster tips check: expected no-change (no AbilityBridge
  hit in any tree scanned at scoping) - still a named check.
- In-game menus check: expected no-change (menus reference the
  /abilities command at most; the command is unchanged).
- Records (post-install): new package
  excelelitemobsabilitybedrockbridge-0.2.0-<date>, G2 duplicate
  guard (pr-exists.sh, BOTH slugs) before any push, draft PR in
  wrv-plugins (stays open draft), Drive jar/source deposit.

## Proof owed after install (player-class - rig cannot discharge)

A real Bedrock player (Kailey / .coolgirlkail, the standing
Bedrock tester) opens /abilities in guild content, sees the
form with live ability names/costs, taps a slot, and the
ability fires through EM. This proof also discharges the
bridge's long-owed first real Bedrock ability fire (R7).
Java /abilities one-line F-key pointer is a secondary
spot-check in the same session.
