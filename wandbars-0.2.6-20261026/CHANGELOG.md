# ExcelWandBars changelog

## 0.2.6 (2026-10-26)
Fix: Tap to pick (original) now really picks.
- A tap on a spell icon only picks that spell. It never casts, including spells that are quick cast in their own
  Magic (elmakers) spells.yml entry (cure, boon, superheal, feed, recall on WRV). Cause: 0.2.5 set the wand's
  `quick_cast` to `false`, which Magic treats as "not disabled", so those spells cast on the tap.
  0.2.6 sets `quick_cast: manual`.
- Bedrock: press and hold casts the picked spell once and the bar stays up. Before, the hold's item use hit Magic's
  `right_click: toggle` and closed the bar. 0.2.6 sets `right_click: none` on Tap to pick wands and handles
  right click in the plugin (one cast per hold, swing and item use deduped within 400 ms, repeats within 350 ms ignored).
- Java: hotbar keys pick, left click casts, right click puts your items back and brings the bar back, Q puts items back.
- Existing Tap to pick wands are updated on the next hold. Other styles are not touched.
- Wording: /wandstyle line, wand tip and how-to lines now say tap picks, press and hold (phone) or left click
  (computer) casts.
- Unchanged: Tap bar (tap casts, hold reads), crouch paging, crouch to change bar (Sneak bars), stance scroll fix.
- New `wandbars` status counters: pick-right-opens, pick-right-closes, pick-hold-casts, pick-hold-swing-cast, pick-right-repeats.
- Tests: 493 unit tests (new PickTest). Rig: 0.2.5 baseline 13 pass / 10 fail, 0.2.6 23 pass / 0 fail.

## 0.2.5 (2026-10-26)
Tap bar shows every unlocked spell in pages (crouch flips, Page 1/2), press and hold reads and never casts,
Tap to cycle (original) renamed Tap to pick (original), Sneak bars on Java, stance scroll fix.
