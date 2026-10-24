# ExcelTips 0.1.0 test results (Oct 24, 2026, box test server only)

Test server: Paper 26.3 rig with Geyser + Floodgate, Magic 11.2.5, ExcelTrash
0.2.0, staged ExcelWandBars 0.2.1 (sha256 d25cf1e8...), ExcelMakersMark 0.1.0,
AutoSort, and a small test-only probe plugin (API register/unregister, fake
item, block and bed events). Test-server timings are looser than the shipped
ones (pop-up calm 8 s, cooldown 60 s, quiet after join 10 s).

Unit checks: 422 passed, 0 failed (`./run-tests.sh`). A deliberate check
against the old `tips.yml` (with the wand tips) fails 4 checks, so the
duplicate and Bedrock-wording guards work.

Two full passes, each with brand-new test players (Java and Bedrock):

| Suite | Pass A | Pass B |
|---|---|---|
| java1: first use, 3-show cap, burst, [OK] click, chat "ok", combat quiet, no stacking, compass, Trash bin, respawn, /tips, /tips show | 22/22 | 22/22 |
| java2: after a server restart (persistence), admin reset, /tips ok, no wand tip in ExcelTips, reload, unknown tip | 8/8 | 8/8 |
| java6: real Magic wand shows only the ExcelWandBars tip, claims, beds (sleep and block) | 7/7 * | 7/7 |
| java4: Trash bin once per open, old trash tip off, MakersMark hand-over not doubled, /mark ok, API register/unregister | 12/12 | 12/12 |
| bed1 (Bedrock through Geyser): tap wording, Floodgate OK pop-up at a calm moment, never during 20 s of hits, Remind me later, /tips ok, new Trash bin words | 20/20 | 20/20 ** |

\* Pass A java6 first crashed on a bug in the test script (item name check),
not the plugin; fixed the script and re-ran it for the same player: 7/7.
\** Pass B Bedrock first ran 17/17, then the test client dropped with an
encryption checksum error from the bedrock-protocol library when the Trash
screen opened. Re-ran with a fresh Bedrock player: 20/20.

Rollback (jar removed, full restart): 5/5. No `/tips`, no tips, ExcelMakersMark
switched to its own built-in tip (shown once), ExcelTrash and ExcelWandBars
fine, no new errors (only the test probe, which needs ExcelTips, refused to
load, as expected). Jar put back and restarted: a player's tip status was
identical to before the rollback (persistence across rollback).

Not tested here: a real phone or console (Bedrock was a protocol test client),
the shipped (slower) timings, ExcelWandBars 0.2.2 Spell Book hand-over, and
Lost & Found (not built into this rig).
