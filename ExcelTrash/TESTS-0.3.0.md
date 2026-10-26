# ExcelTrash 0.3.0 tests (Oct 26, 2026)

Jar: upload/plugins/ExcelTrash-0.3.0.jar, sha256 7733773ab56087f33ef404bc8c3eaf1098bd56abdae489a8db05fb117038a242,
37,258 B. Rebuilt twice from src/ with src/local-build.sh: byte-identical both times.

Unit: 24/24 pass (tests/unit-24.log): Confirm (two-tap state) 5, DeathReturn (death settle) 6, older 13.

Offline Paper rig (/workspace/trash-rig, Paper 26.3-214, the live-like plugin set incl. Essentials, GriefPrevention,
AutoSort, ExcelShops, ExcelAuditLog; mineflayer bot through ViaVersion): tests/rig-run15-T1-T6.log
RESULT pass=41 fail=0 (1:47-1:49 AM CDT):
- T1 first-open hint once, Help book, oak door Cancel, pick wording when not looking at a chest.
- T2 first OK deletes nothing (TNT "Tap again to delete 10 items"), 5 s timeout back to normal, second tap deletes,
  lime "Deleted 10 items", chat line names Recover and /trash undo, trash.log row, Nothing to delete on empty,
  Recover works, count change re-arms.
- T3 looking at a chest: button names it, chest view, two-tap OK, /trash undo puts items back in the chest.
- T4 not looking: tap chest button, screen closes, tap the chest = chest trash view; next tap = normal chest.
- T5 /trash chest arms the pick; it expires after 30 s; /trash help; mode switch refused with items in the trash slots;
  close gives items back.
- T6 death: keep-inventory death (probe mimics a keep-inventory plugin: keepInventory + clear drops at HIGHEST) keeps
  the 7 trash-slot items, chat line, nothing logged as trashed; keep-inventory death with a full bag gives them on respawn
  (at the feet); normal death drops them once, no dupe.
Boot: "ExcelTrash 0.3.0 enabled; undo window 5 minutes; chest mode on; two-tap OK; close-action=return".
SEVERE/ERROR lines are the same rig-only lines as the 0.2.0 baseline (GriefPrevention claim id, Essentials version,
ExcelShops rig journal, Magic shutdown nag). Nothing from ExcelTrash.

Staged tips/What's New on the live plugin jars (ExcelTips 0.1.0 3bf8d0cd, ExcelWhatsNew 0.1.0 691ee97a) on the rig
(tests/tips-whatsnew-check-rig.log): "28 tips (28 switched on)", tips check: trash-bin "(off while ExcelTrash>=0.3.0)",
trash-confirm and trash-chest checked with no warnings (the one warning, guild-gate /ag, is rig-only: no guild plugin
command there). "changelog.yml: 6 entries, no warnings." whatsnew list: 1. Trash asks before it deletes (Oct 26),
2. Stance lock no longer blocks your hotbar (Oct 26), 3. Bedrock players: port 25622 only (Oct 25).

Magic crafting.yml and the 1.5.22 jar were tested in their own packets (audit-20261026/stage-magic-recipes/rig-after.log:
28 WARN -> 0; bedrock-menu-parity-1522-20261026). AutoBroadcaster edit script: tested on a box copy (1 line changed,
valid YAML); the live file is edited only after download.

Not tested (owed in game): a real Bedrock touch through Geyser, a real client right-click on a chest, real
a keep-inventory plugin (Essentials keepinv and the like) (simulated by the probe).
