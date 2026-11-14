# RECEIPTS - ExcelEliteMobsAbilityBedrockBridge 0.2.0 filing prep (run 345, W6)

Pull date: Sat Nov 14, 2026, 13:11-13:14 CST (America/Chicago).
Lane: Homebase PC route (rclone on the PC from shockbyte remote
"1. Michael's Server" into PC staging muse-temp\g1-r345-rename,
then scp down to this folder's pull-g1/). No fallback used.
No panel touched, no MANIFEST edit, no install.
Pulled copies retained in: pull-g1/ (this folder).

## Probe log (PC route, retry pattern)

- Attempt 1 - 2026-11-14 ~13:10 CST:
  ssh -F ~/.ssh/config -o ConnectTimeout=20 mike-pc "hostname"
  -> DESKTOP-IL05DQR, exit 0. PASS on attempt 1 of 3.
  Last successful probe: this run, ~13:10 CST.
- PC cleanup (PC cleanup rule): staging dir g1-r345-rename and
  pull-r345.ps1 / pull-lp.ps1 / pull-gg.ps1 deleted from the PC
  after the pull; verified Test-Path = False.

## Receipt 1 - Live AbilityBridge jar (swap gate premise)

- Live path: plugins/ExcelAbilityBridge-0.1.1.jar
- Pulled copy: pull-g1/ExcelAbilityBridge-0.1.1.jar
- Size: 30,606 B
- SHA-256: 62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55
- Verdict: EXACT match to the 0.1.1 record of record
  (abilitybridge-011-build BUILD-RECORD / INSTALLED.txt).
  Live plugin is ExcelAbilityBridge 0.1.1.

## Receipt 2 - Live data folder census (R1 premise)

- Live path: plugins/ExcelAbilityBridge/ (rclone lsf -R)
- Census file: pull-g1/abilitybridge-folder.txt (612 B /
  6d9d61eb5cf2f417e38da366a6cd02f971bab23877dcb783873644d5117adba7;
  the file's head is the rclone host-key NOTICE + PowerShell
  wrapper text; the listing body is one line: config.yml)
- Contents: exactly ONE file: config.yml. No other state files.
- Pulled copy: pull-g1/plugins__ExcelAbilityBridge__config.yml
- Config: 281 B / SHA-256
  3b9a11930dd1a5511b717dbcf59621c371cc6e4eae771c0f37d7312e55ab5a08
- Config value (verbatim key line): debug-log-fires: false
- Note: this hash equals the mirror rig's pre-rig old-folder
  config hash (RIG-R345.md section 2), i.e. live config is the
  untouched default. The packet's R1 copy step is still mandatory
  (it is what makes the migration explicit and auditable).

## Receipt 3 - Live plugins listing (exactly-one-jar premise)

- Census files (this pull):
  pull-g1/plugins-top.txt - 17,755 B / SHA-256
  2a45e6dfed6a606f9cc6b9bd0229e64509b7ffa41644f422f70f0116b7329a54
  pull-g1/plugins-files.txt (files-only) - 15,987 B / SHA-256
  4ab2f248175c9be93c6f77eb9fcf593c6c5e4ddfc17530fee4278ad0219ff6b9
- Script check: grep -i "abilitybridge" plugins-files.txt
  -> exactly 1 hit: ExcelAbilityBridge-0.1.1.jar
- Script check: grep -i "ExcelEliteMobsAbilityBedrockBridge"
  plugins-top.txt and plugins-files.txt -> 0 hits in both.
  No new-name jar and no new-name folder exist live.
- Boot corroboration: pull-g1/logs__latest.log (164,323 B /
  f27262562b6bec8556bee0e443d6d5c16c54284a7d77e46ee2b274f3e9948bda)
  plugin list contains "ExcelAbilityBridge (0.1.1)" exactly once
  and "EliteMobs (10.9.14)"; grep -c "Enabling ExcelAbilityBridge
  v0.1.1" = 1.

## Receipt 4 - R2 LuckPerms grep (old permission node)

Question: does any live LuckPerms record grant the OLD node
excelabilitybridge.admin (or mention excelabilitybridge at all)?
If yes, the rename to excelelitemobsabilitybedrockbridge.admin
would silently drop that grant.

- Storage identification (from the live census
  pull-g1/luckperms-lsf.txt, 3,376 B / SHA-256
  4b8a226aee1791b208295eadc96b144c581b9326912c35aecfd2c993a72ac672):
  plugins/LuckPerms/config.yml declares storage-method: h2.
  Permission data lives in ONE file:
  plugins/LuckPerms/luckperms-h2-v2.mv.db (229,376 B).
  Other data files: config.yml (37,469 B), contexts.json (53 B),
  and a console export luckperms-2026-11-13-17-38.json.gz
  (1,273 B). translations/ and libs/ are message bundles and
  library jars, not permission storage; excluded by design.
- Method: the four data files were pulled read-only via rclone
  copyto (copies, never the live files) and grepped locally with
  grep -a (binary-safe) on the copies. The live H2 database was
  NOT opened, locked, or written. A pulled copy of an H2 file is
  plain bytes; grep -a over it is a safe read-only check, so no
  window-time check is needed for this question.
- Pulled copies + hashes:
  luckperms-h2-v2.mv.db - 229,376 B / SHA-256
  8ac27cd8bb514785b59869d2d06d626642c33eaa7e40ffdd0913fce9f962dccf
  luckperms-config.yml - 37,469 B / SHA-256
  f6a86fb8fa2da4bec37d8ad2e5c995444d22e743ebf043754825541698eba5f8
  luckperms-contexts.json - 53 B / SHA-256
  b29c50a7e9a5752e6670770e105282af209ebe733aef4820f7806a3f76962914
  luckperms-backup.json.gz - 1,273 B / SHA-256
  609a12f975e0dc7a2cf506795853d132846bd3bca3e9880eb0db47768ba4c3a5
- Counts (grep -a, case-insensitive "excelabilitybridge";
  and exact "excelabilitybridge.admin"):
  luckperms-h2-v2.mv.db: 0 / 0
  luckperms-config.yml: 0 / 0
  luckperms-contexts.json: 0 / 0
  luckperms backup export (gunzipped, 9,956 B): 0 / 0
  strings over the H2 copy, grep -i "ability": 0 lines.
- Corroboration: the Nov 13 export's admin group carries the
  wildcard node "*" (value true); no per-plugin admin nodes are
  enumerated for the bridge in any store.
- R2 VERDICT: NO explicit LuckPerms grant of the old node exists
  anywhere in live LuckPerms storage. The old node is op-default
  only (plugin.yml default: op). The permission rename loses
  nothing; no grant migration step is needed in the packet.

## Receipt 5 - Guild state (boot-verify premise)

- Live path: plugins/ExcelGuildGate/state.yml
- Pulled copy: pull-g1/plugins__ExcelGuildGate__state.yml
- Size: 1,015 B / SHA-256
  79e4470255edc5522db555e6ee935c301a5b74776fed45f3aa1f45220879b4c0
  (identical to this morning's run-335 G1 pull of the same file)
- Key lines: unlocked: false; seals broken: 4 true, 1 false
  (warden). Guild LOCKED 4 of 5 at filing-prep time. The packet's
  boot verify states the standing line "guild LOCKED <n> of 5"
  to be read from the post-boot log, not pinned to 4.

## Candidate identity (desk re-verified this run)

- ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar - 32,339 B /
  SHA-256 c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f
  at ~/workspace/abilitybridge-rename-build-20261114/
  (re-hashed by this worker; matches BUILD-RECORD.md)
- ExcelEliteMobsAbilityBedrockBridge-0.2.0-source.zip - 32,510 B /
  SHA-256 0cab9815167a0c0cc8f234f0cd8199be6fece9f35a63a1b195445d70a295c75a
- Rig record: RIG-R345.md - 11,757 B / SHA-256
  4855e3e4351ed874583da6ea8bb5fb709fc5195737fdae8fe89664235895b941
  (mirror rig PASS, run 345 W2; see INSTALL-PACKET.md)
