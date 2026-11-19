# SOURCE — ExcelShops 1.3.10 revivepatch

Plugin: ExcelShops 1.3.10 (rebuilt, NO version bump — packet
call F-1) — presentation-only Revive patch: adds the
REVIVE_POTION route/product (catalog invariant 21 -> 22
products) dispatching the canonical command
'[console] downed revivepotion buy %player_name%', plus the
Revive shelf entries in both menu formats (D112 slot split:
live Java menu slot 6, jar-bundled Bedrock-validation slot 5).
The free admin mint spelling ('give') appears in NO menu
click list. Live plugins/ExcelShops/config.yml and
spawn-catalog.yml were UNTOUCHED by the install.
Tests: 443/443 passed in two independent packaging runs.
Deterministic double-build byte-identical.
Build tree of record: ~/workspace/board-r509/
excelshops-patch-tree/ (board run 509, worker W6; base is
the verified live 1.3.10 tree, live jar 267,490 B /
1d7f773399d540e30afa990d8183f9cf49f9f469fd18f329de78cd8c0e173b2b).
Packaging of record: ~/workspace/downed-100-filing/
shops-build/ (two runs, 443/443 each, jars byte-identical).
Patch scope vs base: 8 artifacts differ/add — MobileCatalog.java,
ShopUi.java, bundled mobile-menus (2), live-menu-patches (2),
MobileCatalogTest.java, MobileCatalogDerivedTest.java.
Source snapshot in this package: the FULL patch tree
(77 files: src 35 + tests 24 + mobile-menus 6 +
live-menu-patches 2 + plugin.yml, config.yml,
spawn-catalog.yml, local-build.sh, pom.xml, LICENSE,
build-1.3.1.sh, INSTALL-RECORD-1.2.md, tools/,
tests_python/). NOTE: the tree's own config.yml /
spawn-catalog.yml are base-tree copies, NOT placed files.
Sources zip: ExcelShops-1.3.10-revivepatch-sources.zip —
690,254 B /
SHA-256 8de299edf6944f0f9ceffd4f8f4f53d880b2a1319810fd1c87813f78563efd36 /
MD5 3bc58a4bb2024ec18dc1b209767c4a51 (77 files, full tree).
Built jar (NOT inside the git records package, per house
rule): ExcelShops-1.3.10-revivepatch.jar — 268,988 B /
SHA-256 0893dd4350eb541d3c465f2aeea3d2e7e4ee97b6e345f24c00cca373daafb40a /
MD5 2227aff5c9e2430728c4e7117fd3454a. Placed at install
under the live filename plugins/ExcelShops-1.3.10.jar.
The Drive deposit carries it under the revivepatch name;
it is referenced here by hash only.
Install: INSTALLED Nov 18, 2026, 4:38-5:13 PM CST by
wrv-install-failsafe run 9a4cf9a4, in ONE gated unit with
ExcelDowned 1.0.0 (backup wrv-downed100-20261118; boot
Done 23.794s, SEVERE 0, guild OPEN 5 of 5, Economy:
EssentialsX Economy) — see INSTALLED.txt and
INSTALL-PACKET.md in this package.
Status: Installed + Released (D122); post-release P4
purchase checks per platform owed, not gates.

## Drive deposit (Minecraft WRV)

Duplicate check first (2026-11-18): name contains
'ExcelShops-1.3.10' / 'revivepatch' -> 0 files before upload.
- ExcelShops-1.3.10-revivepatch.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  10N2O3gICCU6GYKURXyFw3mnHsRCkAarn, size 268,988,
  MD5 2227aff5c9e2430728c4e7117fd3454a — EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/10N2O3gICCU6GYKURXyFw3mnHsRCkAarn/view?usp=drivesdk
- ExcelShops-1.3.10-revivepatch-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  1ZraYbJJHPQptopuedeSUe5W2sPrmt5fu, size 690,254,
  MD5 3bc58a4bb2024ec18dc1b209767c4a51 — EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1ZraYbJJHPQptopuedeSUe5W2sPrmt5fu/view?usp=drivesdk
- ExcelShops-1.3.10-revivepatch-SOURCE.md (this file) ->
  "Source" folder: Drive file ID + MD5 are recorded in the
  publication close-out (RECORDS-RECORD.md), since this file
  cannot carry its own upload ID.
