# SOURCE — ExcelDowned 1.0.0

Plugin: ExcelDowned 1.0.0 — Revive system (Revive Potion +
Cleric "Raise Ally" + shop charge leg, D109). Designed with
Marvin's revive idea (Nov 17, 2026); launch price $5,000,
shop-only, carry do not drink, revives at 50% HP.
Build ID: EXC-DOWNED-1.0.0-WRV-20261117-01-d0wn3d.
Tests: 71/71 passed (70 baseline + 1 F1 pinning test).
Deterministic double-build byte-identical.
Build folder of record: ~/workspace/board-r510/f1-fix/
(board run 510, worker W7, F1 journal fix). Source tree of
record: build-1.0.0/ in that folder — VERIFIED by hash:
its jar is 140,683 B / fcfa96d2... (the installed jar);
the pre-fix tree ~/workspace/downed-016-build-20261117/
work/build-1.0.0/ builds ed157aee... and is NOT the source
of the installed jar. The F1 fix is one production file
(RevivePurchaseJournal.java, directory sync best-effort in
the ExcelShops 1.3.9 shape) + one test, carried in the
package as F1-source.patch / F1-test.patch.
Source tree in this package: src/ (30 .java) + tests/
(DownedTest.java) + local-build.sh, plugin.yml, config.yml,
DESIGN-0.1.2.md, DESIGN-0.1.3.md, DESIGN-0.1.4.md,
DESIGN-0.1.5.md, DESIGN-1.0.0.md.
Sources zip: ExcelDowned-1.0.0-sources.zip — 377,378 B /
SHA-256 5ed45dc6bc8e1e39bc773dc13bf5027bd18b3832e39ffadddba929ed0b75ddef /
MD5 337c182d8d5afd05c421bb6175f2faf8 (39 files: src + tests +
config.yml + plugin.yml + local-build.sh + DESIGN-*.md).
Built jar (NOT inside the git records package, per house
rule): ExcelDowned-1.0.0.jar — 140,683 B /
SHA-256 fcfa96d2036926b0eee5eaf338196ceee55680ca56e9df415563e35b1990e57c /
MD5 26601e98879ac3eb11529b3f80fad913. The Drive deposit
carries it; it is referenced here by hash only.
Config placed at install: 4,916 B /
94eda94a6ff9b9c23d11936315703199fdf8aabd165283266a035d4cbc87a3bf
(pure superset of the live 0.1.5 config).
Install: INSTALLED Nov 18, 2026, 4:38-5:13 PM CST by
wrv-install-failsafe run 9a4cf9a4 (backup
wrv-downed100-20261118; boot Done 23.794s, SEVERE 0, guild
OPEN 5 of 5) — see INSTALLED.txt and INSTALL-PACKET.md in
this package. Website pair published as winkreyesvalley
PR #263 merged Nov 18, 2026, 5:43 PM CST.
Status: Installed + Released (D122); post-release player
proofs owed (P4 per platform + live revive / Raise Ally),
not gates.
Purchase journal note: plugins/ExcelDowned/purchases.journal
is created by 1.0.0 at first boot; potion_mint records carry
"source":"shop_buy" for shop purchases (the free admin mint
uses the 'give' spelling and is never wired to a menu).

## Drive deposit (Minecraft WRV)

Duplicate check first (2026-11-18): exact-name search for
ExcelDowned-1.0.0 artifacts -> 0 files before upload
(only 0.1.x deposits exist).
- ExcelDowned-1.0.0.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  1vjPNSkMeiqn3TPEL9EkhkCOu6T26uUOB, size 140,683,
  MD5 26601e98879ac3eb11529b3f80fad913 — EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1vjPNSkMeiqn3TPEL9EkhkCOu6T26uUOB/view?usp=drivesdk
- ExcelDowned-1.0.0-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  162DCUTFVaMYJi5n7TnGnwKXB0BGbkDWx, size 377,378,
  MD5 337c182d8d5afd05c421bb6175f2faf8 — EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/162DCUTFVaMYJi5n7TnGnwKXB0BGbkDWx/view?usp=drivesdk
- ExcelDowned-1.0.0-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in the publication
  close-out (RECORDS-RECORD.md), since this file cannot
  carry its own upload ID.
