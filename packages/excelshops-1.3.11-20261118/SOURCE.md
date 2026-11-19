# SOURCE - ExcelShops 1.3.11

ExcelShops 1.3.11 - ExcelItems migration M4 (registry IDs 1-4 by reference; retires the code-pinned MobileCatalog special-product pattern).
plugin.yml gains hard depend: [ExcelItems] (existing softdepend otherwise unchanged). The four special products (veil potion ID 1, death protection basic ID 2, death protection potion ID 3, revive potion ID 4) resolve by registry ID/key from config instead of the code-pinned MobileCatalog Route switch + expectCommands pinning; delivery stays the owner console command, registry-resolved (D124). The catalog count invariant (22 products) and the 'special products are never created, repriced or shadowed by catalog offers' guard are re-expressed against registry IDs and fail closed on collision. Proof of pattern: adding an already-registered item to the shop is now a config line only - demonstrated on the rig with a throwaway config-only addition and reverted before filing. Shops mints nothing; it never did. Shelf slots/prices unchanged (D112: Revive Java slot 6 / Bedrock slot 5; Lost & Found untouched); one internal details-screen lore alignment is the only presentation delta.
Build ID: (none named - version 1.3.11 carried in plugin.yml).
Tests: 453/453 passed (443 baseline revivepatch suite + 10 new M4 tests), in two independent build.sh runs, jars byte-identical across both runs. M4 piece gates PASS: catalog-invariant test at the 22-product baseline with the registry-ID guard; config-only addition demo (reverted).
Build folder of record: ~/workspace/board-r559/m4-shops/ (board supervisor run 559, M4 build worker).
Source tree of record: work/migrated/ in that folder; this package's source tree is the content of the build's source zip of record.
Sources zip: ExcelShops-1.3.11-sources.zip - 663,538 B /
SHA-256 496e8791c71fd1b91e21ed31ae34da91b32750c267013d27f70d5db15ca511a7 /
MD5 061fc0cbeedce41d0d7749bd6fa83087 (73 files; deposited bytes are the
build folder's ExcelShops-1.3.11-source.zip, renamed to the Drive
-sources.zip convention - identical bytes, identical hashes).
Built jar (NOT inside the git records package, per house
rule): ExcelShops-1.3.11.jar - 277,494 B /
SHA-256 91b8559bf63cf2b03aff53a1d7180cffd881d505852ee3c08e0721becc735102 /
MD5 bc6dc0017becc50669914316bb4ef405. The Drive deposit
carries it; it is referenced here by hash only.
Config: Live config pulled at G-1 (build folder pulls/): plugins/ExcelShops/config.yml SHA-256 4eac2df56f4b0c16342f3077a226ad35ee1c01f4867ad199b02023425532468b; spawn-catalog.yml 7f9a41cca38390e9bf3aed1102009d6c8f5ece8ff2ddf498c75559a96fc15360. Installs never replace a live config.
Install: INSTALLED Nov 18, 2026, 11:30 PM CST by wrv-install-failsafe-v2 run 30dd1528-d957-4c98-bfe9-397579735dfe (Homebase lane), chained Install 3 of 3 in the M2+M3+M4 window - see INSTALLED.txt and INSTALL-PACKET.md in this package. Backup wrv-migration-m2m4-20261118; placed bytes pull-back EXACT; boot Done (24.148s), 'ExcelShops purchases available.' x1, 'Mobile shop descriptions unavailable' count 0, SEVERE 0; RCON excelitems status = 4 registered / next free 5.
Status: Installed (D122). Surfaces: no What's New entry owed (reference-only change; packet states the check); menus verified both platforms. Post-release proofs owed (not gates): Bedrock shelf rendering + buy flow per special, reload check - via the board's Needs Testing.

## Drive deposit (Minecraft WRV)

Duplicate checks first (Nov 19, 2026): Drive name searches
for the 1.3.11 artifacts returned no existing deposits
for this version before upload; wrv-plugins duplicate
guard (G2) also NO MATCH for this package.
- ExcelShops-1.3.11.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  1M7-TSlQeP7CUstKGTThIorpJBBQ2O6uZ, size 277,494,
  MD5 bc6dc0017becc50669914316bb4ef405 - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1M7-TSlQeP7CUstKGTThIorpJBBQ2O6uZ/view?usp=drivesdk
- ExcelShops-1.3.11-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  128mSS2Rs0KOnzzKgWAyZ1y8LphLoV5Wh, size 663,538,
  MD5 061fc0cbeedce41d0d7749bd6fa83087 - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/128mSS2Rs0KOnzzKgWAyZ1y8LphLoV5Wh/view?usp=drivesdk
- ExcelShops-1.3.11-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in the publication
  close-out (RECORDS-RECORD.md), since this file cannot
  carry its own upload ID.
