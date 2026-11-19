# SOURCE - ExcelDeathProtection 0.2.1

ExcelDeathProtection 0.2.1 - ExcelItems migration M2 (registry IDs 3 then 2).
plugin.yml depend: [Vault, ExcelItems]. Premium (ID 3, deathpotion): createPotion is full delegation to ExcelItemsAPI.create(3) - the registry mint dual-stamps primary excelitems:id=3 + legacy exceldeathprotection:potion BYTE 1 + lore_version INTEGER 2, test-pinned field-for-field against the 0.2.0 mint; isProtectionPotion identifies via the API (primary-first, legacy fallback, never falls through from a primary mark); behavior exceldeathprotection:premium-drink registered. Basic (ID 2, deathprotection_basic, ACTIVATION): deliverBasic resolves definition 2 from the registry and requires the ACTIVATION kind fail-closed before any grant; the CHEST display stack is display-only - basicDisplayStack() is the only create(2) caller and has no give/delivery/menu caller; behavior exceldeathprotection:basic-activation registered. Premium banked-credit seam (creditPremium) unchanged. No player-facing behavior change.
Build ID: EXC-DPROTECT-0.2.1-WRV-20261118-01-a91c4e.
Tests: 127/127 passed (118 pre-existing + 9 new RegistryMigrationTest run against the real ExcelItems BuiltinItems registry); base gate 118/118 with the rebuilt unmodified 0.2.0 jar byte-identical to the live jar; double-build byte-identical. M2 piece gates PASS (ID-2 negative test; premium credit-seam test).
Build folder of record: ~/workspace/board-r559/m2-deathprotection/ (board supervisor run 559, M2 build worker).
Source tree of record: work/migrated/ in that folder; this package's source tree is the content of the build's source zip of record.
Sources zip: ExcelDeathProtection-0.2.1-sources.zip - 62,231 B /
SHA-256 310c80d5dddceeb6c2816d07f65897c19f822c609215f427ff1b504120b8714f /
MD5 68c4947bd785ad7bc66f6a24f2e1b4d0 (21 files; deposited bytes are the
build folder's ExcelDeathProtection-0.2.1-source.zip, renamed to the Drive
-sources.zip convention - identical bytes, identical hashes).
Built jar (NOT inside the git records package, per house
rule): ExcelDeathProtection-0.2.1.jar - 61,855 B /
SHA-256 deb7b40ae3fdfa74534dc8d1aabfa0434c267275f0db430861a7221f64a92749 /
MD5 daf0d17fb2c03a75aa035b8aca642c7b. The Drive deposit
carries it; it is referenced here by hash only.
Config: Candidate ships the source-tree default config unchanged (config.yml byte-identical base -> migrated). Live data-folder config is minimal (150 B / d6e3208761c5b08dee1687237946b85d4821dd9f3020aa39b54bfbf082115efc: basic-price 15000.0, premium-price 30000.0); installs never replace a live config.
Install: INSTALLED Nov 18, 2026, 11:30 PM CST by wrv-install-failsafe-v2 run 30dd1528-d957-4c98-bfe9-397579735dfe (Homebase lane), chained Install 1 of 3 in the M2+M3+M4 window - see INSTALLED.txt and INSTALL-PACKET.md in this package. Backup wrv-migration-m2m4-20261118; placed bytes pull-back EXACT; boot Done (24.148s), registry line x1, premium-credit seam active, SEVERE 0; RCON excelitems status = 4 registered / next free 5.
Status: Installed (D122). Surfaces: none owed (no player-facing change; packet G-6 check). Post-release proofs owed (not gates): premium buy-and-drink, basic activation, banked-credit re-arm, legacy potion drink - via the board's Needs Testing.

## Drive deposit (Minecraft WRV)

Duplicate checks first (Nov 19, 2026): Drive name searches
for the 0.2.1 artifacts returned no existing deposits
for this version before upload; wrv-plugins duplicate
guard (G2) also NO MATCH for this package.
- ExcelDeathProtection-0.2.1.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  1aw0UMDABu7qWEg_XbSpv_ngk6MSnlfOI, size 61,855,
  MD5 daf0d17fb2c03a75aa035b8aca642c7b - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1aw0UMDABu7qWEg_XbSpv_ngk6MSnlfOI/view?usp=drivesdk
- ExcelDeathProtection-0.2.1-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  1ovtnU1iJIZAgcLRM07JWq9e69-xwMF2I, size 62,231,
  MD5 68c4947bd785ad7bc66f6a24f2e1b4d0 - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1ovtnU1iJIZAgcLRM07JWq9e69-xwMF2I/view?usp=drivesdk
- ExcelDeathProtection-0.2.1-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in the publication
  close-out (RECORDS-RECORD.md), since this file cannot
  carry its own upload ID.
