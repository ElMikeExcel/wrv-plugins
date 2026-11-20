# SOURCE — ExcelWeekendXP 1.2.1

Plugin: ExcelWeekendXP 1.2.1 — audit finding F-1 fix, decision D146.
WeekendXP owns the vanilla (green-bar) XP multiplier itself via a
PlayerExpChangeEvent listener (1.2x Fri-Sun, 1.0x weekdays, >=1.0
guard kept); the reflective mcMMO bridge (VanillaXpBridge) is
deleted. The mcmmo:xprate leg is untouched. mcMMO was NOT rebuilt.
Tests: 25/25 passed on both deterministic builds (7 containers,
0 failed). Double-build byte-identical.
Build folder of record: ~/workspace/weekendxp-121-build-20261119/
(worker W7, board supervisor run 619, Nov 19, 2026).
Source tree in this package: src/ (4 .java) + tests/ (4 .java) +
plugin.yml + build-and-test.sh — identical to the build folder's
src/ tree, hash-referenced in FINGERPRINTS.md / SHA256SUMS.
Source zip of record (build folder, fixed timestamps
2026-11-19 22:30 CST): ExcelWeekendXP-1.2.1-source.zip —
33,236 B / SHA-256 38cc2f3bd47a61120141f8b5cf4d9a09503ef0e2b2fa736b5f352cf56d406955 /
MD5 87869282ab44525ee03f06d4de3bed49 (9 files).
Built jar (NOT inside the git records package, per house rule):
ExcelWeekendXP-1.2.1.jar — 15,148 B /
SHA-256 b4725c555bbf218135d4c3b6eb40ddcdbecb05e46c48e2dec1e49cb1e1d791e6 /
MD5 d13636e6f73173a581bff5e2bf6e3008. The Drive deposit carries
it; it is referenced here by hash only.
Install: INSTALLED Nov 20, 2026, ~01:05-01:09 AM CST by
wrv-install-failsafe chain 0029 (window backup
wrv-d139-chain-20261120; gate list 0/10 twice; boot Done
23.677s at 1:09:14 AM, Paper 26.3-256, SEVERE 0, weekend 1.2x
line observed on Friday) — see INSTALLED.txt and
INSTALL-PACKET.md in this package.
Status: Installed. Proof owed after install (player lane,
Fri-Sun): vanilla XP gain at 1.2x, weekday reset to 1.0x; no
schedule incomplete WARN across one boot + one 30-minute cycle.
Tester Marvin or Jester. No player-facing text changes in
1.2.1; no Guide / website / What's New / tips / menu surfaces
owed (per INSTALL-PACKET.md same-pass note; the fix restores
the already-documented weekend 1.2x vanilla bonus).

## Drive deposit (Minecraft WRV)

Duplicate check first (2026-11-20): exact-name search for
ExcelWeekendXP-1.2.1 artifacts — results recorded in the
publication close-out (RECORDS-RECORD.md).
- ExcelWeekendXP-1.2.1.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file ID, size and
  MD5 are recorded in RECORDS-RECORD.md (verified EXACT with
  download-back MD5 + SHA-256).
- ExcelWeekendXP-1.2.1-source.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file ID, size and
  MD5 are recorded in RECORDS-RECORD.md.
- ExcelWeekendXP-1.2.1-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in RECORDS-RECORD.md, since
  this file cannot carry its own upload ID.
