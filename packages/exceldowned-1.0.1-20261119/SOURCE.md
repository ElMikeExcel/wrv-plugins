# SOURCE - ExcelDowned 1.0.1

ExcelDowned 1.0.1 - ExcelItems migration M1 (registry ID 4, revive potion).
plugin.yml gains hard depend: [ExcelItems]; RevivePotionItem mint fully delegated to ExcelItemsAPI.create(4) (dual-stamp primary excelitems:id + legacy exceldowned:revive_potion BYTE 1 + revive_potion_lore INTEGER 1 preserved); identify via the API (primary-first, legacy fallback); revive behavior registered against ID 4 (exceldowned:revive-potion). Purchase coordinator/journal, buy/give verb split (D109/D110), Cleric Raise Ally and all config tunables unchanged. No player-facing behavior change.
Build ID: EXC-DOWNED-1.0.1-WRV-20261118-01-d0wn3d.
Tests: 77/77 passed (71 baseline 1.0.0 suite + 6 new M1 tests), in two independent local-build.sh runs, jars byte-identical across both runs and within each run (jar == repro). G-1/G-2 PASS EXACT; M1 piece gates (buy/give verb split, purchase-journal boot check) PASS.
Build folder of record: ~/workspace/board-r559/m1-downed/ (board supervisor run 559, M1 build worker).
Source tree of record: work/build-1.0.1/ in that folder; this package's source tree is the content of the build's source zip of record (src + tests + plugin.yml + config.yml + local-build.sh + DESIGN-*.md).
Sources zip: ExcelDowned-1.0.1-sources.zip - 108,373 B /
SHA-256 13390e265444cd543f584cbf2a934429776339264295b08c5e30630d88d902b0 /
MD5 480f932844efb900a98a4c2b53805cad (34 files; deposited bytes are the
build folder's ExcelDowned-1.0.1-source.zip, renamed to the Drive
-sources.zip convention - identical bytes, identical hashes).
Built jar (NOT inside the git records package, per house
rule): ExcelDowned-1.0.1.jar - 140,323 B /
SHA-256 9e8ae0f5b297045a5e913e2c8e7c3e2cccb858c1fe32bbed6e8bb2f7566c32b3 /
MD5 f873c2769e2b05663c6adf153b152774. The Drive deposit
carries it; it is referenced here by hash only.
Config: Config unchanged by the migration: live plugins/ExcelDowned/config.yml 4,916 B / 94eda94a6ff9b9c23d11936315703199fdf8aabd165283266a035d4cbc87a3bf (G-1 pull EXACT); the candidate tree ships the same config.
Install: INSTALLED Nov 19, 2026, 2:10-2:53 AM CST by wrv-install-failsafe-v2 run d94d7abf-efa1-48f5-b8c4-cf7a1981630c (panel lane), chained Install 1 of 2 with ExcelDowned 1.0.2 - see INSTALLED.txt and INSTALL-PACKET.md in this package. Backup wrv-downed-m1-102-20261119. Placed bytes pull-back EXACT, then superseded in-chain by 1.0.2 (the placed 1.0.1 jar is preserved live as ExcelDowned-1.0.1.jar.bak-pre102). The D125 pre-install gate (Homebase-mirror G-4 co-resident boot) was discharged Nov 19, 2026, 1:57 AM - see RIG-RESULTS.md. ExcelDowned 1.0.2 (open potion targeting) is a separate version with its own record package.
Status: Installed (D122). Surfaces: none owed (no player-facing change; packet G-6 check). Post-release proofs ride the board's Needs Testing, not gates.

## Drive deposit (Minecraft WRV)

Duplicate checks first (Nov 19, 2026): Drive name searches
for the 1.0.1 artifacts returned no existing deposits
for this version before upload; wrv-plugins duplicate
guard (G2) also NO MATCH for this package.
- ExcelDowned-1.0.1.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  1sL5hLKKzn4xiSFGKev06mE_0bjOnpzQu, size 140,323,
  MD5 f873c2769e2b05663c6adf153b152774 - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1sL5hLKKzn4xiSFGKev06mE_0bjOnpzQu/view?usp=drivesdk
- ExcelDowned-1.0.1-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  1qXPnpxB0kBZ1tC1ZH2n5HOQqCrS_VLo_, size 108,373,
  MD5 480f932844efb900a98a4c2b53805cad - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1qXPnpxB0kBZ1tC1ZH2n5HOQqCrS_VLo_/view?usp=drivesdk
- ExcelDowned-1.0.1-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in the publication
  close-out (RECORDS-RECORD.md), since this file cannot
  carry its own upload ID.
