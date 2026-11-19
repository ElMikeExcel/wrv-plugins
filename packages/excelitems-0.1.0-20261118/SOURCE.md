# SOURCE - ExcelItems 0.1.0

ExcelItems 0.1.0 - standalone custom-item registry plugin (Mike's design law, Nov 17, 2026). The foundation of the ExcelItems migration: M1-M4 hard-depend on this build.
New plugin. Every custom item is defined once as data with a permanent ID number + name (never reused, never renumbered); identity is carried in the item's own PDC - primary excelitems:id (INTEGER) first, legacy fallback ONLY when no primary mark is present, and a stale/forged primary never falls through to a second identity. Behavior handlers are written once in code in the owning plugin and registered against the ID (registerBehavior); ExcelItems itself declares NO hard depend (it IS the dependency) and stays small, stable and boring - ExcelItems is free, the products on top are the paid part. 0.1.0 ships definitions only for the four existing special products: ID 1 veil_potion (ITEM), ID 2 deathprotection_basic (ACTIVATION - no physical item; CHEST is display only), ID 3 deathpotion (ITEM), ID 4 revive_potion (ITEM). Next free ID 5. Registry minting dual-stamps primary + legacy marks so un-migrated owners still recognize registry-minted items. API (via Bukkit services after depend: [ExcelItems]): create / identify / identifyDefinition / isCustomItem / isItem / definition / register / registerBehavior / behavior / nextId.
Build ID: ExcelItems-0.1.0 (fixed entry order, fixed timestamp 2026-11-18T00:00:00Z, no manifest - house pattern).
Tests: 94/94 passed (RegistryTest 43, BuiltinItemsTest 34, AssignmentsTest 7, PluginYmlGuardTest 10), including a clean-room re-run; double-build and clean-room rebuild from the source zip byte-identical. Mirror rig 17 pass / 0 fail, SEVERE 0 - see RIG-RESULTS.md.
Build folder of record: ~/workspace/excelitems-build-20261118/ (ExcelItems build worker, Nov 18, 2026).
Source tree of record: src/ExcelItems/ in that folder (src/, tests/, plugin.yml, config.yml, build.sh, run-tests.sh); this package's source tree is the content of the build's source zip of record.
Sources zip: ExcelItems-0.1.0-sources.zip - 22,709 B /
SHA-256 0c5f20b9e2a1cd8b6894296df5880bd977ebbccb36bd040c9d6dd6042d027001 /
MD5 b3357c1611ce1ebcb3b7762dc030b7a7 (18 files; deposited bytes are the
build folder's ExcelItems-0.1.0-source.zip, renamed to the Drive
-sources.zip convention - identical bytes, identical hashes).
Built jar (NOT inside the git records package, per house
rule): ExcelItems-0.1.0.jar - 20,214 B /
SHA-256 85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d /
MD5 ed68d6b14e04055ab851c0c317052974. The Drive deposit
carries it; it is referenced here by hash only.
Config: No config replacement at install: the plugin created plugins/ExcelItems/config.yml on first enable carrying the ID assignments 1-4, validated fail-closed at enable (drift disables the plugin). The source-tree config.yml is inside this package's source tree.
Install: INSTALLED Nov 18, 2026, 6:41 PM CST by wrv-install-failsafe-v2 run 20261118-1804 (Homebase lane), Install 1 of 1 - see INSTALLED.txt and INSTALL-PACKET.md in this package. Backup wrv-excelitems-20261118; G1 fresh census 537 entries / 91 jars with EXCELITEMS_MATCHES=0; placed bytes pull-back EXACT; boot Done (23.844s), 'ExcelItems enabled: 4 custom item(s) registered, next free id 5.' x1, SEVERE 0, guild OPEN 5 of 5; post-boot census 539 / 92 EXACT; end state ONLINE, 0 players.
Status: Installed (D122). Surfaces for the 0.1.0 install itself were completed by the parent/website lane (What's New both surfaces + website pair). Proof owed after install at filing: the in-game /excelitems give + identify round-trips (player-required in 0.1.0) - deferred to the migration unit's M5 integration close-out; the JVM harness covers the same round-trips.

## Drive deposit (Minecraft WRV)

Duplicate checks first (Nov 19, 2026): Drive name searches
for the 0.1.0 artifacts returned no existing deposits
for this version before upload; wrv-plugins duplicate
guard (G2) also NO MATCH for this package.
- ExcelItems-0.1.0.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  1HHAmFbwOhhD3ifRNiu1jMDnFYiaJxY3M, size 20,214,
  MD5 ed68d6b14e04055ab851c0c317052974 - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1HHAmFbwOhhD3ifRNiu1jMDnFYiaJxY3M/view?usp=drivesdk
- ExcelItems-0.1.0-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  1y4NH0-3_xKSprtutK0Dx7ogfyD3kTBME, size 22,709,
  MD5 b3357c1611ce1ebcb3b7762dc030b7a7 - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1y4NH0-3_xKSprtutK0Dx7ogfyD3kTBME/view?usp=drivesdk
- ExcelItems-0.1.0-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in the publication
  close-out (RECORDS-RECORD.md), since this file cannot
  carry its own upload ID.
