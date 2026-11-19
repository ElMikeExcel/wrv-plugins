# SOURCE - ExcelDowned 1.0.2

ExcelDowned 1.0.2 - open Revive Potion targeting. SUPERSEDES
design S-1's party-only Revive Potion targeting per Mike's
decision of Nov 19, 2026, 00:07 CST ("I like the idea of being
able to just use it on anyone you come across to be helpful").
Party membership removed at all three consult points on the
potion path: prompt policy (RevivePotionPolicy.promptEligible),
the right-click interact (RevivePotionUi.onInteractEntity,
silent return + dead membership() helper removed), and the
confirm verdict (RevivePotionPolicy.confirmVerdict - the
NOT_PARTY refusal, its enum value, and its UI message removed,
no dead code). Potion-facing copy updated (plugin.yml
description, two DownedCommands lines, the downed-player
message). Built ON the 1.0.1 source tree (M1 ExcelItems
migration, registry ID 4); everything else is 1.0.1's code.
Untouched, source-pinned in the new tests: the Cleric Raise
Ally NOT_PARTY gate and the Resurrection spell's party rule
(MagicBridge) - separate code paths, outside this build.
Economics/mechanics unchanged: $5,000 price, passive carry,
consume-only-after-land, 50% HP revive, 10-minute per-carrier
cooldown, totem-first deferral, 300 s bleed-out, self-revive
exclusion, ranges, buy/give verb split.
Build ID: EXC-DOWNED-1.0.2-WRV-20261119-01-d0wn3d.
Tests: 79/79 passed (77 carried from the 1.0.1 suite + 2 new:
potionOpenTargetingStrangerReviveChain,
potionOpenTargetingSourcePins), double-build byte-identical
(jar == repro), fingerprint gate PASS.
Build folder of record: ~/workspace/downed-102-partyhint-20261119/
(build worker, Nov 19, 2026, ~12:10-12:20 AM CST).
Source tree of record: work/build-1.0.2/ in that folder; this
package's source tree is that tree (src + tests + plugin.yml +
config.yml + local-build.sh), verified byte-exact against the
build tree at records time.
Sources zip: ExcelDowned-1.0.2-sources.zip - 109,326 B /
SHA-256 b93066bf87303a5fe0826e06a5b4a264d4aaf63f23cd7f17be7e0a3c53d2333d /
MD5 f2fd427396c5a3891d0cb9584ec08baa (34 files). Note: unlike
1.0.1, the 1.0.2 build folder carried no source zip of its
own, so this zip of record was created at records time from
the build tree above (same 34-file composition as the 1.0.1
zip) and verified by full extraction diff against the tree;
the staging copy keeps the build-folder name convention
(ExcelDowned-1.0.2-source.zip) - identical bytes, identical
hashes.
Built jar (NOT inside the git records package, per house
rule): ExcelDowned-1.0.2.jar - 139,867 B /
SHA-256 8237327f781c6a63e78637153d4e1b75b52d34cc20169e3f30538bef9085609f /
MD5 5979669be40d9745c9fdef2bed02dfa9. The Drive deposit
carries it; it is referenced here by hash only.
Config: Config unchanged by this build: live
plugins/ExcelDowned/config.yml 4,916 B / 94eda94a6ff9b9c23d11936315703199fdf8aabd165283266a035d4cbc87a3bf (G-1 pull EXACT at M1); the candidate tree ships the same config (hash re-verified at records time).
Install: INSTALLED Nov 19, 2026, 2:10-2:53 AM CST by
wrv-install-failsafe-v2 run d94d7abf-efa1-48f5-b8c4-cf7a1981630c
(panel lane), chained Install 2 of 2 after ExcelDowned 1.0.1 -
see INSTALLED.txt and INSTALL-PACKET.md in this package.
Backup wrv-downed-m1-102-20261119. Placed bytes pull-back
EXACT; boot Done (24.305s), ExcelItems enabling before
ExcelDowned v1.0.2, ID-4 revive-behavior line present, ready
line carries the 1.0.2 build ID, SEVERE 0, guild OPEN 5 of 5;
end state Offline + whitelist on (only .MikeExcel), arrival
state restored. The sequencing gate (1.0.2 installs only
after 1.0.1 INSTALLED) was satisfied inside the same window.
Load check (D122): VM-local Paper 26.2 rig PASS - see
RIG-RESULTS.md.
Status: Installed (D122). Surfaces: DONE - website pair
(winkreyesvalley PR #267, merged; mandated line "Revive
Potions now work on any downed player - no party needed")
and in-game What's New (filed as a MANIFEST TEXT entry for
the failsafe); see
~/workspace/downed-102-surfaces-20261119/SURFACES-RECORD.md.
Guide leg skipped (Guide development hibernated, Mike,
Nov 16, 2026). Post-release live check rides the board's
Needs Testing, not a gate.

## Drive deposit (Minecraft WRV)

Duplicate checks first (Nov 19, 2026): Drive exact-name
searches for the 1.0.2 artifacts returned no existing
deposits for this version before upload; wrv-plugins
duplicate guard (G2) also NO MATCH for this package.
- ExcelDowned-1.0.2.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  1Dwyil0vPz84qZgWEiT9GvGTsGNi6Xxer, size 139,867,
  MD5 5979669be40d9745c9fdef2bed02dfa9 - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1Dwyil0vPz84qZgWEiT9GvGTsGNi6Xxer/view?usp=drivesdk
- ExcelDowned-1.0.2-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  1GuidZ21bnve3WNbbDSiPJ-RY-4g9yxMg, size 109,326,
  MD5 f2fd427396c5a3891d0cb9584ec08baa - EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1GuidZ21bnve3WNbbDSiPJ-RY-4g9yxMg/view?usp=drivesdk
- ExcelDowned-1.0.2-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in the publication
  close-out (RECORDS-RECORD.md), since this file cannot
  carry its own upload ID.
