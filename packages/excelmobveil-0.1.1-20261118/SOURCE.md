# SOURCE - ExcelMobVeil 0.1.1

ExcelMobVeil 0.1.1 - ExcelItems migration M3 (registry ID 1, veil potion). The pattern-proof piece: first plugin to hard-depend on ExcelItems and mint/identify through the registry.
plugin.yml version 0.1.1, depend: [Vault, ExcelItems] (softdepend unchanged); createPotion mint fully delegated to ExcelItemsAPI.create(1) - dual-stamp primary excelitems:id + legacy excelmobveil:potion rides along; drink/veil-time paths identify via the API (primary-first, legacy fallback) so registry-minted and legacy-held veil potions are the same item and stack time identically; behavior excelmobveil:veil-drink registered for ID 1. Veil timing/stacking logic (eight real-time minutes per drink; ends on attack/milk/death) unchanged. The mobveil buy console command stays the delivery command. No player-facing behavior change.
Build ID: (none - version 0.1.1 carried in plugin.yml, pom.xml and the enable log line).
Tests: 195/195 passed, incl. the M3 stacking/time gate (registry-minted and legacy-held potions stack identically); double-build byte-identical (deterministic pack, fixed entry order + fixed DOS timestamp).
Build folder of record: ~/workspace/board-r559/m3-mobveil/ (board supervisor run 559, M3 build worker).
Source tree of record: work/migrated/ in that folder; this package's source tree is the content of the build's source zip of record.
Sources zip: ExcelMobVeil-0.1.1-sources.zip - 64,529 B /
SHA-256 480749132098aaf551abc2bf7992eb7d7a66f6404083d8c4904fe2e821ee26a9 /
MD5 f5eeb6f01c66a5373e2743f076656c79 (28 files; deposited bytes are the
build folder's ExcelMobVeil-0.1.1-source.zip, renamed to the Drive
-sources.zip convention - identical bytes, identical hashes).
Built jar (NOT inside the git records package, per house
rule): ExcelMobVeil-0.1.1.jar - 61,819 B /
SHA-256 42ffa15514513366e973a4cefdd106a108bd31004ed166cbbbac3fca1863560d /
MD5 47e56e677228ab7b8e3587e944dffe92. The Drive deposit
carries it; it is referenced here by hash only.
Config: Live config pulled at G-1 (build folder live-pull/config.yml): SHA-256 fe5ceea01561f3e9e929dfdbd956346abf76e43cd358b4c91d87379b7e2ec331. Installs never replace a live config.
Install: INSTALLED Nov 18, 2026, 11:30 PM CST by wrv-install-failsafe-v2 run 30dd1528-d957-4c98-bfe9-397579735dfe (Homebase lane), chained Install 2 of 3 in the M2+M3+M4 window - see INSTALLED.txt and INSTALL-PACKET.md in this package. Backup wrv-migration-m2m4-20261118; placed bytes pull-back EXACT; boot Done (24.148s), enable line x1 ($2,000.00, eight real-time minutes), SEVERE 0; RCON excelitems status = 4 registered / next free 5.
Status: Installed (D122). Surfaces: none owed (no player-facing change; packet G-6 check). Post-release proofs owed (not gates): veil buy/drink/stack - via the board's Needs Testing.

## Drive deposit (Minecraft WRV)

Duplicate checks first (Nov 19, 2026): Drive name searches
for the 0.1.1 artifacts returned no existing deposits
for this version before upload; wrv-plugins duplicate
guard (G2) also NO MATCH for this package.
- ExcelMobVeil-0.1.1.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  1X4oIBH5HTTlqPWAs1EHidOKcWLS-dy7K, size 61,819,
  MD5 47e56e677228ab7b8e3587e944dffe92 - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1X4oIBH5HTTlqPWAs1EHidOKcWLS-dy7K/view?usp=drivesdk
- ExcelMobVeil-0.1.1-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  1M2n-VCE4s8YJRq6dH-Q11t9O3oUR5uwM, size 64,529,
  MD5 f5eeb6f01c66a5373e2743f076656c79 - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1M2n-VCE4s8YJRq6dH-Q11t9O3oUR5uwM/view?usp=drivesdk
- ExcelMobVeil-0.1.1-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in the publication
  close-out (RECORDS-RECORD.md), since this file cannot
  carry its own upload ID.
