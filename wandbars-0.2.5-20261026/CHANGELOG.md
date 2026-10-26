# ExcelWandBars 0.2.5 (from 0.2.4)

Jar sha256 1e897c936430dacec3b15231e658f7dd87147f9ca8ff4f2cecdf67ba1cecde3c
Base: live 0.2.4, jar sha256 2c1526ba494a6402f508d050e2a7fcbbbf05c37c3ef1ad21b05f99cbbed519c2 (source in the first commit of this PR, so the diff shows only 0.2.5 changes)

## Tap bar: every spell on your bar
- Every unlocked spell is packed onto the hotbar (new core/PageLayout.java). One slot always stays empty so you can get your items back.
- Crouch (sneak on Java) only flips pages when the spells do not all fit. With one page, crouch says on the action bar that all your spells are already on the bar.
- Pages: Beginner 6 spells on 1 page, Student 12 on 2, Master 17 on 3. Every spell shown once.

## Hold-to-read on Bedrock touch
- A phone press and hold can arrive as a swing, as a right click (item use) or as a drop. All three now show the spell list, or the page of the spell you just tapped. None of them casts or closes the bar.
- The Tap bar wand profile sets `right_click: none`. Magic's right click toggle cannot be cancelled by a plugin, so a phone hold used to close the bar. ExcelWandBars now handles the right click itself (new `onRightClickWand`, LOWEST priority, main hand, skips sneaking and interactable blocks).
- Java: right click puts your items back, right click again brings the bar back (350 ms debounce).
- New counters in `/wandbars tapbar`: right-reads, right-opens, right-closes, right-repeats.

## Sneak bars stays a separate option, now on Java too
- New config key `sneak-bars-java: true` (default). With it, the Wand controls menu shows all 5 styles on Java and Sneak bars applies on Java. Existing `bars-bedrock-only: true` configs need no change.

## Menu wording says what crouch does
- One-line descriptions per mode (42 chars or less):
  - Bedrock Classic: "Hold: next. Tap: cast. Crouch + hold: list"
  - Bedrock Original: "Tap: pick. Hold: cast. Crouch: page"
  - Java Classic: "Right click next, sneak + right click list"
  - Java Original: "Key picks, left click casts, sneak pages"
- Java Tap bar how-to ends with "Right click again brings the bar back."
- Wand tips reworded (core/TipGate.java): Tap bar tip for Java and Bedrock, new Java Sneak bars tip with no "crouch".

## Flicker and stance lock
- Tap bar and compass held-item listeners run at MONITOR with ignoreCancelled, so a switch refused by a stance lock no longer opens or flickers the bar. New counter held-cancelled-ignored.
- Stance lock never blocks hotbar scrolling. Checked against ExcelAttunement 0.1.7: scrolling both ways skips only the locked wand slot.

## Tests
- Unit: 460 passed, 0 failed. New PageTest, more TapBarTest and TipTest cases.
- Rig: see TEST-RESULTS.md.

## Rollback
- Put 0.2.4 back. It boots on 0.2.5 data and config (extra key ignored) and puts its own Tap bar controls back. Tested on the rig.
