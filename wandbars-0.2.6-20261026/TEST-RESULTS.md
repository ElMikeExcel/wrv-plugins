# ExcelWandBars 0.2.6 test results (box rig, 2026-10-26)

Jar: files/plugins/ExcelWandBars-0.2.6.jar
sha256 cc70aa3683623cfc1b315950e47d4d11cf017518e813f118701107b3df5f0fe0 (rebuilt byte for byte at 9:42 AM CDT)
Live base: ExcelWandBars-0.2.5.jar sha256 1e897c936430dacec3b15231e658f7dd87147f9ca8ff4f2cecdf67ba1cecde3c (reproduced from src-0.2.5)

## Status, kept separate

| Item | Status |
|---|---|
| Built | YES, deterministic jar (javac --release 21) |
| Unit tests | 493 passed, 0 failed (run-tests.sh; new PickTest, TapBarTest updated) |
| Rig tested (Paper 1.21.11, Magic spells.yml sha c505e510 = live) | YES, see below |
| Java proof | Rig bot on a Java connection. NOT tried with a real Java game client |
| Bedrock proof | Simulated (`wandbars simbedrock`, swing, item use and drop sent as a phone would). NOT tried on a real phone through Geyser |
| Paper 26.3 (live) | NOT tested, rig is 1.21.11 (same as earlier WandBars rounds) |
| Live server | NOT touched. Not installed |

## The bug (0.2.5, reproduced)
- 0.2.5 set the Tap to pick wand to `quick_cast: false`. In Magic (elmakers) that leaves the wand's quick cast
  open, so `Mage.activateIcon` casts any spell that has `quick_cast: true` in its own spells.yml entry the moment
  its icon is tapped. On WRV that is cure, boon, superheal, feed and recall.
- The wand kept Magic's `right_click: toggle`. On Bedrock a press and hold sends an item use, so the hold closed the bar instead of casting.

## The fix (0.2.6)
- Tap to pick wands get `quick_cast: manual` (a tap only picks, for every spell) and `right_click: none`.
- ExcelWandBars handles right click itself: Java = right click puts items back / brings the bar back,
  Bedrock = the item use of a press and hold casts the picked spell once (deduped with the swing of the same hold
  within 400 ms; repeats within 350 ms ignored).
- Tap bar, crouch paging, crouch to change bar and the stance scroll fix are unchanged (code paths not touched,
  unit tests and the Tap bar rig phase pass).
- Wording: /wandstyle button `Tap: pick. Hold: cast. Crouch: page` (Bedrock), `Key picks, left click casts, sneak pages`
  (Java). The wand tip and how-to lines say tap picks, press and hold (or left click) casts.

## Rig runs (logs in evidence/, harness evidence/t_pick.js with tb025.js and lib.js)

| Run | Jar | Result | Log |
|---|---|---|---|
| baseline (earlier script version) | 0.2.5 | 13 pass, 10 fail: taps cast (0->3), right_click=toggle, item use closed the bar | rig-pick-025-baseline.log |
| fix, final run 9:28 AM CDT | 0.2.6 | **23 pass, 0 fail** | rig-pick-026.log (last block) |
| rollback check (0.2.5 booted on the 0.2.6 data, final script) | 0.2.5 | 12 pass, 11 fail: the bug is back (taps cast 1->4, item use closes the bar); 0.2.5 rewrote quick_cast=false and right_click=toggle; items back exact on drop, Q and right click; Tap bar 4/4 | rig-rollback-025.log |

rig-pick-026.log holds 4 runs. The first 3 (15/8, 21/2, 20/3) failed on harness problems that were fixed in
t_pick.js between runs, not in the jar: hard-coded icon slots, a cumulative CAST tag, a stale client view of the
empty slot, and the bar reopening on its last page (page 2) before the crouch check. The jar was the same in all 4.

Highlights of the final 0.2.6 run:
- Tapping every page 1 icon (heal, boon, feed, extinguish, aqualung, cure, shell) picked each, casts 16->16.
- Swing casts the picked spell once (feed, 16->17), bar stays up.
- Item use alone casts once (boon, 17->18), bar stays up (0.2.5 closed it).
- Swing + item use of one hold: 18->20. The item use is deduped (status counter `pick-hold-swing-cast` = 1); the 2nd cast
  is the test's second swing, same as 0.2.5 (Magic cooldowns apply).
- Crouch shows Page 2/2. Drop and the empty slot give items back exact.
- Java: keys pick (0 casts), left click casts, right click closes and reopens, Q items back.
- Tap bar: tap casts at once, press and hold never casts, items back.

## Not verified
- Real phone (Geyser) and real Java client.
- Paper 26.3 boot (rig is Paper 1.21.11).
- Stance scroll fix not rerun on the rig this round (code not touched; unit tests cover it).
- Tapping the aqualung icon leaves extinguish as the active spell. Same in 0.2.5, not changed here.
- rollback-025 run: the "tap the empty slot" check failed after the 0.2.5 item use had already closed the bar
  (a cascade of the bug). Items were intact in the next phase (diamond 7/7, cobblestone 13/13, iron 5/5).

Rig state after testing: the wb025 rig is back on 0.2.5 (0.2.6 jar parked in offjars/ as .off).
